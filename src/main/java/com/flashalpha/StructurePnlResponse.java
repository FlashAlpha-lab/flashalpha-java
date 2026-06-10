package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response for {@code POST /v1/structures/pnl} (Basic+) — at-expiry
 * profit-and-loss curve, breakevens, and bounded max profit/loss for a
 * multi-leg option structure. The payoff is piecewise-linear in the underlying.
 *
 * <p>Obtain via {@link FlashAlphaClient#structurePnlTyped(StructureRequest)}.
 */
public final class StructurePnlResponse {

    /** Echo of the request legs (action / type / strike / premium / quantity). */
    @SerializedName("legs")
    public List<StructureLeg> legs;

    /** P&amp;L curve sampled across the underlying-price range. */
    @SerializedName("pnl_curve")
    public List<PnlPoint> pnlCurve;

    /** Underlying prices where P&amp;L crosses zero (may be empty). */
    @SerializedName("breakevens")
    public List<Double> breakevens;

    /** Max profit at expiry, or {@code null} when unbounded on that side. */
    @SerializedName("max_profit")
    public Double maxProfit;

    /** Max loss at expiry, or {@code null} when unbounded on that side. */
    @SerializedName("max_loss")
    public Double maxLoss;

    /** One {@code {underlying, pnl}} sample of the payoff curve. */
    public static final class PnlPoint {
        /** Underlying price at this sample. */
        @SerializedName("underlying") public Double underlying;
        /** Net P&amp;L of the structure at this underlying price. */
        @SerializedName("pnl") public Double pnl;
    }
}
