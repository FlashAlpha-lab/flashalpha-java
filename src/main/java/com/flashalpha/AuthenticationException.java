package com.flashalpha;

import com.google.gson.JsonObject;

/**
 * Thrown when the API returns HTTP 401 Unauthorized.
 * Check that your API key is valid and has not expired.
 */
public class AuthenticationException extends FlashAlphaException {

    public AuthenticationException(String message, JsonObject response) {
        super(message, 401, response);
    }
}
