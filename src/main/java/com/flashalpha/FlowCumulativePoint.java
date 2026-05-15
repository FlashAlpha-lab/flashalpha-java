package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * One point of a cumulative net-flow series (a {@code points[]} element). Shared by the option and stock cumulative endpoints.
 */
public final class FlowCumulativePoint {

    /** Bucket start (ISO-8601 UTC, minute-aligned). */
    @SerializedName("ts")
    public String ts;

    /** Net volume in this minute bucket. */
    @SerializedName("netVolume")
    public Long netVolume;

    /** Running sum of {@code netVolume} from the start of the window (the "HIRO-style" line). */
    @SerializedName("cumulative")
    public Long cumulative;

    /** Volume-weighted average price in the bucket. */
    @SerializedName("vwap")
    public Double vwap;

    /** Number of trades in the bucket. */
    @SerializedName("tradeCount")
    public Integer tradeCount;
}
