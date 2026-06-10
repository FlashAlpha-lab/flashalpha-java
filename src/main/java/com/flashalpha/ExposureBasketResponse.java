package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response for {@code GET /v1/exposure/basket} (Growth+) — weighted
 * cross-symbol aggregate of GEX / DEX / VEX / CHEX across up to 50 symbols.
 * Equal weights when {@code weights} is omitted; otherwise normalised to sum 1.
 *
 * <p>Obtain via {@link FlashAlphaClient#exposureBasketTyped(String, String)}.
 */
public final class ExposureBasketResponse {

    /** ISO 8601 UTC build time. */
    @SerializedName("as_of")
    public String asOf;

    /** Number of symbols that actually contributed (after drops). */
    @SerializedName("constituent_count")
    public Integer constituentCount;

    /** Symbols requested but with no available data. */
    @SerializedName("missing_symbols")
    public List<String> missingSymbols;

    /** Weighted aggregate net greeks. */
    @SerializedName("aggregate")
    public Aggregate aggregate;

    /** Per-symbol breakdown. */
    @SerializedName("constituents")
    public List<Constituent> constituents;

    /** Weighted aggregate greeks. */
    public static final class Aggregate {
        @SerializedName("net_gex") public Double netGex;
        @SerializedName("net_dex") public Double netDex;
        @SerializedName("net_vex") public Double netVex;
        @SerializedName("net_chex") public Double netChex;
    }

    /** One basket constituent. */
    public static final class Constituent {
        @SerializedName("symbol") public String symbol;
        /** Renormalised weight applied to this symbol. */
        @SerializedName("weight") public Double weight;
        @SerializedName("underlying_price") public Double underlyingPrice;
        @SerializedName("net_gex") public Double netGex;
        @SerializedName("net_dex") public Double netDex;
        @SerializedName("net_vex") public Double netVex;
        @SerializedName("net_chex") public Double netChex;
        /** Weighted-GEX contribution share (0-100). */
        @SerializedName("contribution_pct") public Double contributionPct;
        /** {@code positive_gamma} when {@code net_gex >= 0}, else {@code negative_gamma}. */
        @SerializedName("regime") public String regime;
    }
}
