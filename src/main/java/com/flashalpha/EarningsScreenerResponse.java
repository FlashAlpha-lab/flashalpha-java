package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response for {@code GET /v1/earnings/screener} (MCP {@code get_earnings_screener}) —
 * cross-sectional screener over upcoming earnings in a forward window, ranked by VRP
 * richness, cheapest implied move, highest historical crush, or importance. Requires Alpha+.
 *
 * <p>Obtain via {@link FlashAlphaClient#earningsScreenerTyped()}.
 */
public final class EarningsScreenerResponse {

    @SerializedName("events")
    public List<EarningsScreenerEvent> events;

    /** Total matched events before {@code limit} is applied. */
    @SerializedName("count")
    public Integer count;

    /** One event inside {@link #events}. */
    public static final class EarningsScreenerEvent {
        @SerializedName("symbol") public String symbol;
        @SerializedName("company_name") public String companyName;
        @SerializedName("earnings_date") public String earningsDate;
        @SerializedName("days_to_event") public Integer daysToEvent;
        @SerializedName("timing") public String timing;
        @SerializedName("importance") public Integer importance;
        @SerializedName("implied_move_pct") public Double impliedMovePct;
        @SerializedName("premium_ratio") public Double premiumRatio;
        @SerializedName("iv_crush_median") public Double ivCrushMedian;
        @SerializedName("assessment") public String assessment;
    }
}
