package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response for {@code GET /v1/dispersion} (MCP {@code get_dispersion}) —
 * Demeterfi-Derman-Kani implied correlation between an index and a basket of
 * constituents, paired with a 1-factor realized correlation. Returns
 * {@code correlation_premium = implied - realized} and per-constituent contribution
 * to basket vol. Requires Alpha+.
 *
 * <p>Obtain via {@link FlashAlphaClient#dispersionTyped(String, String)}.
 */
public final class DispersionResponse {

    @SerializedName("as_of")
    public String asOf;

    @SerializedName("index")
    public String index;

    @SerializedName("constituent_count")
    public Integer constituentCount;

    @SerializedName("missing_symbols")
    public List<String> missingSymbols;

    @SerializedName("horizon_days")
    public Integer horizonDays;

    /** Implied correlation. {@code null} for a degenerate basket. */
    @SerializedName("implied_correlation")
    public Double impliedCorrelation;

    /** 1-factor realized correlation. {@code null} when not computable. */
    @SerializedName("realized_correlation")
    public Double realizedCorrelation;

    /** {@code implied - realized}. Positive ⇒ market pricing more correlation than realized. */
    @SerializedName("correlation_premium")
    public Double correlationPremium;

    /** Index ATM IV (decimal). */
    @SerializedName("implied_vol_index")
    public Double impliedVolIndex;

    /** {@code Σ wᵢ σᵢ} after renormalisation. */
    @SerializedName("implied_vol_basket")
    public Double impliedVolBasket;

    /** Sorted descending by {@code contribution_to_basket_vol}. */
    @SerializedName("top_contributors")
    public List<DispersionContributor> topContributors;

    /** One contributor inside {@link #topContributors}. */
    public static final class DispersionContributor {
        @SerializedName("symbol") public String symbol;
        @SerializedName("weight") public Double weight;
        @SerializedName("iv") public Double iv;
        /** {@code wᵢ × σᵢ}. */
        @SerializedName("contribution_to_basket_vol") public Double contributionToBasketVol;
    }
}
