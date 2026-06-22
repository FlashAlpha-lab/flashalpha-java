package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response for {@code GET /v1/earnings/expected-move/{symbol}} — live
 * earnings-implied move decomposition for the next event, splitting the front-expiry
 * straddle into the earnings-jump component vs. baseline diffusion drift. Requires Growth+.
 *
 * <p>Obtain via {@link FlashAlphaClient#earningsExpectedMoveTyped(String)}.
 */
public final class EarningsExpectedMoveResponse {

    @SerializedName("symbol")
    public String symbol;

    @SerializedName("underlying_price")
    public Double underlyingPrice;

    @SerializedName("as_of")
    public String asOf;

    @SerializedName("earnings_date")
    public String earningsDate;

    @SerializedName("session")
    public String session;

    @SerializedName("days_to_event")
    public Integer daysToEvent;

    /** {@code null} when the pre/post-event expiry IVs cannot be resolved. */
    @SerializedName("expected_move")
    public EarningsExpectedMoveBlock expectedMove;

    /** The {@code expected_move} block. */
    public static final class EarningsExpectedMoveBlock {
        @SerializedName("raw_straddle_pct") public Double rawStraddlePct;
        @SerializedName("earnings_implied_pct") public Double earningsImpliedPct;
        @SerializedName("baseline_drift_pct") public Double baselineDriftPct;
        @SerializedName("earnings_iv") public Double earningsIv;
        @SerializedName("term_iv_post_event") public Double termIvPostEvent;
        @SerializedName("term_kink_pct") public Double termKinkPct;
    }
}
