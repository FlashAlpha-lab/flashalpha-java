package com.flashalpha;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Typed response for {@code GET /v1/flow/options/leaderboard} (Alpha+). Top-N net-dollar buyers and sellers (cached ~30s).
 */
public final class FlowOptionLeaderboardResponse {

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
     * One ranked underlying. Option rows carry {@code avgPremium};
     * the stock leaderboard uses {@code vwap} instead.
     */
    public static final class Row {
        /** Ranked underlying. */
        @SerializedName("symbol") public String symbol;
        /** Net contracts ({@code buyVolume - sellVolume}). */
        @SerializedName("netVolume") public Long netVolume;
        /** Net dollar option flow (approx net contracts x avg premium x 100). */
        @SerializedName("netNotional") public Double netNotional;
        /** Buy-classified contract volume. */
        @SerializedName("buyVolume") public Long buyVolume;
        /** Sell-classified contract volume. */
        @SerializedName("sellVolume") public Long sellVolume;
        /** Volume-weighted average option premium over the window. */
        @SerializedName("avgPremium") public Double avgPremium;
        /** Number of trades over the window. */
        @SerializedName("tradeCount") public Integer tradeCount;
        /** Timestamp of the most recent print. */
        @SerializedName("lastTradeUtc") public String lastTradeUtc;
    }
}
