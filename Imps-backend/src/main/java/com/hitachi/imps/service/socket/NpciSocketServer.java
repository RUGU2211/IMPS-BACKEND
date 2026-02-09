package com.hitachi.imps.service.socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.hitachi.imps.config.NpciSocketConfig;
import com.hitachi.imps.config.SocketConfig;
import com.hitachi.imps.config.SslConfig;
import com.hitachi.imps.exception.CommonCodeValidationException;
import com.hitachi.imps.exception.ReqPayValidationException;
import com.hitachi.imps.service.ImpsInboundService;
import com.hitachi.imps.service.XmlParsingService;
import com.hitachi.imps.service.ack.AckService;
import com.hitachi.imps.service.npci.INpciResponseSender;
import com.hitachi.imps.service.pay.ReqPayValidationService;
import com.hitachi.imps.service.validation.CommonCodeValidationService;

/**
 * TCP server for NPCI. Listens on socket.server.npci.port.
 * Protocol: [4-byte length big-endian][XML].
 * When npci.compliant-flow=false: ACK + Resp on same connection.
 * When npci.compliant-flow=true: ACK only; Resp sent via outbound socket (NpciSocketClient).
 */
@Component
@ConditionalOnProperty(name = "socket.enabled", havingValue = "true")
public class NpciSocketServer {

    private static final Logger log = LoggerFactory.getLogger(NpciSocketServer.class);

    @Autowired private SocketConfig socketConfig;
    @Autowired(required = false) private SslConfig sslConfig;
    @Autowired private NpciSocketConfig npciSocketConfig;
    @Autowired(required = false) private INpciResponseSender npciResponseSender;
    @Autowired private PendingSocketResponseStore pendingStore;
    @Autowired private XmlParsingService xmlParsingService;
    @Autowired private ImpsInboundService impsInboundService;
    @Autowired private CommonCodeValidationService commonCodeValidationService;
    @Autowired private ReqPayValidationService reqPayValidationService;
    @Autowired private AckService ackService;
    @Autowired private SocketRequestDispatcher dispatcher;

    private ServerSocket serverSocket;  // plain or SSLServerSocket
    private final ExecutorService acceptor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "npci-socket-acceptor");
        t.setDaemon(false);
        return t;
    });
    private final ExecutorService handlers = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "npci-socket-handler");
        t.setDaemon(false);
        return t;
    });

    @jakarta.annotation.PostConstruct
    public void start() {
        if (!socketConfig.isEnabled()) return;
        String bindHost = socketConfig.getServer().getBindHost();
        boolean useSsl = sslConfig != null && sslConfig.isEnabled() && sslConfig.getKeyStore() != null && !sslConfig.getKeyStore().isBlank();
        int port = useSsl ? socketConfig.getServer().getNpci().getSslPort() : socketConfig.getServer().getNpci().getPort();

        acceptor.execute(() -> {
            try {
                if (useSsl) {
                    var ctx = sslConfig.buildServerContext();
                    SSLServerSocket sslServer = (SSLServerSocket) ctx.getServerSocketFactory().createServerSocket();
                    sslServer.bind(new InetSocketAddress(bindHost, port));
                    if (sslConfig.isNeedClientAuth()) sslServer.setNeedClientAuth(true);
                    else if (sslConfig.isWantClientAuth()) sslServer.setWantClientAuth(true);
                    serverSocket = sslServer;
                    log.info("NPCI SSL socket server listening on {}:{} (TLS)", bindHost, port);
                } else {
                    serverSocket = new ServerSocket();
                    serverSocket.bind(new InetSocketAddress(bindHost, port));
                    log.info("NPCI socket server listening on {}:{}", bindHost, port);
                }
                while (true) {
                    Socket client = serverSocket.accept();
                    if (client instanceof SSLSocket) {
                        ((SSLSocket) client).startHandshake();
                        try {
                            var session = ((SSLSocket) client).getSession();
                            var peerCerts = session.getPeerCertificates();
                            if (peerCerts != null && peerCerts.length > 0) {
                                log.info("NPCI SSL client cert: {}", peerCerts[0].toString());
                            }
                        } catch (Exception e) { /* optional log */ }
                    }
                    handlers.execute(() -> handleConnection(client));
                }
            } catch (IOException e) {
                if (serverSocket != null && !serverSocket.isClosed())
                    log.error("NPCI socket acceptor error", e);
            } catch (Exception e) {
                log.error("NPCI SSL setup error", e);
            }
        });
    }

    @jakarta.annotation.PreDestroy
    public void stop() {
        if (serverSocket != null) {
            try { serverSocket.close(); } catch (IOException ignored) {}
        }
        acceptor.shutdownNow();
        handlers.shutdownNow();
        try { handlers.awaitTermination(10, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
    }

    private void handleConnection(Socket socket) {
        String clientAddr = socket.getRemoteSocketAddress().toString();
        log.info("NPCI socket connected: {}", clientAddr);
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            int maxSize = socketConfig.getMaxXmlSize();
            int timeoutSec = socketConfig.getResponseTimeoutSeconds();
            while (true) {
                int length = in.readInt(); // big-endian
                if (length <= 0 || length > maxSize) {
                    log.warn("Invalid length from NPCI: {}", length);
                    break;
                }
                byte[] payload = new byte[length];
                in.readFully(payload);
                String xml = new String(payload, StandardCharsets.UTF_8);
                System.out.println("[IMPS] Req/Resp received from NPCI (socket):");
                System.out.println(xml);
                // Single msg_id extraction when request lands (socket entry)
                String reqMsgId = xmlParsingService.extractMsgId(xml);
                String txnId = xmlParsingService.extractTxnId(xml);
                if (txnId == null || txnId.isBlank()) txnId = reqMsgId;
                String msgType = detectMessageType(xml);
                boolean isRespType = isRespMessageType(msgType);
                String responseXml;
                try {
                    if (!isRespType) {
                        impsInboundService.validateNewTxnId(txnId);
                    }
                    if ("ReqPay".equals(msgType)) {
                        reqPayValidationService.validate(xml);
                    } else {
                        commonCodeValidationService.validateCommonHeadTxn(xml);
                    }
                    if (isRespType) {
                        dispatcher.dispatch(xml, txnId, msgType, reqMsgId);
                        responseXml = ackService.buildAckWithFallback(msgType, reqMsgId, txnId);
                    } else {
                        String ackXml = ackService.buildAckWithFallback(msgType, reqMsgId, txnId);
                        byte[] ackBytes = ackXml.getBytes(StandardCharsets.UTF_8);
                        out.writeInt(ackBytes.length);
                        out.write(ackBytes);
                        out.flush();
                        if (npciSocketConfig.isCompliantFlow() && npciResponseSender != null) {
                            dispatcher.dispatch(xml, txnId, msgType, reqMsgId);
                            continue;
                        }
                        CompletableFuture<String> future = pendingStore.registerPending(txnId);
                        dispatcher.dispatch(xml, txnId, msgType, reqMsgId);
                        responseXml = future.get(timeoutSec, TimeUnit.SECONDS);
                    }
                } catch (ReqPayValidationException e) {
                    responseXml = buildErrorAck("ReqPay", reqMsgId, e.getMessage());
                } catch (CommonCodeValidationException e) {
                    responseXml = buildErrorAck(msgType, reqMsgId, e.getMessage());
                } catch (IllegalArgumentException e) {
                    responseXml = buildErrorAck("", reqMsgId, e.getMessage());
                } catch (java.util.concurrent.TimeoutException e) {
                    pendingStore.removePending(txnId);
                    responseXml = buildErrorAck("", txnId, "Response timeout");
                } catch (Exception e) {
                    log.error("Socket request error txnId={}", txnId, e);
                    responseXml = buildErrorAck("", txnId != null ? txnId : "", e.getMessage());
                }
                byte[] respBytes = responseXml.getBytes(StandardCharsets.UTF_8);
                out.writeInt(respBytes.length);
                out.write(respBytes);
                out.flush();
            }
        } catch (IOException e) {
            log.debug("NPCI socket closed: {}", clientAddr);
        } catch (Exception e) {
            log.error("NPCI socket error: {}", clientAddr, e);
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private boolean isRespMessageType(String msgType) {
        return "RespPay".equals(msgType) || "RespChkTxn".equals(msgType)
            || "RespListAccPvd".equals(msgType) || "RespValAdd".equals(msgType);
    }

    private String detectMessageType(String xml) {
        if (xml.contains("RespPay") && xml.contains("</")) return "RespPay";
        if (xml.contains("RespChkTxn")) return "RespChkTxn";
        if (xml.contains("RespListAccPvd")) return "RespListAccPvd";
        if (xml.contains("RespValAdd")) return "RespValAdd";
        if (xml.contains("ReqPay") && xml.contains("</")) return "ReqPay";
        if (xml.contains("ReqChkTxn")) return "ReqChkTxn";
        if (xml.contains("ReqHbt")) return "ReqHbt";
        if (xml.contains("ReqValAdd")) return "ReqValAdd";
        if (xml.contains("ReqListAccPvd")) return "ReqListAccPvd";
        return "";
    }

    private String buildErrorAck(String api, String reqMsgId, String message) {
        String apiVal = (api != null && !api.isBlank()) ? api : "Ack";
        String msg = (reqMsgId != null && !reqMsgId.isBlank()) ? reqMsgId : "SOCKET";
        return ackService.buildAckWithFallback(apiVal, reqMsgId, msg);
    }
}
