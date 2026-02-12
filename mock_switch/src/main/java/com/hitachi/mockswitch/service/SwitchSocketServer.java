package com.hitachi.mockswitch.service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.hitachi.mockswitch.config.SocketConfig;
import com.hitachi.mockswitch.config.SslConfig;
import com.hitachi.mockswitch.iso.MockIsoPackager;

/**
 * TCP server for IMPS. Listens on socket.server.port.
 * Protocol: [4-byte length big-endian][ISO]. Response: [4-byte length][ISO] on same connection.
 */
@Component
@ConditionalOnProperty(name = "socket.enabled", havingValue = "true")
public class SwitchSocketServer {

    private static final Logger log = LoggerFactory.getLogger(SwitchSocketServer.class);
    private static final int MAX_ISO_SIZE = 1024 * 1024;

    @Autowired private SocketConfig socketConfig;
    @Autowired(required = false) private SslConfig sslConfig;
    @Autowired private MockResponseService responseService;

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
        if (!socketConfig.isEnabled()) return;
        String bindHost = socketConfig.getServer().getBindHost();
        acceptor.execute(() -> {
            try {
                if (socketConfig.isSslEnabled() && sslConfig != null) {
                    int sslPort = socketConfig.getServer().getSslPort();
                    var factory = sslConfig.createServerSocketFactory();
                    serverSocket = factory.createServerSocket();
                    serverSocket.bind(new InetSocketAddress(bindHost, sslPort));
                    log.info("Switch SSL socket server listening on {}:{}", bindHost, sslPort);
                } else {
                    int port = socketConfig.getServer().getPort();
                    serverSocket = new ServerSocket();
                    serverSocket.bind(new InetSocketAddress(bindHost, port));
                    log.info("Switch socket server listening on {}:{}", bindHost, port);
                }
                while (true) {
                    Socket client = serverSocket.accept();
                    handlers.execute(() -> handleConnection(client));
                }
            } catch (IOException e) {
                if (serverSocket != null && !serverSocket.isClosed())
                    log.error("Switch socket acceptor error", e);
            } catch (Exception e) {
                log.error("Switch socket startup error", e);
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
        String protocol = socket instanceof javax.net.ssl.SSLSocket ? "SSL/TLS" : "TCP";
        log.info("[MOCK_SWITCH] IMPS socket connected: {} ({})", clientAddr, protocol);
        System.out.println("==========================================");
        System.out.println("[MOCK_SWITCH] INCOMING CONNECTION FROM IMPS");
        System.out.println("Protocol: " + protocol + " | Address: " + clientAddr);
        System.out.println("==========================================");
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            while (true) {
                int length = in.readInt();
                if (length <= 0 || length > MAX_ISO_SIZE) {
                    log.warn("Invalid ISO length from IMPS: {}", length);
                    break;
                }
                byte[] isoBytes = new byte[length];
                in.readFully(isoBytes);
                String isoDisplay = formatIsoForConsole(isoBytes);
                String txnId = extractTxnId(isoBytes);
                String msgType = detectMessageType(isoBytes);
                log.info("[MOCK_SWITCH] ========== REQUEST FROM IMPS (SOCKET) ==========");
                log.info("[MOCK_SWITCH] Message Type: {} | TxnId: {} | Length: {} bytes", msgType, txnId, length);
                log.info("[MOCK_SWITCH] ISO received from IMPS:\n{}", isoDisplay);
                System.out.println("==========================================");
                System.out.println("[MOCK_SWITCH] REQUEST FROM IMPS (SOCKET)");
                System.out.println("Message Type: " + msgType + " | TxnId: " + txnId + " | Length: " + length + " bytes");
                System.out.println("==========================================");
                System.out.println(isoDisplay);
                byte[] respIso = processIso(isoBytes, txnId);
                if (respIso != null) {
                    String respDisplay = formatIsoForConsole(respIso);
                    String respMsgType = detectMessageType(respIso);
                    log.info("[MOCK_SWITCH] ========== RESPONSE TO IMPS (SOCKET) ==========");
                    log.info("[MOCK_SWITCH] Message Type: {} | TxnId: {} | Length: {} bytes", respMsgType, txnId, respIso.length);
                    log.info("[MOCK_SWITCH] ISO response sent to IMPS:\n{}", respDisplay);
                    System.out.println("==========================================");
                    System.out.println("[MOCK_SWITCH] RESPONSE TO IMPS (SOCKET)");
                    System.out.println("Message Type: " + respMsgType + " | TxnId: " + txnId + " | Length: " + respIso.length + " bytes");
                    System.out.println("==========================================");
                    System.out.println(respDisplay);
                    out.writeInt(respIso.length);
                    out.write(respIso);
                    out.flush();
                } else {
                    log.warn("[MOCK_SWITCH] No response generated for ISO from {}", clientAddr);
                    System.out.println("[MOCK_SWITCH] WARNING: No response generated for request from " + clientAddr);
                    break;
                }
            }
        } catch (IOException e) {
            log.info("[MOCK_SWITCH] IMPS socket closed: {} ({})", clientAddr, e.getMessage() != null ? e.getMessage() : "no data or connection reset");
        } catch (Exception e) {
            log.error("[MOCK_SWITCH] Switch socket error: {}", clientAddr, e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                log.debug("[MOCK_SWITCH] Error closing socket: {}", e.getMessage());
            }
        }
    }

    private String formatIsoForConsole(byte[] isoBytes) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(new MockIsoPackager());
            iso.unpack(isoBytes);
            StringBuilder sb = new StringBuilder();
            sb.append("MTI=").append(iso.getMTI()).append("\n");
            for (int i = 1; i <= 128; i++) {
                if (iso.hasField(i)) {
                    sb.append("DE").append(i).append("=").append(iso.getString(i)).append("\n");
                }
            }
            return sb.toString();
        } catch (ISOException e) {
            return "ISO parse failed: " + e.getMessage();
        }
    }

    private String extractTxnId(byte[] isoBytes) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(new MockIsoPackager());
            iso.unpack(isoBytes);
            if (iso.hasField(120)) return iso.getString(120);
        } catch (ISOException ignored) {}
        return "";
    }

    private String detectMessageType(byte[] isoBytes) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(new MockIsoPackager());
            iso.unpack(isoBytes);
            String mti = iso.getMTI();
            String de3 = iso.hasField(3) ? iso.getString(3) : "";
            String de24 = iso.hasField(24) ? iso.getString(24) : "";
            if ("0800".equals(mti) && "831".equals(de24)) {
                return "ReqHbt";
            }
            if ("0210".equals(mti) || "0810".equals(mti)) {
                if (de3.startsWith("40")) return "RespPay";
                if (de3.startsWith("38")) return "RespChkTxn";
                if (de3.startsWith("31")) return "RespValAdd";
                if (de3.startsWith("32")) return "RespListAccPvd";
                if ("831".equals(de24)) return "RespHbt";
            }
            if ("0200".equals(mti)) {
                if (de3.startsWith("40")) return "ReqPay";
                if (de3.startsWith("38")) return "ReqChkTxn";
                if (de3.startsWith("31")) return "ReqValAdd";
                if (de3.startsWith("32")) return "ReqListAccPvd";
            }
            return "Unknown (MTI=" + mti + ", DE3=" + de3 + ")";
        } catch (Exception e) {
            return "ParseError";
        }
    }

    private byte[] processIso(byte[] isoBytes, String txnId) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(new MockIsoPackager());
            iso.unpack(isoBytes);
            String mti = iso.getMTI();
            String de3 = iso.hasField(3) ? iso.getString(3) : "";
            String de24 = iso.hasField(24) ? iso.getString(24) : "";
            if ("0800".equals(mti) && "831".equals(de24)) {
                return responseService.buildRespHbtSync(isoBytes, txnId);
            }
            if ("0200".equals(mti)) {
                if (de3.startsWith("40")) return responseService.buildRespPaySync(isoBytes, txnId);
                if (de3.startsWith("38")) return responseService.buildRespChkTxnSync(isoBytes, txnId);
                if (de3.startsWith("31")) return responseService.buildRespValAddSync(isoBytes, txnId);
                if (de3.startsWith("32")) return responseService.buildRespListAccPvdSync(isoBytes, txnId);
            }
            log.warn("[MOCK_SWITCH] Unknown ISO type: MTI={} DE3={}", mti, de3);
            return null;
        } catch (Exception e) {
            log.error("[MOCK_SWITCH] processIso error", e);
            return null;
        }
    }
}
