package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response for {@code GET /v1/volatility/spot-vol-correlation/{symbol}}
 * (MCP {@code get_spot_vol_correlation}) — daily Pearson correlation between spot
 * log-returns and first-differences of ATM implied vol over 20-day and 60-day
 * windows. Requires Growth+.
 *
 * <p>Obtain via {@link FlashAlphaClient#spotVolCorrelationTyped(String)}.
 */
public final class SpotVolCorrelationResponse {

    @SerializedName("symbol")
    public String symbol;

    @SerializedName("as_of")
    public String asOf;

    /** Pearson over the last 20 snapshots. {@code null} when undefined. */
    @SerializedName("spot_vol_correlation_20d")
    public Double spotVolCorrelation20d;

    /** Pearson over the last 60 snapshots. {@code null} when history is too short. */
    @SerializedName("spot_vol_correlation_60d")
    public Double spotVolCorrelation60d;

    @SerializedName("data_points_20d")
    public Integer dataPoints20d;

    @SerializedName("data_points_60d")
    public Integer dataPoints60d;

    @SerializedName("interpretation")
    public String interpretation;
}
