package com.flashalpha;

import com.google.gson.JsonObject;

/**
 * Thrown when the API returns an HTTP 5xx server error.
 */
public class ServerException extends FlashAlphaException {

    public ServerException(String message, int statusCode, JsonObject response) {
        super(message, statusCode, response);
    }
}
