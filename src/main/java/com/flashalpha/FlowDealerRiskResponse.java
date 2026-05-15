package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response for {@code GET /v1/flow/dealer-risk/{symbol}} (Alpha+). Side-by-side of the settled snapshot and the live flow-adjusted book.
 */
public final class FlowDealerRiskResponse {

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

    /** Net GEX from the settled (prior close) snapshot. */
    @SerializedName("settled_net_gex")
    public Double settledNetGex;

    /** Net GEX from the live flow-adjusted book. */
    @SerializedName("live_net_gex")
    public Double liveNetGex;

    /** {@code live_net_gex - settled_net_gex} (dollars). */
    @SerializedName("flow_gex_adjustment")
    public Double flowGexAdjustment;

    /** % GEX shift from flow; null when the settled baseline is zero. */
    @SerializedName("flow_gex_pct_shift")
    public Double flowGexPctShift;

    /** Net DEX from the settled snapshot. */
    @SerializedName("settled_net_dex")
    public Double settledNetDex;

    /** Net DEX from the live flow-adjusted book. */
    @SerializedName("live_net_dex")
    public Double liveNetDex;

    /** {@code live_net_dex - settled_net_dex} (dollars). */
    @SerializedName("flow_dex_adjustment")
    public Double flowDexAdjustment;

    /** % DEX shift from flow; null when the settled baseline is zero. */
    @SerializedName("flow_dex_pct_shift")
    public Double flowDexPctShift;

    /** Absolute delta-weighted contracts traded today (flow magnitude). */
    @SerializedName("total_abs_delta_contracts")
    public Long totalAbsDeltaContracts;

    /** Contracts that printed at least one trade today. */
    @SerializedName("contracts_with_flow")
    public Integer contractsWithFlow;

    /** Net classified flow direction. */
    @SerializedName("flow_direction")
    public String flowDirection;

    /** Plain-English summary of whether flow has moved the dealer book. Safe to surface verbatim. */
    @SerializedName("description")
    public String description;
}
