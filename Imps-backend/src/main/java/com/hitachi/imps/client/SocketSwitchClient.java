package com.hitachi.imps.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocket;

import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.hitachi.imps.config.RoutingConfig;
import com.hitachi.imps.config.SslConfig;
import com.hitachi.imps.iso.ImpsIsoPackager;
import com.hitachi.imps.service.routing.SwitchAddressResolver;
import com.hitachi.imps.service.routing.SwitchAddressResolver.SwitchAddress;
import com.hitachi.imps.util.IsoUtil;

/**
 * Switch client via TCP socket.
 * Uses institution_master (lookup by request_org_id) for switch_ip/switch_port; falls back to application.yml.
 */
@Component
@ConditionalOnProperty(name = "routing.switch.socket.enabled", havingValue = "true", matchIfMissing = true)
public class SocketSwitchClient implements ISwitchClient {

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
        try {
            if (useSsl) {
                var ctx = SslConfig.buildClientContext(
                    sockConfig.getTrustStore(), sockConfig.getTrustStorePassword(),
                    sockConfig.getKeyStore(), sockConfig.getKeyStorePassword(), "JKS");
                SSLSocket ssl = (SSLSocket) ctx.getSocketFactory().createSocket();
                ssl.connect(new InetSocketAddress(host, port), 10000);
                ssl.startHandshake();
                s = ssl;
            } else {
                s = new Socket(host, port);
            }
            s.setSoTimeout(READ_TIMEOUT_MS);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            DataInputStream in = new DataInputStream(s.getInputStream());
            out.writeInt(isoBytes.length);
            out.write(isoBytes);
            out.flush();
            int respLen = in.readInt();
            if (respLen <= 0 || respLen > MAX_ISO_SIZE) return null;
            byte[] resp = new byte[respLen];
            in.readFully(resp);
            String isoDisplay = formatIsoForConsole(resp);
            System.out.println("[IMPS] Resp ISO received from Switch (" + apiType + "/" + txnId + "):");
            System.out.println(isoDisplay);
            return resp;
        } catch (IOException e) {
            System.err.println("Switch socket send failed [" + apiType + "/" + txnId + "]: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Switch SSL setup failed [" + apiType + "/" + txnId + "]: " + e.getMessage());
            return null;
        } finally {
            if (s != null) try { s.close(); } catch (IOException ignored) {}
        }
    }

    private static String formatIsoForConsole(byte[] data) {
        try {
            ISOMsg iso = IsoUtil.unpack(data, new ImpsIsoPackager());
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

    @Override
    public byte[] sendReqPay(ISOMsg iso, String txnId) {
        return sendReqPay(iso, txnId, null);
    }

    @Override
    public byte[] sendReqPay(ISOMsg iso, String txnId, String requestOrgId) {
        return sendIso("reqpay", IsoUtil.pack(iso), txnId, requestOrgId);
    }

    @Override
    public byte[] sendReqPay(byte[] isoBytes, String txnId) {
        return sendReqPay(isoBytes, txnId, null);
    }

    @Override
    public byte[] sendReqPay(byte[] isoBytes, String txnId, String requestOrgId) {
        return sendIso("reqpay", isoBytes, txnId, requestOrgId);
    }

    @Override
    public byte[] sendReqChkTxn(ISOMsg iso, String txnId) {
        return sendReqChkTxn(iso, txnId, null);
    }

    @Override
    public byte[] sendReqChkTxn(ISOMsg iso, String txnId, String requestOrgId) {
        return sendIso("reqchktxn", IsoUtil.pack(iso), txnId, requestOrgId);
    }

    @Override
    public byte[] sendReqChkTxn(byte[] isoBytes, String txnId) {
        return sendReqChkTxn(isoBytes, txnId, null);
    }

    @Override
    public byte[] sendReqChkTxn(byte[] isoBytes, String txnId, String requestOrgId) {
        return sendIso("reqchktxn", isoBytes, txnId, requestOrgId);
    }

    @Override
    public byte[] sendReqHbt(ISOMsg iso, String txnId) {
        return sendReqHbt(iso, txnId, null);
    }

    @Override
    public byte[] sendReqHbt(ISOMsg iso, String txnId, String requestOrgId) {
        return sendIso("reqhbt", IsoUtil.pack(iso), txnId, requestOrgId);
    }

    @Override
    public byte[] sendReqHbt(byte[] isoBytes, String txnId) {
        return sendReqHbt(isoBytes, txnId, null);
    }

    @Override
    public byte[] sendReqHbt(byte[] isoBytes, String txnId, String requestOrgId) {
        return sendIso("reqhbt", isoBytes, txnId, requestOrgId);
    }

    @Override
    public byte[] sendReqListAccPvd(ISOMsg iso, String txnId) {
        return sendReqListAccPvd(iso, txnId, null);
    }

    @Override
    public byte[] sendReqListAccPvd(ISOMsg iso, String txnId, String requestOrgId) {
        return sendIso("reqlistaccpvd", IsoUtil.pack(iso), txnId, requestOrgId);
    }

    @Override
    public byte[] sendReqListAccPvd(byte[] isoBytes, String txnId) {
        return sendReqListAccPvd(isoBytes, txnId, null);
    }

    @Override
    public byte[] sendReqListAccPvd(byte[] isoBytes, String txnId, String requestOrgId) {
        return sendIso("reqlistaccpvd", isoBytes, txnId, requestOrgId);
    }

    @Override
    public byte[] sendReqValAdd(ISOMsg iso, String txnId) {
        return sendReqValAdd(iso, txnId, null);
    }

    @Override
    public byte[] sendReqValAdd(ISOMsg iso, String txnId, String requestOrgId) {
        return sendIso("reqvaladd", IsoUtil.pack(iso), txnId, requestOrgId);
    }

    @Override
    public byte[] sendReqValAdd(byte[] isoBytes, String txnId) {
        return sendReqValAdd(isoBytes, txnId, null);
    }

    @Override
    public byte[] sendReqValAdd(byte[] isoBytes, String txnId, String requestOrgId) {
        return sendIso("reqvaladd", isoBytes, txnId, requestOrgId);
    }

    @Override
    public byte[] sendRespPay(ISOMsg iso, String txnId) {
        return txnId != null && !txnId.isBlank() ? sendIso("resppay", IsoUtil.pack(iso), txnId, null) : null;
    }

    @Override
    public byte[] sendRespChkTxn(ISOMsg iso, String txnId) {
        return txnId != null && !txnId.isBlank() ? sendIso("respchktxn", IsoUtil.pack(iso), txnId, null) : null;
    }

    @Override
    public byte[] sendRespHbt(ISOMsg iso, String txnId) {
        return txnId != null && !txnId.isBlank() ? sendIso("resphbt", IsoUtil.pack(iso), txnId, null) : null;
    }

    @Override
    public byte[] sendRespValAdd(ISOMsg iso, String txnId) {
        return txnId != null && !txnId.isBlank() ? sendIso("respvaladd", IsoUtil.pack(iso), txnId, null) : null;
    }
}
