package com.flashalpha;

import com.google.gson.JsonObject;

/**
 * Thrown when the API returns HTTP 429 Too Many Requests.
 * Respect the {@code retryAfter} value before retrying.
 */
public class RateLimitException extends FlashAlphaException {

    private final Integer retryAfter;

    public RateLimitException(String message, JsonObject response, Integer retryAfter) {
        super(message, 429, response);
        this.retryAfter = retryAfter;
    }

    /**
     * Seconds to wait before retrying, as reported by the {@code Retry-After} response header.
     * May be null if the header was absent.
     */
    public Integer getRetryAfter() {
        return retryAfter;
    }
}
