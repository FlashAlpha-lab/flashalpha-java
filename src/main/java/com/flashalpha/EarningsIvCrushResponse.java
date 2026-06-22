package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response for {@code GET /v1/earnings/iv-crush/{symbol}} — expected IV crush
 * for the next event plus the symbol's historical IV-crush distribution. Requires Growth+.
 *
 * <p>Obtain via {@link FlashAlphaClient#earningsIvCrushTyped(String)}.
 */
public final class EarningsIvCrushResponse {

    @SerializedName("symbol")
    public String symbol;

    @SerializedName("as_of")
    public String asOf;

    /** Next event date; {@code null} when no upcoming event but history exists. */
    @SerializedName("earnings_date")
    public String earningsDate;

    /** Live crush estimate; {@code null} when no upcoming event or term structure unresolved. */
    @SerializedName("current_estimate")
    public EarningsIvCrushEstimate currentEstimate;

    @SerializedName("distribution")
    public EarningsIvCrushDistribution distribution;

    /** The {@code current_estimate} block. */
    public static final class EarningsIvCrushEstimate {
        @SerializedName("expected_crush_pct") public Double expectedCrushPct;
        @SerializedName("pre_iv") public Double preIv;
        @SerializedName("post_iv") public Double postIv;
    }

    /** The historical {@code distribution} block. */
    public static final class EarningsIvCrushDistribution {
        @SerializedName("median") public Double median;
        @SerializedName("p25") public Double p25;
        @SerializedName("p75") public Double p75;
        @SerializedName("worst") public Double worst;
        @SerializedName("best") public Double best;
        @SerializedName("count") public Integer count;
    }
}
