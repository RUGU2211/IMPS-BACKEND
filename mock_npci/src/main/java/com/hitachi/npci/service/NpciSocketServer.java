package com.hitachi.npci.service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.hitachi.npci.config.SocketConfig;
import com.hitachi.npci.config.SslConfig;

/**
 * TCP server for IMPS. Listens on socket.server.port.
 * Receives [4-byte length big-endian][XML] (RespPay, RespHbt, etc.), logs, and sends [4-byte length][Ack] back.
 * NPCI-compliant: IMPS connects outbound; NPCI sends ACK on same socket.
 */
@Component
@ConditionalOnProperty(name = "socket.enabled", havingValue = "true")
public class NpciSocketServer {

    private static final Logger log = LoggerFactory.getLogger(NpciSocketServer.class);
    private static final int MAX_XML_SIZE = 1024 * 1024;
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @Autowired private SocketConfig socketConfig;
    @Autowired(required = false) private SslConfig sslConfig;

    private ServerSocket serverSocket;
    private final ExecutorService acceptor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "npci-mock-socket-acceptor");
        t.setDaemon(false);
        return t;
    });
    private final ExecutorService handlers = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "npci-mock-socket-handler");
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
                    log.info("NPCI Mock SSL socket server listening on {}:{}", bindHost, sslPort);
                } else {
                    int port = socketConfig.getServer().getPort();
                    serverSocket = new ServerSocket();
                    serverSocket.bind(new InetSocketAddress(bindHost, port));
                    log.info("NPCI Mock socket server listening on {}:{}", bindHost, port);
                }
                while (true) {
                    Socket client = serverSocket.accept();
                    handlers.execute(() -> handleConnection(client));
                }
            } catch (IOException e) {
                if (serverSocket != null && !serverSocket.isClosed())
                    log.error("NPCI Mock socket acceptor error", e);
            } catch (Exception e) {
                log.error("NPCI Mock socket startup error", e);
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
        log.info("IMPS socket connected: {}", clientAddr);
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            while (true) {
                int length = in.readInt();
                if (length <= 0 || length > MAX_XML_SIZE) {
                    log.warn("Invalid XML length from IMPS: {}", length);
                    break;
                }
                byte[] payload = new byte[length];
                in.readFully(payload);
                String xml = new String(payload, StandardCharsets.UTF_8);
                System.out.println("[MOCK_NPCI] Resp received from IMPS (socket):");
                System.out.println(xml);
                System.out.println("[MOCK_NPCI] ACK sent to IMPS");

                String ackXml = buildAck(xml);
                byte[] ackBytes = ackXml.getBytes(StandardCharsets.UTF_8);
                out.writeInt(ackBytes.length);
                out.write(ackBytes);
                out.flush();
                log.debug("Sent ACK to IMPS");
            }
        } catch (IOException e) {
            log.debug("IMPS socket closed: {}", clientAddr);
        } catch (Exception e) {
            log.error("NPCI Mock socket error: {}", clientAddr, e);
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private String buildAck(String xml) {
        String api = detectApi(xml);
        String reqMsgId = extractMsgId(xml);
        if (reqMsgId == null || reqMsgId.isBlank()) reqMsgId = "ACK" + System.currentTimeMillis();
        String ts = OffsetDateTime.now().format(TS_FORMAT);
        String ns = "http://npci.org/upi/schema/";
        return "<ns2:Ack xmlns:ns2=\"" + ns + "\" api=\"" + escape(api) + "\" reqMsgId=\"" + escape(reqMsgId) + "\" ts=\"" + ts + "\"></ns2:Ack>";
    }

    private String detectApi(String xml) {
        if (xml.contains("RespPay")) return "RespPay";
        if (xml.contains("RespChkTxn")) return "RespChkTxn";
        if (xml.contains("RespHbt")) return "RespHbt";
        if (xml.contains("RespListAccPvd")) return "RespListAccPvd";
        if (xml.contains("RespValAdd")) return "RespValAdd";
        return "Ack";
    }

    private String extractMsgId(String xml) {
        int i = xml.indexOf("msgId=\"");
        if (i < 0) return null;
        i += 7;
        int j = xml.indexOf("\"", i);
        if (j < 0) return null;
        return xml.substring(i, j);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
