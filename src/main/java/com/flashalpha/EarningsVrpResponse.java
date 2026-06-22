package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response for {@code GET /v1/earnings/vrp/{symbol}} — earnings
 * volatility-risk-premium: the live event-implied move vs. the symbol's realized
 * history of actual moves, with a richness assessment and surprise-reaction
 * breakdown. Requires Alpha+.
 *
 * <p>Obtain via {@link FlashAlphaClient#earningsVrpTyped(String)}.
 */
public final class EarningsVrpResponse {

    @SerializedName("symbol")
    public String symbol;

    @SerializedName("underlying_price")
    public Double underlyingPrice;

    @SerializedName("as_of")
    public String asOf;

    @SerializedName("earnings_date")
    public String earningsDate;

    @SerializedName("days_to_event")
    public Integer daysToEvent;

    @SerializedName("earnings_vrp")
    public EarningsVrpBlock earningsVrp;

    @SerializedName("surprise_reaction")
    public EarningsSurpriseReaction surpriseReaction;

    /** The {@code earnings_vrp} block. */
    public static final class EarningsVrpBlock {
        @SerializedName("implied_move_pct") public Double impliedMovePct;
        @SerializedName("realized_median") public Double realizedMedian;
        @SerializedName("realized_mean") public Double realizedMean;
        /** {@code implied_move / realized_median}. &gt;1 means options price more than history realized. */
        @SerializedName("premium_ratio") public Double premiumRatio;
        /** {@code null} when fewer than 5 historical moves. */
        @SerializedName("z_score") public Double zScore;
        @SerializedName("percentile") public Double percentile;
        /** {@code rich} / {@code slightly_rich} / {@code fair} / {@code slightly_cheap} / {@code cheap} / {@code insufficient_data}. */
        @SerializedName("assessment") public String assessment;
        /** {@code downside_overpriced} / {@code upside_overpriced}; otherwise null. */
        @SerializedName("directional_bias") public String directionalBias;
    }

    /** The {@code surprise_reaction} block. */
    public static final class EarningsSurpriseReaction {
        @SerializedName("beat_avg_move_pct") public Double beatAvgMovePct;
        @SerializedName("miss_avg_move_pct") public Double missAvgMovePct;
        @SerializedName("inline_avg_move_pct") public Double inlineAvgMovePct;
    }
}
