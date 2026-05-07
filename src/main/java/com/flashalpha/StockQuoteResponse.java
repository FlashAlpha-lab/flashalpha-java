package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response model for {@code GET /stockquote/{ticker}} (Free+, live only).
 *
 * <p>Live stock NBBO snapshot — bid / ask / mid plus the most recent
 * trade price and quote-update timestamp.
 *
 * <p><b>Camel-case fields:</b> {@code lastPrice} and {@code lastUpdate}
 * are camelCase on the wire.
 */
public final class StockQuoteResponse {

    @SerializedName("ticker")
    public String ticker;

    @SerializedName("bid")
    public Double bid;

    @SerializedName("ask")
    public Double ask;

    /** {@code (bid + ask) / 2}. */
    @SerializedName("mid")
    public Double mid;

    /** Most recent trade price. camelCase on the wire. */
    @SerializedName("lastPrice")
    public Double lastPrice;

    /** Last quote update, ET wall-clock. camelCase on the wire. */
    @SerializedName("lastUpdate")
    public String lastUpdate;
}
