package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Request body for {@code POST /v1/structures/greeks} — aggregate Black-Scholes
 * Greeks across a multi-leg position. Each leg carries its own expiry and
 * implied vol so calendars and diagonals aggregate correctly. Pure math: no
 * market-data lookup.
 *
 * <p>Use with {@link FlashAlphaClient#structureGreeks(StructureGreeksRequest)} →
 * {@link StructureGreeksResponse}. Each leg must supply {@code action}, {@code type},
 * {@code strike}, {@code expiry}, {@code impliedVol}, and (optionally) {@code quantity}.
 */
public final class StructureGreeksRequest {

    /** One or more legs. Must be non-empty. Each leg carries action / type / strike / expiry / impliedVol / quantity. */
    @SerializedName("legs")
    public List<StructureLeg> legs;

    /** Underlying spot price priced against (required; must be {@code > 0}). */
    @SerializedName("spot")
    public Double spot;

    /** Valuation date {@code yyyy-MM-dd} (nullable; defaults to today UTC server-side). */
    @SerializedName("today")
    public String today;

    /** Risk-free rate as a decimal (nullable; default {@code 0.045}). */
    @SerializedName("rate")
    public Double rate;

    /** Continuous dividend yield as a decimal (nullable; default {@code 0.013}). */
    @SerializedName("dividendYield")
    public Double dividendYield;

    public StructureGreeksRequest() {
    }

    public StructureGreeksRequest(List<StructureLeg> legs, Double spot) {
        this.legs = legs;
        this.spot = spot;
    }

    public StructureGreeksRequest(List<StructureLeg> legs, Double spot, String today,
                                  Double rate, Double dividendYield) {
        this.legs = legs;
        this.spot = spot;
        this.today = today;
        this.rate = rate;
        this.dividendYield = dividendYield;
    }
}
