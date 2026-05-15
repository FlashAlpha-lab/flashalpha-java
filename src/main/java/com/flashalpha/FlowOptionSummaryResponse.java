package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response for {@code GET /v1/flow/options/{symbol}/summary} (Alpha+). Per-underlying option-flow aggregates.
 */
public final class FlowOptionSummaryResponse {

    /** Underlying ticker echoed from the request path. */
    @SerializedName("symbol")
    public String symbol;

    /** Expiration filter echoed back when supplied, else null. */
    @SerializedName("expiry")
    public String expiry;

    /** Distinct contracts that printed at least one trade. */
    @SerializedName("contractsWithTrades")
    public Integer contractsWithTrades;

    /** Total number of trade prints. */
    @SerializedName("totalTrades")
    public Integer totalTrades;

    /** Buy-classified contract volume. */
    @SerializedName("buyVolume")
    public Long buyVolume;

    /** Sell-classified contract volume. */
    @SerializedName("sellVolume")
    public Long sellVolume;

    /** Volume classified at the mid (uninformed). */
    @SerializedName("midVolume")
    public Long midVolume;

    /** {@code buyVolume - sellVolume}. */
    @SerializedName("netVolume")
    public Long netVolume;

    /** Largest single trade size. */
    @SerializedName("biggestSingleTrade")
    public Integer biggestSingleTrade;

    /** Timestamp of the most recent print; null when no trades. */
    @SerializedName("lastTradeUtc")
    public String lastTradeUtc;
}
