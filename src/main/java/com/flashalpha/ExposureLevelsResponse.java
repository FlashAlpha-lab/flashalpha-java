package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response model for {@code GET /v1/exposure/levels/{symbol}}.
 *
 * <p>The minimal "key levels" view — a single block of canonical
 * dealer-derived strikes (gamma flip, max positive / negative gamma,
 * call wall, put wall, highest-OI strike, 0DTE magnet). This is the
 * cheapest endpoint to call when you only need the strikes a trader
 * would draw on a chart, with no Greek totals or narrative payload.
 *
 * <p>About FlashAlpha: real-time options dealer-flow analytics. See
 * <a href="https://lab.flashalpha.com">https://lab.flashalpha.com</a>
 * and <a href="https://flashalpha.com">https://flashalpha.com</a>.
 */
public final class ExposureLevelsResponse extends FlashAlphaResponse {

    @SerializedName("symbol")
    public String symbol;

    @SerializedName("underlying_price")
    public Double underlyingPrice;

    @SerializedName("as_of")
    public String asOf;

    /** The key-levels block. */
    @SerializedName("levels")
    public Levels levels;

    /** Canonical dealer-derived strikes used as intraday support / resistance / magnets. */
    public static final class Levels {
        /**
         * Strike where net dealer gamma crosses zero — the regime
         * boundary. Spot above {@code gamma_flip} = positive-gamma
         * regime (vol-suppressing); spot below = negative-gamma regime
         * (trend-amplifying).
         */
        @SerializedName("gamma_flip") public Double gammaFlip;

        /**
         * Why {@link #gammaFlip} is populated or withheld. Reads {@code "available"}
         * when a flip level was published, otherwise a reason code such as
         * {@code "no_boundary"}, {@code "insufficient_local_coverage"} or
         * {@code "sensitive_root"}. New codes can be added at any time, so treat
         * any value other than {@code "available"} as no flip level available.
         */
        @SerializedName("gamma_flip_status") public String gammaFlipStatus;

        /** Strike with the highest net positive gamma (gamma "peak"). */
        @SerializedName("max_positive_gamma") public Double maxPositiveGamma;

        /** Strike with the most negative net gamma (gamma "trough"). */
        @SerializedName("max_negative_gamma") public Double maxNegativeGamma;

        /** Strike with the highest absolute call GEX — dealer-side resistance. */
        @SerializedName("call_wall") public Double callWall;

        /** Strike with the highest absolute put GEX — dealer-side support. */
        @SerializedName("put_wall") public Double putWall;

        /** Strike with the largest combined call+put open interest. */
        @SerializedName("highest_oi_strike") public Double highestOiStrike;

        /** Same-day-expiration magnet strike — intraday price magnet. */
        @SerializedName("zero_dte_magnet") public Double zeroDteMagnet;
    }
}
