package com.flashalpha;

import java.util.List;

/**
 * The result of an unfiltered option-quote call: the chain plus its envelope.
 *
 * <p>{@code /optionquote} returns a bare JSON array when no expiry/strike/type filter is
 * given, so the quotes cannot be carried on a response model and the envelope cannot be
 * carried in the body. This pairs the two.
 */
public final class OptionQuotes {

    /** Every quote returned. Empty rather than null when the chain is empty. */
    public final List<OptionQuoteResponse> quotes;

    /** Per-feed provenance, read from the response headers. */
    public final ResponseMeta meta;

    OptionQuotes(List<OptionQuoteResponse> quotes, ResponseMeta meta) {
        this.quotes = quotes;
        this.meta = meta;
    }
}
