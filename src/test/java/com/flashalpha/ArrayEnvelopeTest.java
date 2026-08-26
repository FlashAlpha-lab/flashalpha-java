package com.flashalpha;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A bare JSON array is the one body shape that cannot carry an envelope, so the API moves
 * it to headers. These tests pin both halves: the rows survive decoding, and the envelope
 * is still reachable.
 */
public class ArrayEnvelopeTest {

    private static final String AS_OF = "{\"node\":\"fa2\",\"equity_feed\":\"2026-08-25T18:48:58.204Z\","
            + "\"equity_options_feed\":\"2026-08-25T18:48:57.900Z\",\"index_feed\":null,"
            + "\"index_options_feed\":null,\"futures_feed\":null,\"futures_options_feed\":null,"
            + "\"flow_feed\":null,\"oi_feed\":\"2026-08-24T20:00:00.000Z\",\"macro_feed\":null}";

    private MockWebServer server;
    private FlashAlphaClient client;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = new FlashAlphaClient("k", server.url("/").toString().replaceAll("/$", ""));
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    private void enqueueArray() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setHeader("X-Endpoint-Version", "2026.08.25")
                .setHeader("X-Data-As-Of", AS_OF)
                .setBody("[{\"strike\":500,\"type\":\"call\"},{\"strike\":505,\"type\":\"put\"}]"));
    }

    /** Regression: this shape used to throw IllegalStateException out of getAsJsonObject(). */
    @Test
    public void arrayBodyKeepsItsRows() {
        enqueueArray();
        OptionQuotes result = client.optionQuoteWithMetadata("SPY");
        assertEquals(2, result.quotes.size());
    }

    @Test
    public void arrayBodyExposesTheEnvelopeFromHeaders() {
        enqueueArray();
        OptionQuotes result = client.optionQuoteWithMetadata("SPY");

        assertEquals("2026.08.25", result.meta.endpointVersion);
        assertNotNull("data_as_of header was not parsed", result.meta.dataAsOf);
        assertEquals("fa2", result.meta.dataAsOf.node);
        assertEquals("2026-08-24T20:00:00.000Z", result.meta.dataAsOf.oiFeed);
        assertNull("unseen feed should stay null", result.meta.dataAsOf.indexFeed);
    }

    /**
     * Provenance is diagnostic: a malformed header must not turn a good response into an
     * error, or a header change upstream would break every array call.
     */
    @Test
    public void malformedEnvelopeHeaderDoesNotFailTheCall() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("X-Data-As-Of", "not json")
                .setBody("[{\"strike\":500}]"));

        OptionQuotes result = client.optionQuoteWithMetadata("SPY");
        assertEquals(1, result.quotes.size());
        assertNull(result.meta.dataAsOf);
    }

    /** The filtered call returns a single object; both shapes normalise to a list. */
    @Test
    public void singleObjectShapeStillDecodes() {
        server.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"strike\":500,\"type\":\"call\"}"));

        OptionQuotes result = client.optionQuoteWithMetadata("SPY", "2026-08-28", 500.0, "call");
        assertEquals(1, result.quotes.size());
    }

    /** The untyped accessor cannot represent an array, so it must say so usefully. */
    @Test
    public void untypedAccessorFailsWithAnActionableMessage() {
        enqueueArray();
        try {
            client.optionQuote("SPY");
            fail("expected a clear failure");
        } catch (FlashAlphaException e) {
            assertTrue(e.getMessage().contains("WithMetadata"));
        }
    }
}
