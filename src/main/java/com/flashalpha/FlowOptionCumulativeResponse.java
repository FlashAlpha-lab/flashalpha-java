package com.flashalpha;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Typed response for {@code GET /v1/flow/options/{symbol}/cumulative} (Alpha+).
 */
public final class FlowOptionCumulativeResponse {

    /** Underlying ticker echoed from the request path. */
    @SerializedName("symbol")
    public String symbol;

    /** Expiration filter echoed back when supplied, else null. */
    @SerializedName("expiry")
    public String expiry;

    /** Lookback window in minutes (echoed back). */
    @SerializedName("minutes")
    public Integer minutes;

    /** Number of points returned. */
    @SerializedName("count")
    public Integer count;

    /** Chronological cumulative net-flow series. */
    @SerializedName("points")
    public List<FlowCumulativePoint> points;
}
