package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response for {@code GET /v1/exposure/oi-diff/{symbol}} (Growth+) —
 * day-over-day open-interest deltas (today's OI minus prior trading day),
 * top-N changes sorted by absolute magnitude, and call/put aggregate totals.
 *
 * <p>Obtain via {@link FlashAlphaClient#oiDiffTyped(String, Integer)}.
 */
public final class OiDiffResponse {

    @SerializedName("symbol")
    public String symbol;

    @SerializedName("underlying_price")
    public Double underlyingPrice;

    @SerializedName("as_of")
    public String asOf;

    /** {@code false} when no prior-day OI data exists yet — totals are then 0 and the list empty. */
    @SerializedName("prior_snapshot_available")
    public Boolean priorSnapshotAvailable;

    /** Sum of per-contract deltas across all call contracts with a prior-day match. */
    @SerializedName("total_call_oi_change")
    public Double totalCallOiChange;

    /** Same for puts. */
    @SerializedName("total_put_oi_change")
    public Double totalPutOiChange;

    /** Top-N contract OI changes sorted by {@code |oi_change|} descending. */
    @SerializedName("top_oi_changes")
    public List<OiChange> topOiChanges;

    /** One contract's day-over-day OI delta. */
    public static final class OiChange {
        @SerializedName("strike") public Double strike;
        /** {@code "C"} or {@code "P"}. */
        @SerializedName("type") public String type;
        @SerializedName("expiry") public String expiry;
        @SerializedName("today_oi") public Long todayOi;
        @SerializedName("prior_oi") public Long priorOi;
        @SerializedName("oi_change") public Long oiChange;
    }
}
