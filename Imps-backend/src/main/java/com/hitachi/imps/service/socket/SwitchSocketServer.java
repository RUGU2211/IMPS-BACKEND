package com.hitachi.imps.service.socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.hitachi.imps.config.SocketConfig;
import com.hitachi.imps.config.SslConfig;
import com.hitachi.imps.iso.ImpsIsoPackager;
import com.hitachi.imps.service.ack.AckService;
import com.hitachi.imps.service.chktxn.ReqChkTxnService;
import com.hitachi.imps.service.heartbeat.ReqHbtService;
import com.hitachi.imps.service.listaccpvd.ReqListAccPvdService;
import com.hitachi.imps.service.pay.ReqPayService;
import com.hitachi.imps.service.valadd.ReqValAddService;
import com.hitachi.imps.util.IsoUtil;

/**
 * TCP server for Switch (reverse flow). Listens on socket.server.switch.port.
 * Protocol: [4-byte length big-endian][ISO].
 * Same connection pattern as NPCI→IMPS ([4 bytes][XML]), but payload is ISO.
 * Switch connects → sends [4 bytes][ISO] → IMPS responds [4 bytes][ISO].
 */
@Component
@ConditionalOnProperty(name = "socket.enabled", havingValue = "true")
public class SwitchSocketServer {

    private static final Logger log = LoggerFactory.getLogger(SwitchSocketServer.class);

    @Autowired private SocketConfig socketConfig;
    @Autowired(required = false) private SslConfig sslConfig;
    @Autowired private ReqPayService reqPayService;
    @Autowired private ReqChkTxnService reqChkTxnService;
    @Autowired private ReqHbtService reqHbtService;
    @Autowired private ReqListAccPvdService reqListAccPvdService;
    @Autowired private ReqValAddService reqValAddService;
    @Autowired private AckService ackService;

    private ServerSocket serverSocket;
    private final ExecutorService acceptor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "switch-socket-acceptor");
        t.setDaemon(false);
        return t;
    });
    private final ExecutorService handlers = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "switch-socket-handler");
        t.setDaemon(false);
        return t;
    });

    @jakarta.annotation.PostConstruct
    public void start() {
        var sw = socketConfig.getServer().getSwitchServer();
        if (!sw.isEnabled() || !socketConfig.isEnabled()) return;
        String bindHost = socketConfig.getServer().getBindHost();
        boolean useSsl = sslConfig != null && sslConfig.isEnabled() && sslConfig.getKeyStore() != null && !sslConfig.getKeyStore().isBlank();
        int port = useSsl ? sw.getSslPort() : sw.getPort();

        acceptor.execute(() -> {
            try {
                if (useSsl) {
                    var ctx = sslConfig.buildServerContext();
                    SSLServerSocket sslServer = (SSLServerSocket) ctx.getServerSocketFactory().createServerSocket();
                    sslServer.bind(new InetSocketAddress(bindHost, port));
                    if (sslConfig.isNeedClientAuth()) sslServer.setNeedClientAuth(true);
                    else if (sslConfig.isWantClientAuth()) sslServer.setWantClientAuth(true);
                    serverSocket = sslServer;
                    log.info("Switch SSL socket server listening on {}:{} (TLS) - [4 bytes][ISO]", bindHost, port);
                } else {
                    serverSocket = new ServerSocket();
                    serverSocket.bind(new InetSocketAddress(bindHost, port));
                    log.info("Switch socket server listening on {}:{} - [4 bytes][ISO]", bindHost, port);
                }
                while (true) {
                    Socket client = serverSocket.accept();
                    if (client instanceof SSLSocket) {
                        ((SSLSocket) client).startHandshake();
                    }
                    handlers.execute(() -> handleConnection(client));
                }
            } catch (IOException e) {
                if (serverSocket != null && !serverSocket.isClosed())
                    log.error("Switch socket acceptor error", e);
            } catch (Exception e) {
                log.error("Switch SSL setup error", e);
            }
        });
    }

    @jakarta.annotation.PreDestroy
    public void stop() {
        if (serverSocket != null) {
            try { serverSocket.close(); } catch (IOException e) { log.debug("ServerSocket close: {}", e.getMessage()); }
        }
        acceptor.shutdownNow();
        handlers.shutdownNow();
        try { handlers.awaitTermination(10, TimeUnit.SECONDS); } catch (InterruptedException e) { log.debug("Await termination interrupted: {}", e.getMessage()); Thread.currentThread().interrupt(); }
    }

    private void handleConnection(Socket socket) {
        String clientAddr = socket.getRemoteSocketAddress().toString();
        String protocol = socket instanceof SSLSocket ? "SSL/TLS" : "TCP";
        log.info("[IMPS] Switch socket connected: {} ({})", clientAddr, protocol);
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            int maxSize = socketConfig.getMaxXmlSize();
            while (true) {
                int length = in.readInt();
                if (length <= 0 || length > maxSize) {
                    log.warn("Invalid ISO length from Switch: {}", length);
                    break;
                }
                byte[] isoBytes = new byte[length];
                in.readFully(isoBytes);
                String msgType = detectMessageType(isoBytes);
                String txnId = extractTxnId(isoBytes);
                String isoDisplay = formatIsoForConsole(isoBytes);
                log.debug("[IMPS] Request from Switch (socket): {} txnId={} {} bytes", msgType, txnId, length);
                log.debug("ISO: {}", isoDisplay);
                byte[] respIso;
                try {
                    switch (msgType) {
                        case "ReqPay":
                            respIso = reqPayService.processFromSwitchSync(isoBytes, txnId);
                            break;
                        case "ReqChkTxn":
                            respIso = reqChkTxnService.processFromSwitchSync(isoBytes, txnId);
                            break;
                        case "ReqHbt":
                            respIso = reqHbtService.processFromSwitch(isoBytes, txnId);
                            break;
                        case "ReqListAccPvd":
                            respIso = reqListAccPvdService.processFromSwitchSync(isoBytes, txnId);
                            break;
                        case "ReqValAdd":
                            respIso = reqValAddService.processFromSwitchSync(isoBytes, txnId);
                            break;
                        default:
                            respIso = ackService.buildIsoAckFromResponse(isoBytes);
                    }
                    if (respIso == null || respIso.length == 0) {
                        respIso = ackService.buildIsoAckFromResponse(isoBytes);
                    }
                } catch (Exception e) {
                    log.error("[IMPS] Switch socket process error txnId={} msgType={}", txnId, msgType, e);
                    respIso = ackService.buildIsoAckFromResponse(isoBytes);
                    if (respIso == null) respIso = new byte[0];
                }
                if (respIso != null && respIso.length > 0) {
                    String respDisplay = formatIsoForConsole(respIso);
                    String respMsgType = detectMessageType(respIso);
                    log.debug("[IMPS] Response to Switch (socket): {} txnId={} {} bytes", respMsgType, txnId, respIso.length);
                    log.debug("ISO: {}", respDisplay);
                }
                out.writeInt(respIso.length);
                out.write(respIso);
                out.flush();
            }
        } catch (IOException e) {
            log.debug("[IMPS] Switch socket closed: {}", clientAddr);
            log.debug("[IMPS] Connection closed by Switch: {} ({})", clientAddr, e.getMessage());
        } catch (Exception e) {
            log.error("[IMPS] Switch socket error: {}", clientAddr, e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                log.debug("[IMPS] Error closing socket: {}", e.getMessage());
            }
        }
    }

    /** Detect message type from ISO: MTI + DE3. MTI 0800=ReqHbt, MTI 0200 + DE3 prefix = Pay/ChkTxn/ValAdd/ListAccPvd */
    private String detectMessageType(byte[] isoBytes) {
        try {
            ISOMsg iso = IsoUtil.unpack(isoBytes, new ImpsIsoPackager());
            String mti = iso.getMTI();
            if (mti == null) mti = "";
            if ("0800".equals(mti)) return "ReqHbt";
            if ("0200".equals(mti)) {
                String de3 = iso.hasField(3) ? iso.getString(3) : "";
                if (de3 != null && de3.length() >= 2) {
                    if (de3.startsWith("32")) return "ReqListAccPvd";
                    if (de3.startsWith("31")) return "ReqValAdd";
                    if (de3.startsWith("90")) return "ReqChkTxn";
                }
                return "ReqPay";
            }
            return "ReqPay";
        } catch (Exception e) {
            return "ReqPay";
        }
    }

    /** Extract txnId from ISO: DE37 (RRN) + DE11 (STAN), padded to 35 chars */
    private String extractTxnId(byte[] isoBytes) {
        try {
            ISOMsg iso = IsoUtil.unpack(isoBytes, new ImpsIsoPackager());
            String de37 = iso.hasField(37) ? iso.getString(37) : "";
            String de11 = iso.hasField(11) ? iso.getString(11) : "";
            String combined = "SWI" + (de37 != null ? de37 : "") + (de11 != null ? de11 : "");
            return combined.length() >= 35 ? combined.substring(0, 35) : String.format("%-35s", combined).replace(' ', '0');
        } catch (Exception e) {
            return "SWI" + System.currentTimeMillis();
        }
    }

    private String formatIsoForConsole(byte[] isoBytes) {
        try {
            ISOMsg iso = IsoUtil.unpack(isoBytes, new ImpsIsoPackager());
            StringBuilder sb = new StringBuilder();
            sb.append("MTI=").append(iso.getMTI()).append("\n");
            for (int i = 1; i <= 128; i++) {
                if (iso.hasField(i)) {
                    sb.append("DE").append(i).append("=").append(iso.getString(i)).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "ISO parse failed: " + e.getMessage();
        }
    }
}
