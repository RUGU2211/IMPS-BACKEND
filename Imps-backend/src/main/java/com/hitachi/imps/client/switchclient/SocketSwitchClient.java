package com.hitachi.imps.client.switchclient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocket;

import org.jpos.iso.ISOMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.hitachi.imps.config.RoutingConfig;
import com.hitachi.imps.iso.ImpsIsoPackager;
import com.hitachi.imps.config.SslConfig;
import com.hitachi.imps.service.routing.SwitchAddressResolver;
import com.hitachi.imps.service.routing.SwitchAddressResolver.SwitchAddress;
import com.hitachi.imps.util.IsoUtil;

@Component
@ConditionalOnProperty(name = "routing.switch.socket.enabled", havingValue = "true", matchIfMissing = true)
public class SocketSwitchClient implements ISwitchClient {

    private static final Logger log = LoggerFactory.getLogger(SocketSwitchClient.class);

    @Autowired
    private SwitchAddressResolver switchAddressResolver;
    @Autowired
    private RoutingConfig routingConfig;
    private static final int READ_TIMEOUT_MS = 60000;
    private static final int MAX_ISO_SIZE = 1024 * 1024;

    private byte[] sendIso(String apiType, byte[] isoBytes, String txnId, String requestOrgId) {
        SwitchAddress addr = switchAddressResolver.resolve(requestOrgId);
        String host = addr.getHost();
        int port = addr.getPort();
        var sockConfig = routingConfig.getSwitch().getSocket();
        boolean useSsl = sockConfig != null && sockConfig.isSslEnabled();
        Socket s = null;
        String protocol = useSsl ? "SSL/TLS" : "TCP";
        try {
            System.out.println("==========================================");
            System.out.println("[IMPS] OPENING CONNECTION TO SWITCH");
            System.out.println("Protocol: " + protocol + " | Host: " + host + " | Port: " + port);
            System.out.println("Message Type: " + apiType + " | TxnId: " + txnId);
            System.out.println("==========================================");
            
            if (useSsl) {
                var ctx = SslConfig.buildClientContext(
                    sockConfig.getTrustStore(), sockConfig.getTrustStorePassword(),
                    sockConfig.getKeyStore(), sockConfig.getKeyStorePassword(), "PKCS12");
                SSLSocket ssl = (SSLSocket) ctx.getSocketFactory().createSocket();
                ssl.connect(new InetSocketAddress(host, port), 10000);
                ssl.startHandshake();
                s = ssl;
                System.out.println("[IMPS] SSL/TLS handshake completed with Switch");
            } else {
                s = new Socket();
                s.connect(new InetSocketAddress(host, port), 10000);
                System.out.println("[IMPS] TCP connection established with Switch");
            }
            s.setSoTimeout(READ_TIMEOUT_MS);
            
            System.out.println("[IMPS] Sending ISO message to Switch (" + isoBytes.length + " bytes)");
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            DataInputStream in = new DataInputStream(s.getInputStream());
            out.writeInt(isoBytes.length);
            out.write(isoBytes);
            out.flush();
            
            System.out.println("[IMPS] Waiting for response from Switch...");
            int respLen = in.readInt();
            if (respLen <= 0 || respLen > MAX_ISO_SIZE) {
                System.err.println("[IMPS] Invalid response length from Switch: " + respLen);
                return null;
            }
            byte[] resp = new byte[respLen];
            in.readFully(resp);
            System.out.println("==========================================");
            System.out.println("[IMPS] RESPONSE RECEIVED FROM SWITCH (SOCKET)");
            System.out.println("Message Type: " + apiType.replace("req", "resp").replace("Req", "Resp") + " | TxnId: " + txnId + " | Length: " + resp.length + " bytes");
            System.out.println("==========================================");
            try {
                ISOMsg respIso = IsoUtil.unpack(resp, new ImpsIsoPackager());
                StringBuilder sb = new StringBuilder();
                sb.append("MTI=").append(respIso.getMTI()).append("\n");
                for (int i = 1; i <= 128; i++) {
                    if (respIso.hasField(i)) sb.append("DE").append(i).append("=").append(respIso.getString(i)).append("\n");
                }
                System.out.println(sb.toString());
            } catch (Exception e) {
                System.out.println("(ISO format failed: " + e.getMessage() + ")");
            }
            log.info("[IMPS] Response received from Switch [{}] txnId={} {} bytes", apiType, txnId, resp.length);
            return resp;
        } catch (IOException e) {
            log.error("[IMPS] Switch socket send failed [{}]/{}: {}", apiType, txnId, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("[IMPS] Switch SSL setup failed [{}]/{}: {}", apiType, txnId, e.getMessage(), e);
            return null;
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    log.debug("[IMPS] Error closing socket to Switch: {}", e.getMessage());
                }
            }
        }
    }

    @Override public byte[] sendReqPay(ISOMsg iso, String txnId) { return sendReqPay(iso, txnId, null); }
    @Override public byte[] sendReqPay(ISOMsg iso, String txnId, String requestOrgId) { return sendIso("reqpay", IsoUtil.pack(iso), txnId, requestOrgId); }
    @Override public byte[] sendReqPay(byte[] isoBytes, String txnId) { return sendReqPay(isoBytes, txnId, null); }
    @Override public byte[] sendReqPay(byte[] isoBytes, String txnId, String requestOrgId) { return sendIso("reqpay", isoBytes, txnId, requestOrgId); }

    @Override public byte[] sendReqChkTxn(ISOMsg iso, String txnId) { return sendReqChkTxn(iso, txnId, null); }
    @Override public byte[] sendReqChkTxn(ISOMsg iso, String txnId, String requestOrgId) { return sendIso("reqchktxn", IsoUtil.pack(iso), txnId, requestOrgId); }
    @Override public byte[] sendReqChkTxn(byte[] isoBytes, String txnId) { return sendReqChkTxn(isoBytes, txnId, null); }
    @Override public byte[] sendReqChkTxn(byte[] isoBytes, String txnId, String requestOrgId) { return sendIso("reqchktxn", isoBytes, txnId, requestOrgId); }

    @Override public byte[] sendReqHbt(ISOMsg iso, String txnId) { return sendReqHbt(iso, txnId, null); }
    @Override public byte[] sendReqHbt(ISOMsg iso, String txnId, String requestOrgId) { return sendIso("reqhbt", IsoUtil.pack(iso), txnId, requestOrgId); }
    @Override public byte[] sendReqHbt(byte[] isoBytes, String txnId) { return sendReqHbt(isoBytes, txnId, null); }
    @Override public byte[] sendReqHbt(byte[] isoBytes, String txnId, String requestOrgId) { return sendIso("reqhbt", isoBytes, txnId, requestOrgId); }

    @Override public byte[] sendReqListAccPvd(ISOMsg iso, String txnId) { return sendReqListAccPvd(iso, txnId, null); }
    @Override public byte[] sendReqListAccPvd(ISOMsg iso, String txnId, String requestOrgId) { return sendIso("reqlistaccpvd", IsoUtil.pack(iso), txnId, requestOrgId); }
    @Override public byte[] sendReqListAccPvd(byte[] isoBytes, String txnId) { return sendReqListAccPvd(isoBytes, txnId, null); }
    @Override public byte[] sendReqListAccPvd(byte[] isoBytes, String txnId, String requestOrgId) { return sendIso("reqlistaccpvd", isoBytes, txnId, requestOrgId); }

    @Override public byte[] sendReqValAdd(ISOMsg iso, String txnId) { return sendReqValAdd(iso, txnId, null); }
    @Override public byte[] sendReqValAdd(ISOMsg iso, String txnId, String requestOrgId) { return sendIso("reqvaladd", IsoUtil.pack(iso), txnId, requestOrgId); }
    @Override public byte[] sendReqValAdd(byte[] isoBytes, String txnId) { return sendReqValAdd(isoBytes, txnId, null); }
    @Override public byte[] sendReqValAdd(byte[] isoBytes, String txnId, String requestOrgId) { return sendIso("reqvaladd", isoBytes, txnId, requestOrgId); }

    @Override public byte[] sendRespPay(ISOMsg iso, String txnId) { return txnId != null && !txnId.isBlank() ? sendIso("resppay", IsoUtil.pack(iso), txnId, null) : null; }
    @Override public byte[] sendRespChkTxn(ISOMsg iso, String txnId) { return txnId != null && !txnId.isBlank() ? sendIso("respchktxn", IsoUtil.pack(iso), txnId, null) : null; }
    @Override public byte[] sendRespHbt(ISOMsg iso, String txnId) { return txnId != null && !txnId.isBlank() ? sendIso("resphbt", IsoUtil.pack(iso), txnId, null) : null; }
    @Override public byte[] sendRespValAdd(ISOMsg iso, String txnId) { return txnId != null && !txnId.isBlank() ? sendIso("respvaladd", IsoUtil.pack(iso), txnId, null) : null; }
}
