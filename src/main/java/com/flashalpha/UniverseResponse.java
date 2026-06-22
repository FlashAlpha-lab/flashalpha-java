package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response for {@code GET /v1/universe} (MCP {@code get_universe}) — curated
 * tier-1 / tier-2 symbol directory, the symbols the screener background loop keeps
 * pre-warmed. Public — no auth required.
 *
 * <p>Obtain via {@link FlashAlphaClient#universeTyped()}.
 */
public final class UniverseResponse {

    @SerializedName("as_of")
    public String asOf;

    /** Total universe size (tier-1 ∪ tier-2, deduplicated). */
    @SerializedName("count")
    public Integer count;

    /** {@code min(count, limit)}. */
    @SerializedName("returned")
    public Integer returned;

    @SerializedName("limit")
    public Integer limit;

    /** Echoes the effective sort. */
    @SerializedName("sort")
    public String sort;

    @SerializedName("symbols")
    public List<UniverseSymbol> symbols;

    /** One symbol inside {@link #symbols}. */
    public static final class UniverseSymbol {
        @SerializedName("symbol") public String symbol;
        /** 1 = high-traffic loop (fast refresh); 2 = remaining curated symbols. */
        @SerializedName("tier") public Integer tier;
        @SerializedName("is_pre_warmed") public Boolean isPreWarmed;
        /**
         * {@code true} when the underlying lists same-day-expiry (0DTE) options every
         * trading day — currently SPX/SPXW, SPY, QQQ, IWM. Filter {@code has_0dte == true}
         * to get the full daily-expiry basket up front.
         */
        @SerializedName("has_0dte") public Boolean has0dte;
    }
}
