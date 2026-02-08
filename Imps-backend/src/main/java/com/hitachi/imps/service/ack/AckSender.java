package com.hitachi.imps.service.ack;

/**
 * Sends ACK to the caller (REST/HTTP to NPCI Mock).
 */
@FunctionalInterface
public interface AckSender {
    void send(String ackXml);
}
