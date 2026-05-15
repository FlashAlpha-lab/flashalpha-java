package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * One flagged underlying in an outliers table (shared by the option and stock outliers endpoints).
 */
public final class FlowOutlierRow {

    /** Flagged underlying. */
    @SerializedName("symbol")
    public String symbol;

    /** Number of trades over the window. */
    @SerializedName("tradeCount")
    public Integer tradeCount;

    /** Buy-classified volume. */
    @SerializedName("buyVolume")
    public Long buyVolume;

    /** Sell-classified volume. */
    @SerializedName("sellVolume")
    public Long sellVolume;

    /** Mid-classified volume. */
    @SerializedName("midVolume")
    public Long midVolume;

    /** {@code buyVolume - sellVolume}. */
    @SerializedName("netVolume")
    public Long netVolume;

    /** {@code |buy-sell| / (buy+sell)} x 100: 0 = balanced, 100 = one-sided. */
    @SerializedName("imbalancePct")
    public Double imbalancePct;

    /** Tiered skew label (FLAT/MILD_BUY/BUY/STRONG_BUY/...). */
    @SerializedName("skew")
    public String skew;

    /** Gross traded notional over the window (dollars). */
    @SerializedName("notional")
    public Double notional;

    /** Net (signed) traded notional over the window (dollars). */
    @SerializedName("netNotional")
    public Double netNotional;

    /** Largest single trade size. */
    @SerializedName("biggestTrade")
    public Integer biggestTrade;

    /** Timestamp of the biggest print; null if none in window. */
    @SerializedName("biggestTradeUtc")
    public String biggestTradeUtc;

    /** Age of the biggest print in seconds; -1 if none. */
    @SerializedName("biggestAgeSec")
    public Integer biggestAgeSec;

    /** VWAP of the most recent activity. */
    @SerializedName("lastVwap")
    public Double lastVwap;

    /** Timestamp of the last print; null if none. */
    @SerializedName("lastTradeUtc")
    public String lastTradeUtc;

    /** Age of the last print in seconds; -1 if none. */
    @SerializedName("lastTradeAgeSec")
    public Integer lastTradeAgeSec;
}
