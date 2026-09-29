package com.hypixeltracker.server;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Zwischenspeicher fuer erfolgreiche Hypixel-Antworten.
 * Gleichzeitige Anfragen fuer denselben Schluessel teilen sich einen
 * einzigen Hypixel-Aufruf, damit das Rate-Limit geschont wird.
 */
public class ResponseCache<V> {

    @FunctionalInterface
    public interface Loader<V> {
        V load() throws Exception;
    }

    private record Entry<V>(CompletableFuture<V> future, Instant expiresAt) {
    }

    private final Map<String, Entry<V>> entries = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock clock;

    public ResponseCache(Duration ttl, Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    /**
     * Liefert den gecachten Wert oder laedt ihn. Wirft der Loader eine
     * Exception, wird nichts gespeichert und die Exception weitergereicht.
     */
    public V get(String key, Loader<V> loader) throws Exception {
        CompletableFuture<V> ownFuture = new CompletableFuture<>();
        Entry<V> entry = entries.compute(key, (k, existing) -> {
            if (existing != null && (!existing.future().isDone() || clock.instant().isBefore(existing.expiresAt()))) {
                return existing;
            }
            // Ablaufzeit wird erst nach erfolgreichem Laden gesetzt
            return new Entry<>(ownFuture, Instant.MAX);
        });

        if (entry.future() == ownFuture) {
            try {
                V value = loader.load();
                entries.put(key, new Entry<>(ownFuture, clock.instant().plus(ttl)));
                ownFuture.complete(value);
            } catch (Exception e) {
                entries.remove(key, entry);
                ownFuture.completeExceptionally(e);
                throw e;
            }
        }

        try {
            return entry.future().get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw e;
        }
    }

    /** Entfernt abgelaufene Eintraege, damit der Speicher nicht waechst. */
    public void evictExpired() {
        Instant now = clock.instant();
        entries.values().removeIf(entry -> entry.future().isDone() && !now.isBefore(entry.expiresAt()));
    }

    public int size() {
        return entries.size();
    }
}
