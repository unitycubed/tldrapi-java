package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests that don't hit the network. Cover builder validation, option
 * validation, error-mapping, and JSON parsing. HTTP integration tests
 * would need a WireMock or a live gateway and are out of scope for the
 * default `mvn test` run — those live under a separate integration
 * profile that isn't wired up yet.
 */
class TldrapiTest {

    @Test
    void builder_requires_rapidApiKey() {
        assertThrows(IllegalArgumentException.class,
            () -> Tldrapi.builder().build(),
            "empty rapidApiKey should throw");
        assertThrows(IllegalArgumentException.class,
            () -> Tldrapi.builder().rapidApiKey("").build(),
            "blank rapidApiKey should throw");
    }

    @Test
    void builder_defaults_populate() {
        Tldrapi client = Tldrapi.builder().rapidApiKey("test-key").build();
        assertNotNull(client);
        // Nothing to publicly assert on defaults — cover via a full request
        // in the integration tier when it lands.
    }

    @Test
    void summarizeOptions_defaults_are_empty() {
        SummarizeOptions o = SummarizeOptions.builder().build();
        assertNull(o.getTier());
        assertNull(o.getSessionId());
        assertNull(o.getModelAlias());
        assertFalse(o.isAllowOverage());
        assertTrue(o.getExtraHeaders().isEmpty());
        assertEquals(0, o.getPerCallTimeoutSeconds());
    }

    @Test
    void summarizeOptions_builder_sets_fields() {
        SummarizeOptions o = SummarizeOptions.builder()
            .tier("deep")
            .sessionId("s-123")
            .modelAlias("openai-gpt-4o")
            .allowOverage(true)
            .perCallTimeoutSeconds(120)
            .build();
        assertEquals("deep", o.getTier());
        assertEquals("s-123", o.getSessionId());
        assertEquals("openai-gpt-4o", o.getModelAlias());
        assertTrue(o.isAllowOverage());
        assertEquals(120, o.getPerCallTimeoutSeconds());
    }

    @Test
    void summarize_rejects_empty_input() {
        Tldrapi client = Tldrapi.builder().rapidApiKey("x").build();
        assertThrows(IllegalArgumentException.class,
            () -> client.summarize(""), "empty input should throw");
        assertThrows(IllegalArgumentException.class,
            () -> client.summarize(null), "null input should throw");
    }

    @Test
    void summarize_rejects_invalid_tier() {
        Tldrapi client = Tldrapi.builder().rapidApiKey("x").build();
        SummarizeOptions bad = SummarizeOptions.builder().tier("supreme").build();
        assertThrows(IllegalArgumentException.class,
            () -> client.summarize("some text", bad));
    }

    @Test
    void error_mapping_401_gives_authentication() {
        JsonNode body = JsonUtil.MAPPER.createObjectNode().put("error", "invalid key");
        TldrapiException e = TldrapiException.fromResponse(401, body, "req-1", 0);
        assertTrue(e instanceof TldrapiException.Authentication);
        assertEquals(401, e.getStatusCode());
        assertEquals("req-1", e.getRequestId());
    }

    @Test
    void error_mapping_402_gives_insufficientCredits() {
        JsonNode body = JsonUtil.MAPPER.createObjectNode().put("message", "top up");
        TldrapiException e = TldrapiException.fromResponse(402, body, "req-2", 0);
        assertTrue(e instanceof TldrapiException.InsufficientCredits);
    }

    @Test
    void error_mapping_429_gives_rateLimit_with_retryAfter() {
        JsonNode body = JsonUtil.MAPPER.createObjectNode().put("message", "slow down");
        TldrapiException e = TldrapiException.fromResponse(429, body, "req-3", 42);
        assertTrue(e instanceof TldrapiException.RateLimit);
        assertEquals(42, ((TldrapiException.RateLimit) e).getRetryAfterSeconds());
    }

    @Test
    void error_mapping_400_language_gives_languageNotSupported() {
        ObjectNode body = JsonUtil.MAPPER.createObjectNode();
        body.put("error_code", "language_not_supported");
        body.put("message", "only English supported");
        TldrapiException e = TldrapiException.fromResponse(400, body, "req-4", 0);
        assertTrue(e instanceof TldrapiException.LanguageNotSupported);
    }

    @Test
    void error_mapping_400_quality_gives_qualityException() {
        ObjectNode body = JsonUtil.MAPPER.createObjectNode();
        body.put("error_code", "quality_selection_requires_paid_plan");
        TldrapiException e = TldrapiException.fromResponse(400, body, "", 0);
        assertTrue(e instanceof TldrapiException.QualitySelectionRequiresPaidPlan);
    }

    @Test
    void error_mapping_400_other_gives_invalidRequest() {
        ObjectNode body = JsonUtil.MAPPER.createObjectNode();
        body.put("error", "missing input_text");
        TldrapiException e = TldrapiException.fromResponse(400, body, "", 0);
        assertTrue(e instanceof TldrapiException.InvalidRequest);
    }

    @Test
    void error_mapping_500_gives_server() {
        TldrapiException e = TldrapiException.fromResponse(503, JsonUtil.MAPPER.createObjectNode(), "", 0);
        assertTrue(e instanceof TldrapiException.Server);
    }

    @Test
    void jsonUtil_parseOrMissing_returns_missingNode_on_bad_input() {
        assertTrue(JsonUtil.parseOrMissing(null).isMissingNode());
        assertTrue(JsonUtil.parseOrMissing(new byte[0]).isMissingNode());
        assertTrue(JsonUtil.parseOrMissing("not json at all".getBytes()).isMissingNode());
    }

    @Test
    void jsonUtil_parses_valid_json() {
        byte[] json = "{\"summary\":\"hi\"}".getBytes();
        JsonNode n = JsonUtil.parseOrMissing(json);
        assertEquals("hi", n.path("summary").asText());
    }
}
