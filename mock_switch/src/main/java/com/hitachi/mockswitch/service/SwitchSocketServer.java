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
import com.hitachi.mockswitch.util.Iso8583PrettyFormatter;

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
        String clientHost = socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : "?";
        int clientPort = socket.getPort();
        String actualAddress = clientHost + ":" + clientPort;
        String protocol = socket instanceof javax.net.ssl.SSLSocket ? "SSL/TLS" : "TCP";
        log.info("[SWITCH] IMPS socket connected: {} ({})", actualAddress, protocol);
        System.out.println("========== [SWITCH] IMPS → Switch (Socket) | Incoming connection ==========");
        System.out.println("  Protocol: " + protocol + " | Remote address: " + actualAddress);
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
                log.info("[SWITCH] ========== REQUEST FROM IMPS (SOCKET) ==========");
                log.info("[SWITCH] Message Type: {} | TxnId: {} | Length: {} bytes", msgType, txnId, length);
                log.info("[SWITCH] ISO received from IMPS:\n{}", isoDisplay);
                System.out.println("========== [SWITCH] IMPS → Switch | REQ received (Socket) | " + msgType + " ==========");
                System.out.println("  REQ received from: IMPS at " + actualAddress);
                System.out.println("  TxnId: " + txnId + " | Length: " + length + " bytes");
                System.out.println("  ---------- ISO 8583 ----------");
                System.out.println(isoDisplay);
                byte[] respIso = processIso(isoBytes, txnId);
                if (respIso != null) {
                    String respDisplay = formatIsoForConsole(respIso);
                    String respMsgType = detectMessageType(respIso);
                    log.info("[SWITCH] ========== RESPONSE TO IMPS (SOCKET) ==========");
                    log.info("[SWITCH] Message Type: {} | TxnId: {} | Length: {} bytes", respMsgType, txnId, respIso.length);
                    log.info("[SWITCH] ISO response sent to IMPS:\n{}", respDisplay);
                    System.out.println("========== [SWITCH] Switch → IMPS | RESP sent (Socket) | " + respMsgType + " ==========");
                    System.out.println("  RESP sent to: IMPS at " + actualAddress);
                    System.out.println("  TxnId: " + txnId + " | Length: " + respIso.length + " bytes");
                    System.out.println("  ---------- ISO 8583 ----------");
                    System.out.println(respDisplay);
                    out.writeInt(respIso.length);
                    out.write(respIso);
                    out.flush();
                } else {
                    log.warn("[SWITCH] No response generated for ISO from {}", actualAddress);
                    System.out.println("[SWITCH] WARNING: No response generated for request from " + actualAddress);
                    break;
                }
            }
        } catch (IOException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("reset") || msg.contains("aborted") || msg.contains("closed")) {
                log.info("[SWITCH] IMPS connection closed: {} (normal – response sent, client disconnected)", actualAddress);
            } else {
                log.info("[SWITCH] IMPS socket closed: {} ({})", actualAddress, msg);
            }
        } catch (Exception e) {
            log.error("[SWITCH] Switch socket error: {}", actualAddress, e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                log.debug("[SWITCH] Error closing socket: {}", e.getMessage());
            }
        }
    }

    private String formatIsoForConsole(byte[] isoBytes) {
        try {
            ISOMsg iso = new ISOMsg();
            iso.setPackager(new MockIsoPackager());
            iso.unpack(isoBytes);
            return Iso8583PrettyFormatter.format(iso);
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
            log.warn("[SWITCH] Unknown ISO type: MTI={} DE3={}", mti, de3);
            return null;
        } catch (Exception e) {
            log.error("[SWITCH] processIso error", e);
            return null;
        }
    }
}
