package com.hitachi.imps.client.npci;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.SSLSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.hitachi.imps.config.NpciSocketConfig;
import com.hitachi.imps.config.SslConfig;

/**
 * Outbound socket client for IMPS → NPCI. Sends [4-byte length][Resp XML], reads ACK.
 */
@Component
@ConditionalOnProperty(name = "npci.compliant-flow", havingValue = "true")
public class NpciSocketClient {

    private static final Logger log = LoggerFactory.getLogger(NpciSocketClient.class);
    private static final int MAX_ACK_SIZE = 2048;

    @Autowired
    private NpciSocketConfig npciSocketConfig;

    public boolean sendResponse(String respXml) {
        if (respXml == null || respXml.isBlank()) return false;
        var sock = npciSocketConfig.getSocket();
        String host = sock.getHost();
        int port = sock.getPort();
        int connectTimeout = sock.getConnectTimeoutMs();
        int readTimeout = sock.getReadTimeoutMs();
        boolean useSsl = sock.isSslEnabled();
        try {
            Socket socket;
            if (useSsl) {
                var ctx = SslConfig.buildClientContext(
                    sock.getTrustStore(), sock.getTrustStorePassword(),
                    sock.getKeyStore(), sock.getKeyStorePassword(), "JKS");
                SSLSocket sslSocket = (SSLSocket) ctx.getSocketFactory().createSocket();
                sslSocket.connect(new InetSocketAddress(host, port), connectTimeout);
                sslSocket.startHandshake();
                socket = sslSocket;
            } else {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), connectTimeout);
            }
            try {
                socket.setSoTimeout(readTimeout);
                try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                     DataInputStream in = new DataInputStream(socket.getInputStream())) {
                    byte[] payload = respXml.getBytes(StandardCharsets.UTF_8);
                    out.writeInt(payload.length);
                    out.write(payload);
                    out.flush();
                    int ackLen = in.readInt();
                    if (ackLen <= 0 || ackLen > MAX_ACK_SIZE) {
                        log.warn("Invalid ACK length from NPCI: {}", ackLen);
                        return false;
                    }
                    byte[] ackPayload = new byte[ackLen];
                    in.readFully(ackPayload);
                    return true;
                }
            } finally {
                socket.close();
            }
        } catch (IOException e) {
            log.error("Failed to send response to NPCI {}:{}: {}", host, port, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("SSL setup failed for NPCI {}:{}: {}", host, port, e.getMessage());
            return false;
        }
    }
}
