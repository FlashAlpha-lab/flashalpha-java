package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * A single option leg of a multi-leg structure request, shared by both
 * {@code POST /v1/structures/pnl} and {@code POST /v1/structures/greeks}.
 *
 * <p>For P&amp;L ({@link FlashAlphaClient#structurePnl(StructureRequest)}) supply
 * {@link #action}, {@link #type}, {@link #strike}, {@link #premium}, and
 * {@link #quantity}. For Greeks
 * ({@link FlashAlphaClient#structureGreeks(StructureGreeksRequest)}) supply
 * {@link #action}, {@link #type}, {@link #strike}, {@link #expiry},
 * {@link #impliedVol}, and {@link #quantity}.
 *
 * <p>Field names are camelCase to match the API request body verbatim
 * ({@code impliedVol}). {@code quantity} defaults to {@code 1} server-side when
 * left {@code null}.
 */
public final class StructureLeg {

    /** {@code "buy"} (alias {@code "long"}) or {@code "sell"} (alias {@code "short"}). */
    @SerializedName("action")
    public String action;

    /** {@code "call"} (alias {@code "c"}) or {@code "put"} (alias {@code "p"}). */
    @SerializedName("type")
    public String type;

    /** Strike price (must be {@code > 0}). */
    @SerializedName("strike")
    public Double strike;

    /** Per-contract premium paid/received (P&amp;L only; must be {@code >= 0}). */
    @SerializedName("premium")
    public Double premium;

    /** Leg expiry {@code yyyy-MM-dd} (Greeks only). */
    @SerializedName("expiry")
    public String expiry;

    /** Implied volatility as a decimal, e.g. {@code 0.28} (Greeks only; must be {@code > 0}). */
    @SerializedName("impliedVol")
    public Double impliedVol;

    /** Number of contracts (must be {@code > 0}; defaults to {@code 1}). */
    @SerializedName("quantity")
    public Integer quantity;

    public StructureLeg() {
    }

    /** Convenience constructor for a P&amp;L leg. */
    public static StructureLeg pnlLeg(String action, String type, double strike, double premium, Integer quantity) {
        StructureLeg leg = new StructureLeg();
        leg.action = action;
        leg.type = type;
        leg.strike = strike;
        leg.premium = premium;
        leg.quantity = quantity;
        return leg;
    }

    /** Convenience constructor for a Greeks leg. */
    public static StructureLeg greeksLeg(String action, String type, double strike, String expiry,
                                         double impliedVol, Integer quantity) {
        StructureLeg leg = new StructureLeg();
        leg.action = action;
        leg.type = type;
        leg.strike = strike;
        leg.expiry = expiry;
        leg.impliedVol = impliedVol;
        leg.quantity = quantity;
        return leg;
    }
}
