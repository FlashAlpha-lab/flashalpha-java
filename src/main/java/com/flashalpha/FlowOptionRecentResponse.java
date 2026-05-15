package com.flashalpha;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Typed response for {@code GET /v1/flow/options/{symbol}/recent} (Alpha+). Newest-first option trade tape. {@code expiry} is echoed only when the filter is supplied.
 */
public final class FlowOptionRecentResponse {

    /** Underlying ticker echoed from the request path. */
    @SerializedName("symbol")
    public String symbol;

    /** Expiration filter echoed back when supplied, else null. */
    @SerializedName("expiry")
    public String expiry;

    /** Number of trades returned (capped by the limit). */
    @SerializedName("count")
    public Integer count;

    /** Unclamped total trade count. */
    @SerializedName("totalAvailable")
    public Integer totalAvailable;

    /** Newest-first list of trade prints. */
    @SerializedName("trades")
    public List<Trade> trades;


    /** A single option trade print. */
    public static final class Trade {
        /** Trade timestamp (ISO-8601 UTC). */
        @SerializedName("ts") public String ts;
        /** OPRA instrument id of the contract. */
        @SerializedName("instrumentId") public Long instrumentId;
        /** Contract expiration (YYYY-MM-DD). */
        @SerializedName("expiry") public String expiry;
        /** Contract strike price. */
        @SerializedName("strike") public Double strike;
        /** "C" (call) or "P" (put). */
        @SerializedName("right") public String right;
        /** Trade price. */
        @SerializedName("price") public Double price;
        /** Trade size in contracts. */
        @SerializedName("size") public Integer size;
        /** Side classification vs the NBBO at print ("buy"/"sell"/"mid"). */
        @SerializedName("side") public String side;
        /** True when the print is at/above the block-size threshold. */
        @SerializedName("isBlock") public Boolean isBlock;
        /** NBBO bid at the moment of the trade. */
        @SerializedName("bid") public Double bid;
        /** NBBO ask at the moment of the trade. */
        @SerializedName("ask") public Double ask;
    }
}
