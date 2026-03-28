package com.flashalpha;

import com.google.gson.JsonObject;

/**
 * Base exception for all FlashAlpha API errors.
 */
public class FlashAlphaException extends RuntimeException {

    private final int statusCode;
    private final JsonObject response;

    public FlashAlphaException(String message, int statusCode, JsonObject response) {
        super(message);
        this.statusCode = statusCode;
        this.response = response;
    }

    /** The HTTP status code returned by the API. */
    public int getStatusCode() {
        return statusCode;
    }

    /** The raw JSON response body, or null if the body was not parseable. */
    public JsonObject getResponse() {
        return response;
    }
}
