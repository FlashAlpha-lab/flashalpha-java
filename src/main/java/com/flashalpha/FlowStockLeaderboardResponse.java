package com.flashalpha;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Typed response for {@code GET /v1/flow/stocks/leaderboard} (Alpha+). Top-N net-dollar buyers and sellers (cached ~30s).
 */
public final class FlowStockLeaderboardResponse {

    /** When the cached snapshot was generated (ISO-8601 UTC). */
    @SerializedName("generatedUtc")
    public String generatedUtc;

    /** Number of ranked rows requested per side. */
    @SerializedName("n")
    public Integer n;

    /** Aggregation window in minutes. */
    @SerializedName("windowMinutes")
    public Integer windowMinutes;

    /** Top net-dollar buyers. */
    @SerializedName("buyers")
    public List<Row> buyers;

    /** Top net-dollar sellers. */
    @SerializedName("sellers")
    public List<Row> sellers;


    /**
     * One ranked symbol. Stock rows carry {@code vwap};
     * the option leaderboard uses {@code avgPremium} instead.
     */
    public static final class Row {
        /** Ranked symbol. */
        @SerializedName("symbol") public String symbol;
        /** Net shares ({@code buyVolume - sellVolume}). */
        @SerializedName("netVolume") public Long netVolume;
        /** Net dollar flow (net shares x VWAP). */
        @SerializedName("netNotional") public Double netNotional;
        /** Buy-classified share volume. */
        @SerializedName("buyVolume") public Long buyVolume;
        /** Sell-classified share volume. */
        @SerializedName("sellVolume") public Long sellVolume;
        /** Volume-weighted average trade price over the window. */
        @SerializedName("vwap") public Double vwap;
        /** Number of trades over the window. */
        @SerializedName("tradeCount") public Integer tradeCount;
        /** Timestamp of the most recent print. */
        @SerializedName("lastTradeUtc") public String lastTradeUtc;
    }
}
