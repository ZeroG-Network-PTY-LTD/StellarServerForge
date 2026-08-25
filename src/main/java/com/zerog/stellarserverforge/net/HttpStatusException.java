package com.zerog.stellarserverforge.net;

import java.io.IOException;

/** Thrown by {@link HttpFetcher} for a non-2xx response, carrying the actual status code so
 * callers can distinguish e.g. "404 — this optional resource genuinely doesn't exist" from a
 * transient 5xx/network condition, instead of treating every failure identically. */
public class HttpStatusException extends IOException {

    private final int statusCode;

    public HttpStatusException(int statusCode, String url) {
        super("HTTP " + statusCode + " fetching " + url);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }

    public boolean isNotFound() {
        return statusCode == 404;
    }
}
