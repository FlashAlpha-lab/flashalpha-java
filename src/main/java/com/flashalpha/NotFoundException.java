package com.flashalpha;

import com.google.gson.JsonObject;

/**
 * Thrown when the API returns HTTP 404 Not Found.
 * The requested symbol, ticker, or resource does not exist.
 */
public class NotFoundException extends FlashAlphaException {

    public NotFoundException(String message, JsonObject response) {
        super(message, 404, response);
    }
}
