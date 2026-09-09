package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Result of {@link Tldrapi#summarize(String, SummarizeOptions)}.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code summary} — the summarized text</li>
 *   <li>{@code sessionId} — server-issued session id; pass to a future
 *       call via {@link SummarizeOptions.Builder#sessionId} to keep the
 *       session model + state stable across a document</li>
 *   <li>{@code usage} — token counts + model used for this call</li>
 *   <li>{@code requestId} — X-Request-ID; include when reporting an issue</li>
 *   <li>{@code credits} — {balance, charged, tier} snapshot from response headers</li>
 *   <li>{@code raw} — full parsed JSON body, for fields we haven't hoisted</li>
 * </ul>
 */
public final class SummarizeResult {
    private final String summary;
    private final String sessionId;
    private final Usage usage;
    private final String requestId;
    private final Credits credits;
    private final JsonNode raw;

    SummarizeResult(String summary, String sessionId, Usage usage,
                    String requestId, Credits credits, JsonNode raw) {
        this.summary = summary;
        this.sessionId = sessionId;
        this.usage = usage;
        this.requestId = requestId;
        this.credits = credits;
        this.raw = raw;
    }

    public String  getSummary()   { return summary; }
    public String  getSessionId() { return sessionId; }
    public Usage   getUsage()     { return usage; }
    public String  getRequestId() { return requestId; }
    public Credits getCredits()   { return credits; }
    public JsonNode getRaw()      { return raw; }

    static SummarizeResult fromResponse(HttpTransport.Response r) throws TldrapiException {
        JsonNode body = r.jsonOrEmpty();
        JsonNode u = body.get("usage");
        Usage usage = Usage.fromNode(u);
        return new SummarizeResult(
            body.path("summary").asText(""),
            body.path("session_id").asText(""),
            usage,
            r.headerFirst("x-request-id"),
            Credits.fromHeaders(r),
            body
        );
    }
}
