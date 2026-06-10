package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Request body for {@code POST /v1/structures/pnl} — at-expiry profit-and-loss
 * curve, breakevens, and max profit/loss for an arbitrary multi-leg option
 * structure. Pure math: no market-data lookup, no symbol resolution.
 *
 * <p>Use with {@link FlashAlphaClient#structurePnl(StructureRequest)} →
 * {@link StructurePnlResponse}.
 */
public final class StructureRequest {

    /** One or more legs. Must be non-empty. Each leg carries action / type / strike / premium / quantity. */
    @SerializedName("legs")
    public List<StructureLeg> legs;

    /** Lower bound of the underlying-price curve (nullable — derived from leg strikes ±30% when omitted). */
    @SerializedName("minUnderlying")
    public Double minUnderlying;

    /** Upper bound of the curve (nullable — see {@link #minUnderlying}). */
    @SerializedName("maxUnderlying")
    public Double maxUnderlying;

    /** Number of equally-spaced curve sample points (nullable; default {@code 81}, min 2). */
    @SerializedName("points")
    public Integer points;

    public StructureRequest() {
    }

    public StructureRequest(List<StructureLeg> legs) {
        this.legs = legs;
    }

    public StructureRequest(List<StructureLeg> legs, Double minUnderlying, Double maxUnderlying, Integer points) {
        this.legs = legs;
        this.minUnderlying = minUnderlying;
        this.maxUnderlying = maxUnderlying;
        this.points = points;
    }
}
