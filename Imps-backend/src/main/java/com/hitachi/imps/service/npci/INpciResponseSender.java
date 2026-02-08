package com.hitachi.imps.service.npci;

/**
 * Sends response XML to NPCI.
 * Implementation depends on npci.compliant-flow:
 * - compliant: NpciSocketClientSender (outbound socket to NPCI)
 * - same-connection: PendingSocketResponseSender (writes to request socket)
 */
public interface INpciResponseSender {

    /**
     * Send response to NPCI.
     *
     * @param txnId   transaction ID
     * @param respXml response XML (RespPay, RespHbt, etc.)
     * @return true if sent successfully (or will be sent on same connection)
     */
    boolean send(String txnId, String respXml);
}
