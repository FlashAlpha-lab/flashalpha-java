package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response model for {@code GET /v1/exposure/summary/{symbol}}.
 *
 * <p>This is a strongly-typed mirror of the JSON response. The original
 * {@link FlashAlphaClient#exposureSummary(String)} method continues to return
 * {@link com.google.gson.JsonObject}; a parallel typed wrapper will be added
 * alongside this class.
 *
 * <p>All numeric fields are boxed wrappers ({@link Double}) so that
 * {@code null} can represent values the API could not compute (insufficient
 * data, market closed, etc.).
 *
 * <p><b>Direction casing:</b> confirmed via live probe — both
 * {@code /v1/exposure/summary/} and {@code /v1/exposure/zero-dte/} return
 * lowercase {@code "buy"}/{@code "sell"}. Casing is consistent across
 * summary and zero-DTE endpoints.
 */
public final class ExposureSummaryResponse {

    @SerializedName("symbol")
    public String symbol;

    @SerializedName("underlying_price")
    public Double underlyingPrice;

    @SerializedName("as_of")
    public String asOf;

    @SerializedName("gamma_flip")
    public Double gammaFlip;

    /** One of "positive_gamma", "negative_gamma", "unknown". */
    @SerializedName("regime")
    public String regime;

    @SerializedName("exposures")
    public Exposures exposures;

    @SerializedName("interpretation")
    public Interpretation interpretation;

    @SerializedName("hedging_estimate")
    public HedgingEstimate hedgingEstimate;

    @SerializedName("zero_dte")
    public ZeroDte zeroDte;

    /** Net exposure totals across the entire chain. */
    public static final class Exposures {
        @SerializedName("net_gex")  public Double netGex;
        @SerializedName("net_dex")  public Double netDex;
        @SerializedName("net_vex")  public Double netVex;
        @SerializedName("net_chex") public Double netChex;
    }

    /** Verbal interpretation of the gamma/vanna/charm regimes. */
    public static final class Interpretation {
        @SerializedName("gamma") public String gamma;
        @SerializedName("vanna") public String vanna;
        @SerializedName("charm") public String charm;
    }

    /** One side (up or down) of a dealer-hedging estimate. */
    public static final class HedgingMove {
        @SerializedName("dealer_shares_to_trade") public Double dealerSharesToTrade;
        /** "buy" or "sell" (lowercase on both this endpoint and zero-dte). */
        @SerializedName("direction")              public String direction;
        @SerializedName("notional_usd")           public Double notionalUsd;
    }

    /** Estimated dealer hedging flow at +/- 1% spot moves. */
    public static final class HedgingEstimate {
        @SerializedName("spot_up_1pct")   public HedgingMove spotUp1Pct;
        @SerializedName("spot_down_1pct") public HedgingMove spotDown1Pct;
    }

    /** Same-day-expiration contribution to total GEX. */
    public static final class ZeroDte {
        @SerializedName("net_gex")          public Double netGex;
        @SerializedName("pct_of_total_gex") public Double pctOfTotalGex;
        @SerializedName("expiration")       public String expiration;
    }
}
