package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response for {@code GET /v1/flow/stocks/{symbol}/summary} (Alpha+). Per-symbol stock-flow aggregates.
 */
public final class FlowStockSummaryResponse {

    /** Underlying ticker echoed from the request path. */
    @SerializedName("symbol")
    public String symbol;

    /** Total number of trade prints. */
    @SerializedName("totalTrades")
    public Integer totalTrades;

    /** Buy-classified share volume. */
    @SerializedName("buyVolume")
    public Long buyVolume;

    /** Sell-classified share volume. */
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
