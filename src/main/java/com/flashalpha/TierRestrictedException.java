package com.flashalpha;

import com.google.gson.JsonObject;

/**
 * Thrown when the API returns HTTP 403 Forbidden.
 * The requested endpoint requires a higher subscription tier.
 */
public class TierRestrictedException extends FlashAlphaException {

    private final String currentPlan;
    private final String requiredPlan;

    public TierRestrictedException(String message, JsonObject response, String currentPlan, String requiredPlan) {
        super(message, 403, response);
        this.currentPlan = currentPlan;
        this.requiredPlan = requiredPlan;
    }

    /** The caller's current subscription plan. */
    public String getCurrentPlan() {
        return currentPlan;
    }

    /** The minimum plan required to access the requested resource. */
    public String getRequiredPlan() {
        return requiredPlan;
    }
}
