package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response for {@code GET /v1/earnings/history/{symbol}} — past earnings events
 * with EPS/revenue actuals and surprises, implied vs. actual moves, and realized IV
 * crush. Requires Growth+.
 *
 * <p>Obtain via {@link FlashAlphaClient#earningsHistoryTyped(String)}.
 */
public final class EarningsHistoryResponse {

    @SerializedName("symbol")
    public String symbol;

    @SerializedName("count")
    public Integer count;

    @SerializedName("history")
    public List<EarningsHistoryEvent> history;

    /** One reported event inside {@link #history}. */
    public static final class EarningsHistoryEvent {
        @SerializedName("date") public String date;
        @SerializedName("fiscal_period") public String fiscalPeriod;
        @SerializedName("fiscal_year") public Integer fiscalYear;
        @SerializedName("eps_estimate") public Double epsEstimate;
        @SerializedName("eps_actual") public Double epsActual;
        @SerializedName("eps_surprise_pct") public Double epsSurprisePct;
        @SerializedName("revenue_actual") public Double revenueActual;
        @SerializedName("revenue_surprise_pct") public Double revenueSurprisePct;
        @SerializedName("implied_move_pct") public Double impliedMovePct;
        @SerializedName("actual_move_pct") public Double actualMovePct;
        @SerializedName("iv_crush_pct") public Double ivCrushPct;
        @SerializedName("pre_atm_iv") public Double preAtmIv;
        @SerializedName("post_atm_iv") public Double postAtmIv;
    }
}
