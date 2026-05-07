package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response model for {@code GET /v1/options/{ticker}}.
 *
 * <p>Option chain metadata — the available expirations and the strike
 * grid at each expiration. Counts ({@link #expirationCount},
 * {@link #totalContracts}) summarise the chain breadth.
 */
public final class OptionsMetaResponse {

    /** Echoed from the request path (e.g. {@code "SPY"}). */
    @SerializedName("symbol")
    public String symbol;

    /** Expirations and their strike grids. */
    @SerializedName("expirations")
    public List<ExpirationRow> expirations;

    /** {@code expirations.size()}. */
    @SerializedName("expiration_count")
    public Integer expirationCount;

    /** Total option contracts across all expirations and strikes (calls + puts). */
    @SerializedName("total_contracts")
    public Integer totalContracts;

    /** One expiration with its strike grid. */
    public static final class ExpirationRow {
        /** Expiration date string (ISO {@code yyyy-MM-dd}). */
        @SerializedName("expiration") public String expiration;
        /** Strike prices listed at this expiration, ascending. */
        @SerializedName("strikes") public List<Double> strikes;
    }
}
