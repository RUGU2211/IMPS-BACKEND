package com.hitachi.imps.service.socket;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Holds pending response futures for socket-originated requests.
 * When NPCI sends a request over TCP, we register a future; when Switch callback
 * (or local handling) produces the response, we complete it so the socket thread can send it.
 */
@Component
public class PendingSocketResponseStore {

    private final ConcurrentHashMap<String, CompletableFuture<String>> pending = new ConcurrentHashMap<>();

    /** Register a pending response for txnId. Socket thread will block on the returned future. */
    public CompletableFuture<String> registerPending(String txnId) {
        CompletableFuture<String> future = new CompletableFuture<>();
        pending.put(txnId, future);
        return future;
    }

    /** Complete the pending response (e.g. when Switch callback arrives). Returns true if there was a pending. */
    public boolean completePending(String txnId, String responseXml) {
        CompletableFuture<String> future = pending.remove(txnId);
        if (future != null) {
            future.complete(responseXml);
            return true;
        }
        return false;
    }

    /** Remove pending (e.g. on timeout) so we don't leak. */
    public void removePending(String txnId) {
        CompletableFuture<String> future = pending.remove(txnId);
        if (future != null) {
            future.cancel(true);
        }
    }
}
