package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response model for {@code GET /v1/tickers}.
 *
 * <p>Lists every ticker the live API will accept. {@link #count} mirrors
 * {@code tickers.size()}.
 */
public final class TickersResponse {

    /** All available stock ticker symbols. */
    @SerializedName("tickers")
    public List<String> tickers;

    /** Number of tickers — {@code tickers.size()}. */
    @SerializedName("count")
    public Integer count;
}
