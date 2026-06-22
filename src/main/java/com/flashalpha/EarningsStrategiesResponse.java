package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response for {@code GET /v1/earnings/strategies/{symbol}} — strategy-suitability
 * scores (0-100) for the upcoming event across common earnings structures, blending
 * implied move, VRP premium ratio, expected IV crush, ATM liquidity, and the gamma
 * regime. Requires Alpha+.
 *
 * <p>Obtain via {@link FlashAlphaClient#earningsStrategiesTyped(String)}.
 */
public final class EarningsStrategiesResponse {

    @SerializedName("symbol")
    public String symbol;

    @SerializedName("as_of")
    public String asOf;

    @SerializedName("earnings_date")
    public String earningsDate;

    @SerializedName("scores")
    public EarningsStrategyScores scores;

    @SerializedName("context")
    public EarningsStrategyContext context;

    /** The {@code scores} block (0-100 per structure). */
    public static final class EarningsStrategyScores {
        @SerializedName("long_straddle") public Integer longStraddle;
        @SerializedName("short_strangle") public Integer shortStrangle;
        @SerializedName("iron_condor") public Integer ironCondor;
        @SerializedName("calendar_spread") public Integer calendarSpread;
        @SerializedName("earnings_diagonal") public Integer earningsDiagonal;
    }

    /** The {@code context} block. */
    public static final class EarningsStrategyContext {
        @SerializedName("premium_ratio") public Double premiumRatio;
        @SerializedName("iv_crush_median") public Double ivCrushMedian;
        @SerializedName("regime") public String regime;
        @SerializedName("implied_move_pct") public Double impliedMovePct;
    }
}
