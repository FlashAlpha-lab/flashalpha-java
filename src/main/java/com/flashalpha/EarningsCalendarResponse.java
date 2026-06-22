package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response for {@code GET /v1/earnings/calendar} (MCP {@code get_earnings_calendar}) —
 * upcoming earnings calendar over a forward window, optionally filtered to specific
 * symbols and a minimum importance rating. Requires Growth+.
 *
 * <p>Obtain via {@link FlashAlphaClient#earningsCalendarTyped()}.
 */
public final class EarningsCalendarResponse {

    @SerializedName("events")
    public List<EarningsCalendarEvent> events;

    @SerializedName("count")
    public Integer count;

    /** One event inside {@link #events}. */
    public static final class EarningsCalendarEvent {
        @SerializedName("symbol") public String symbol;
        @SerializedName("company_name") public String companyName;
        @SerializedName("earnings_date") public String earningsDate;
        /** {@code bmo} (before market open) / {@code amc} (after market close); may be null. */
        @SerializedName("timing") public String timing;
        @SerializedName("is_confirmed") public Boolean isConfirmed;
        @SerializedName("fiscal_period") public String fiscalPeriod;
        @SerializedName("fiscal_year") public Integer fiscalYear;
        @SerializedName("importance") public Integer importance;
        @SerializedName("eps_estimate") public Double epsEstimate;
        @SerializedName("implied_move_pct") public Double impliedMovePct;
        @SerializedName("days_to_event") public Integer daysToEvent;
    }
}
