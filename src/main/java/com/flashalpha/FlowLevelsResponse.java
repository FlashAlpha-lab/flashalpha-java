package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response for {@code GET /v1/flow/levels/{symbol}} (Alpha+). Gamma flip / call &amp; put walls / max pain recomputed against the live (intraday-flow-adjusted) book; each level is {@code null} when it can't be located.
 */
public final class FlowLevelsResponse {

    /** Underlying ticker echoed from the request path. */
    @SerializedName("symbol")
    public String symbol;

    /** Timestamp this snapshot was computed for (ISO-8601 UTC). */
    @SerializedName("as_of")
    public String asOf;

    /** Spot mid at {@link #asOf}. */
    @SerializedName("underlying_price")
    public Double underlyingPrice;

    /** Expiration filter echoed back (YYYY-MM-DD), or null for the whole chain. */
    @SerializedName("expiry")
    public String expiry;

    /** Spot where live net dealer gamma crosses zero. Null if no flip. */
    @SerializedName("live_gamma_flip")
    public Double liveGammaFlip;

    /** Strike of the largest live call-gamma concentration (upside magnet). */
    @SerializedName("live_call_wall")
    public Double liveCallWall;

    /** Strike of the largest live put-gamma concentration (downside magnet). */
    @SerializedName("live_put_wall")
    public Double livePutWall;

    /** Live max-pain strike (most option value expires worthless). */
    @SerializedName("live_max_pain")
    public Double liveMaxPain;
}
