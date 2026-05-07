package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response model for {@code GET /v1/symbols}.
 *
 * <p>Symbols currently being polled for live analytics, with a free-form
 * {@link #note} on coverage and the {@link #lastUpdated} timestamp of the
 * latest refresh.
 */
public final class SymbolsResponse {

    /** Symbols currently being polled for live analytics. */
    @SerializedName("symbols")
    public List<String> symbols;

    /** {@code symbols.size()}. */
    @SerializedName("count")
    public Integer count;

    /** Free-form coverage note (e.g. universe scope or refresh cadence). */
    @SerializedName("note")
    public String note;

    /** Timestamp of the latest refresh (ET wall-clock string). */
    @SerializedName("last_updated")
    public String lastUpdated;
}
