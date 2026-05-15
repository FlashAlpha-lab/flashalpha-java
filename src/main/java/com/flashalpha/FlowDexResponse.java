package com.flashalpha;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Typed response for {@code GET /v1/flow/dex/{symbol}} (Alpha+). Live (flow-adjusted) DEX with the same per-strike shape as {@link DexResponse} (reuses {@link DexResponse.DexStrikeRow}).
 */
public final class FlowDexResponse {

    /** Underlying ticker echoed from the request path. */
    @SerializedName("symbol")
    public String symbol;

    /** Timestamp this snapshot was computed for (ISO-8601 UTC). */
    @SerializedName("as_of")
    public String asOf;

    /** Spot mid at the snapshot time. */
    @SerializedName("underlying_price")
    public Double underlyingPrice;

    /** Expiration filter echoed back, or null. */
    @SerializedName("expiry")
    public String expiry;

    /** Live net DEX across the chain (dollars). */
    @SerializedName("live_net_dex")
    public Double liveNetDex;

    /** Per-strike DEX breakdown. */
    @SerializedName("strikes")
    public List<DexResponse.DexStrikeRow> strikes;
}
