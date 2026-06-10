package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response for {@code GET /v1/exposure/sheet/{symbol}} (Growth+) — a
 * unified per-strike rowset joining GEX, DEX, VEX, CHEX, and DAG
 * (delta-adjusted gamma), plus chain totals, the Line-in-the-Sand inflection
 * strike, all gamma peaks, and OPEX / triple-witching flags when an expiration
 * filter is supplied.
 *
 * <p>Obtain via {@link FlashAlphaClient#exposureSheetTyped(String)} or
 * {@link FlashAlphaClient#exposureSheetTyped(String, String, Integer)}.
 */
public final class ExposureSheetResponse {

    @SerializedName("symbol") public String symbol;
    @SerializedName("underlying_price") public Double underlyingPrice;
    @SerializedName("as_of") public String asOf;
    /** The expiration filter applied (nullable when aggregating the full chain). */
    @SerializedName("expiration") public String expiration;
    @SerializedName("is_opex") public Boolean isOpex;
    @SerializedName("is_triple_witching") public Boolean isTripleWitching;
    @SerializedName("totals") public Totals totals;
    /** Line-in-the-Sand inflection strike (nullable when <3 strikes). */
    @SerializedName("lis") public Lis lis;
    @SerializedName("peaks") public List<Peak> peaks;
    @SerializedName("strikes") public List<StrikeRow> strikes;

    /** Chain totals across all greeks. */
    public static final class Totals {
        @SerializedName("net_gex") public Double netGex;
        @SerializedName("net_dex") public Double netDex;
        @SerializedName("net_vex") public Double netVex;
        @SerializedName("net_chex") public Double netChex;
        @SerializedName("net_dag") public Double netDag;
    }

    /** Line-in-the-Sand inflection strike. */
    public static final class Lis {
        @SerializedName("strike") public Double strike;
        @SerializedName("magnitude") public Double magnitude;
    }

    /** A local maximum of {@code |net_gex|}. */
    public static final class Peak {
        @SerializedName("strike") public Double strike;
        @SerializedName("net_gex") public Double netGex;
        @SerializedName("strength") public Double strength;
        /** {@code "call_wall"} or {@code "put_wall"}. */
        @SerializedName("side") public String side;
    }

    /** One per-strike row joining every greek triple plus DAG and OI. */
    public static final class StrikeRow {
        @SerializedName("strike") public Double strike;
        @SerializedName("call_gex") public Double callGex;
        @SerializedName("put_gex") public Double putGex;
        @SerializedName("net_gex") public Double netGex;
        @SerializedName("call_dex") public Double callDex;
        @SerializedName("put_dex") public Double putDex;
        @SerializedName("net_dex") public Double netDex;
        @SerializedName("call_vex") public Double callVex;
        @SerializedName("put_vex") public Double putVex;
        @SerializedName("net_vex") public Double netVex;
        @SerializedName("call_chex") public Double callChex;
        @SerializedName("put_chex") public Double putChex;
        @SerializedName("net_chex") public Double netChex;
        @SerializedName("dag") public Double dag;
        @SerializedName("call_oi") public Long callOi;
        @SerializedName("put_oi") public Long putOi;
    }
}
