package com.hitachi.imps.service.ack;

/**
 * Sends ACK to the caller (REST/HTTP to NPCI).
 */
@FunctionalInterface
public interface AckSender {
    void send(String ackXml);
}
