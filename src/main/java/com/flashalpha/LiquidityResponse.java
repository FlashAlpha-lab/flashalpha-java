package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response for {@code GET /v1/liquidity/{symbol}} (Growth+) — per-expiry
 * options execution score (0-100), ATM bid-ask spread %, OI-weighted spread %,
 * ATM OI depth, plus chain-level OI-weighted score and best/worst expiry.
 *
 * <p>Obtain via {@link FlashAlphaClient#liquidityTyped(String)}.
 */
public final class LiquidityResponse {

    @SerializedName("symbol")
    public String symbol;

    @SerializedName("underlying_price")
    public Double underlyingPrice;

    @SerializedName("as_of")
    public String asOf;

    /** OI-weighted average of per-expiry execution scores (0-100). */
    @SerializedName("chain_execution_score")
    public Integer chainExecutionScore;

    @SerializedName("best_expiry")
    public String bestExpiry;

    @SerializedName("worst_expiry")
    public String worstExpiry;

    /** Number of expiries labelled {@code illiquid}. */
    @SerializedName("thin_expiry_count")
    public Integer thinExpiryCount;

    /** Per-expiry execution detail. */
    @SerializedName("expiries")
    public List<ExpiryLiquidity> expiries;

    /** One expiry's liquidity detail. */
    public static final class ExpiryLiquidity {
        @SerializedName("expiration") public String expiration;
        @SerializedName("dte") public Integer dte;
        /** Average bid-ask spread % at the ATM strike; {@code null} when neither side quotes. */
        @SerializedName("atm_spread_pct") public Double atmSpreadPct;
        /** OI-weighted bid-ask spread %; {@code null} when no contract qualifies. */
        @SerializedName("weighted_spread_pct") public Double weightedSpreadPct;
        /** Sum of call+put OI at the ATM strike. */
        @SerializedName("atm_oi") public Long atmOi;
        /** 0-100 composite execution score. */
        @SerializedName("execution_score") public Integer executionScore;
        /** {@code tight} / {@code normal} / {@code wide} / {@code illiquid}. */
        @SerializedName("label") public String label;
    }
}
