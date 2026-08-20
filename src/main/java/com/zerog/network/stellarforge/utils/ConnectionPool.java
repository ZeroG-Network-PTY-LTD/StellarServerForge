package com.zerog.network.stellarforge.utils;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Shared OkHttpClient singleton with a tuned connection pool.
 *
 * All API clients should obtain their OkHttpClient from here rather than
 * creating separate instances, so connections are reused across the whole app.
 *
 * Usage:
 *   OkHttpClient client = ConnectionPool.getClient();
 */
public class ConnectionPool {

    private static final Logger log = LoggerFactory.getLogger(ConnectionPool.class);

    /** Max idle connections kept alive in the pool. */
    private static final int  MAX_IDLE_CONNECTIONS = 10;
    /** How long idle connections are kept before eviction (minutes). */
    private static final long KEEP_ALIVE_MINUTES   = 5;
    /** Connection timeout (TCP handshake). */
    private static final int  CONNECT_TIMEOUT_SEC  = 15;
    /** Read timeout (waiting for data). */
    private static final int  READ_TIMEOUT_SEC     = 30;
    /** Write timeout (uploading data). */
    private static final int  WRITE_TIMEOUT_SEC    = 30;

    private static volatile OkHttpClient client;

    private ConnectionPool() {}

    /**
     * Returns the shared OkHttpClient, creating it lazily on first call.
     */
    public static OkHttpClient getClient() {
        if (client == null) {
            synchronized (ConnectionPool.class) {
                if (client == null) {
                    client = buildClient();
                    log.debug("OkHttpClient connection pool initialised (max={}, keepAlive={}m)",
                            MAX_IDLE_CONNECTIONS, KEEP_ALIVE_MINUTES);
                }
            }
        }
        return client;
    }

    /**
     * Shuts down the connection pool, releasing all idle connections.
     * Call this when the application is closing.
     */
    public static synchronized void shutdown() {
        if (client != null) {
            client.connectionPool().evictAll();
            client.dispatcher().executorService().shutdown();
            client = null;
            log.debug("OkHttpClient connection pool shut down");
        }
    }

    /**
     * Returns a lightweight stats string for diagnostics.
     * E.g. "connections: 2 idle / 5 active"
     */
    public static String stats() {
        if (client == null) return "connection pool not initialised";
        okhttp3.ConnectionPool pool = client.connectionPool();
        return String.format("connections: %d idle / %d active",
                pool.idleConnectionCount(),
                pool.connectionCount() - pool.idleConnectionCount());
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    private static OkHttpClient buildClient() {
        okhttp3.ConnectionPool pool = new okhttp3.ConnectionPool(
                MAX_IDLE_CONNECTIONS,
                KEEP_ALIVE_MINUTES,
                TimeUnit.MINUTES);

        return new OkHttpClient.Builder()
                .connectionPool(pool)
                .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
                .readTimeout   (READ_TIMEOUT_SEC,    TimeUnit.SECONDS)
                .writeTimeout  (WRITE_TIMEOUT_SEC,   TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }
}

