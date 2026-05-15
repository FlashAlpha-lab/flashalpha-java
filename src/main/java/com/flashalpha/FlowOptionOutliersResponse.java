package com.flashalpha;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Typed response for {@code GET /v1/flow/options/outliers} (Alpha+, cached ~30s).
 */
public final class FlowOptionOutliersResponse {

    /** When the cached snapshot was generated (ISO-8601 UTC). */
    @SerializedName("generatedUtc")
    public String generatedUtc;

    /** Aggregation window in minutes. */
    @SerializedName("windowMinutes")
    public Integer windowMinutes;

    /** Number of symbols evaluated. */
    @SerializedName("tracked")
    public Integer tracked;

    /** Symbols that met minTrades and had non-zero volume. */
    @SerializedName("qualified")
    public Integer qualified;

    /** Max rows requested. */
    @SerializedName("limit")
    public Integer limit;

    /** Imbalance-ranked flagged underlyings. */
    @SerializedName("outliers")
    public List<FlowOutlierRow> outliers;
}
