package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response for {@code GET /v1/flow/summary/{symbol}} (Alpha+). At-a-glance read on whether today's tape has shifted the dealer book.
 */
public final class FlowSummaryResponse {

    /** Underlying ticker echoed from the request path. */
    @SerializedName("symbol")
    public String symbol;

    /** Timestamp this snapshot was computed for (ISO-8601 UTC). */
    @SerializedName("as_of")
    public String asOf;

    /** Spot mid at the snapshot time. */
    @SerializedName("underlying_price")
    public Double underlyingPrice;

    /** Expiration filter echoed back, or null. */
    @SerializedName("expiry")
    public String expiry;

    /** Net classified direction of intraday flow (e.g. "bullish", "bearish", "neutral"). */
    @SerializedName("flow_direction")
    public String flowDirection;

    /** Net change in simulated open interest since the open (contracts). */
    @SerializedName("intraday_oi_delta")
    public Long intradayOiDelta;

    /** Contracts that have printed at least one trade today. */
    @SerializedName("contracts_with_flow")
    public Integer contractsWithFlow;

    /** Total contracts tracked for the underlying. */
    @SerializedName("contracts_total")
    public Integer contractsTotal;

    /** Live (flow-adjusted) net GEX (dollars per 1% spot move). */
    @SerializedName("live_gex")
    public Double liveGex;

    /** % shift in net GEX caused by today's flow vs the settled book; null when the settled baseline is zero. */
    @SerializedName("flow_gex_pct_shift")
    public Double flowGexPctShift;
}
