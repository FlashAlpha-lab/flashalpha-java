package com.flashalpha;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Typed response for {@code GET /v1/flow/stocks/{symbol}/blocks} (Alpha+). All trades with {@code size &gt;= minSize}, newest-first.
 */
public final class FlowStockBlocksResponse {

    /** Underlying ticker echoed from the request path. */
    @SerializedName("symbol")
    public String symbol;

    /** Minimum trade size that qualified as a block (echoed back). */
    @SerializedName("minSize")
    public Integer minSize;

    /** Number of blocks returned. */
    @SerializedName("count")
    public Integer count;

    /** Newest-first list of large prints. */
    @SerializedName("blocks")
    public List<Block> blocks;


    /** A single large stock print. */
    public static final class Block {
        /** Trade timestamp (ISO-8601 UTC). */
        @SerializedName("ts") public String ts;
        /** Trade price. */
        @SerializedName("price") public Double price;
        /** Trade size in shares. */
        @SerializedName("size") public Integer size;
        /** Side classification ("buy"/"sell"/"mid"). */
        @SerializedName("side") public String side;
        /** NBBO bid at the moment of the trade. */
        @SerializedName("bid") public Double bid;
        /** NBBO ask at the moment of the trade. */
        @SerializedName("ask") public Double ask;
    }
}
