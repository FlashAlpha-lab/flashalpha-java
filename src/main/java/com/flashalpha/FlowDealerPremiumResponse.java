package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response for {@code GET /v1/flow/options/{symbol}/dealer-premium}
 * (MCP {@code get_dealer_premium}) — full-tape Net Dealer Premium roll-up over a
 * configurable window. Sums each side of the customer-flow tape weighted by VWAP per
 * minute bucket. Requires Alpha+.
 *
 * <p>Obtain via {@link FlashAlphaClient#flowDealerPremiumTyped(String)}.
 */
public final class FlowDealerPremiumResponse {

    @SerializedName("symbol")
    public String symbol;

    @SerializedName("as_of")
    public String asOf;

    @SerializedName("window_minutes")
    public Integer windowMinutes;

    @SerializedName("expiry")
    public String expiry;

    /** Dealer is the BUYER when customer hits the bid. */
    @SerializedName("dealer_buy_premium")
    public Double dealerBuyPremium;

    /** Dealer is the WRITER when customer lifts the ask. */
    @SerializedName("dealer_write_premium")
    public Double dealerWritePremium;

    /** {@code dealer_buy_premium - dealer_write_premium}. Positive ⇒ dealers net long premium. */
    @SerializedName("net_dealer_premium")
    public Double netDealerPremium;

    @SerializedName("total_premium")
    public Double totalPremium;

    @SerializedName("trade_count")
    public Long tradeCount;

    @SerializedName("bucket_count")
    public Integer bucketCount;
}
