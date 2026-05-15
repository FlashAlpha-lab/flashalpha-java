package com.flashalpha;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Typed response for {@code GET /v1/flow/stocks/{symbol}/recent} (Alpha+). Newest-first stock trade tape.
 */
public final class FlowStockRecentResponse {

    /** Underlying ticker echoed from the request path. */
    @SerializedName("symbol")
    public String symbol;

    /** Number of trades returned (capped by the limit). */
    @SerializedName("count")
    public Integer count;

    /** Unclamped total trade count. */
    @SerializedName("totalAvailable")
    public Integer totalAvailable;

    /** Newest-first list of trade prints. */
    @SerializedName("trades")
    public List<Trade> trades;


    /** A single stock trade print. */
    public static final class Trade {
        /** Trade timestamp (ISO-8601 UTC). */
        @SerializedName("ts") public String ts;
        /** Trade price. */
        @SerializedName("price") public Double price;
        /** Trade size in shares. */
        @SerializedName("size") public Integer size;
        /** Side classification ("buy"/"sell"/"mid"). */
        @SerializedName("side") public String side;
        /** True when the print is at/above the block-size threshold. */
        @SerializedName("isBlock") public Boolean isBlock;
        /** NBBO bid at the moment of the trade. */
        @SerializedName("bid") public Double bid;
        /** NBBO ask at the moment of the trade. */
        @SerializedName("ask") public Double ask;
    }
}
