package com.flashalpha;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Typed response for {@code GET /v1/flow/options/{symbol}/blocks} (Alpha+). All trades with {@code size &gt;= minSize}, newest-first.
 */
public final class FlowOptionBlocksResponse {

    /** Underlying ticker echoed from the request path. */
    @SerializedName("symbol")
    public String symbol;

    /** Expiration filter echoed back when supplied, else null. */
    @SerializedName("expiry")
    public String expiry;

    /** Minimum trade size that qualified as a block (echoed back). */
    @SerializedName("minSize")
    public Integer minSize;

    /** Number of blocks returned. */
    @SerializedName("count")
    public Integer count;

    /** Newest-first list of large prints. */
    @SerializedName("blocks")
    public List<Block> blocks;


    /** A single large option print. */
    public static final class Block {
        /** Trade timestamp (ISO-8601 UTC). */
        @SerializedName("ts") public String ts;
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
        /** Side classification ("buy"/"sell"/"mid"). */
        @SerializedName("side") public String side;
    }
}
