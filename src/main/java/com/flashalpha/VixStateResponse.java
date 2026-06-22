package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response for {@code GET /v1/macro/vix-state} (MCP {@code get_vix_state}) —
 * "overvixing / undervixing" regime label for the index complex, comparing spot VIX
 * against SPX 20-day realized vol. Requires Growth+.
 *
 * <p>Obtain via {@link FlashAlphaClient#vixStateTyped()}.
 */
public final class VixStateResponse {

    @SerializedName("as_of")
    public String asOf;

    @SerializedName("vix")
    public Double vix;

    /** SPX 20-day annualised realized vol (%). */
    @SerializedName("spx_rv_20d")
    public Double spxRv20d;

    /** {@code vix - spx_rv_20d} (vol points). */
    @SerializedName("spread")
    public Double spread;

    /** {@code vix / spx_rv_20d}. {@code null} when {@code spx_rv_20d == 0}. */
    @SerializedName("ratio")
    public Double ratio;

    /** {@code overvixing} (spread ≥ 5) / {@code undervixing} (spread ≤ 0) / {@code neutral}. */
    @SerializedName("state")
    public String state;

    @SerializedName("interpretation")
    public String interpretation;
}
