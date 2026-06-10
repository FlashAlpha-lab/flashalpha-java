package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response for {@code POST /v1/structures/greeks} (Basic+) — aggregate
 * position Greeks across a multi-leg option structure. Each Greek is signed for
 * direction (long {@code +}, short {@code −}), scaled by quantity, and summed
 * across legs.
 *
 * <p>Obtain via {@link FlashAlphaClient#structureGreeksTyped(StructureGreeksRequest)}.
 */
public final class StructureGreeksResponse {

    /** Underlying spot priced against. */
    @SerializedName("spot")
    public Double spot;

    /** ISO 8601 UTC time the response was built. */
    @SerializedName("as_of")
    public String asOf;

    /** Resolved valuation date ({@code yyyy-MM-dd}). */
    @SerializedName("valuation_date")
    public String valuationDate;

    /** Risk-free rate used (decimal). */
    @SerializedName("rate")
    public Double rate;

    /** Continuous dividend yield used (decimal). */
    @SerializedName("dividend_yield")
    public Double dividendYield;

    /** Echo of the request legs. */
    @SerializedName("legs")
    public List<StructureLeg> legs;

    /** Aggregated position Greeks. */
    @SerializedName("position_greeks")
    public PositionGreeks positionGreeks;

    /** Aggregated, quantity-scaled, sign-aware position Greeks. */
    public static final class PositionGreeks {
        @SerializedName("delta") public Double delta;
        @SerializedName("gamma") public Double gamma;
        @SerializedName("theta") public Double theta;
        @SerializedName("vega") public Double vega;
        @SerializedName("rho") public Double rho;
        @SerializedName("vanna") public Double vanna;
        @SerializedName("charm") public Double charm;
    }
}
