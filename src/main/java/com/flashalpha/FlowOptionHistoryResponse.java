package com.flashalpha;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Typed response for {@code GET /v1/flow/options/{symbol}/history} (Alpha+). Newest-first per-minute buckets.
 */
public final class FlowOptionHistoryResponse {

    /** Underlying ticker echoed from the request path. */
    @SerializedName("symbol")
    public String symbol;

    /** Expiration filter echoed back when supplied, else null. */
    @SerializedName("expiry")
    public String expiry;

    /** Lookback window in minutes (echoed back). */
    @SerializedName("minutes")
    public Integer minutes;

    /** Number of buckets returned. */
    @SerializedName("count")
    public Integer count;

    /** Newest-first list of per-minute aggregates. */
    @SerializedName("buckets")
    public List<Bucket> buckets;


    /** One per-minute option-flow bucket. */
    public static final class Bucket {
        /** Bucket start (ISO-8601 UTC, minute-aligned). */
        @SerializedName("ts") public String ts;
        /** Buy-classified volume in the bucket. */
        @SerializedName("buyVolume") public Long buyVolume;
        /** Sell-classified volume in the bucket. */
        @SerializedName("sellVolume") public Long sellVolume;
        /** Mid-classified volume in the bucket. */
        @SerializedName("midVolume") public Long midVolume;
        /** {@code buyVolume - sellVolume}. */
        @SerializedName("netVolume") public Long netVolume;
        /** Number of trades in the bucket. */
        @SerializedName("tradeCount") public Integer tradeCount;
        /** Largest single trade size in the bucket. */
        @SerializedName("biggestTrade") public Integer biggestTrade;
        /** Volume-weighted average trade price across the bucket. */
        @SerializedName("vwap") public Double vwap;
        /** Highest trade price in the bucket. */
        @SerializedName("high") public Double high;
        /** Lowest trade price in the bucket. */
        @SerializedName("low") public Double low;
    }
}
