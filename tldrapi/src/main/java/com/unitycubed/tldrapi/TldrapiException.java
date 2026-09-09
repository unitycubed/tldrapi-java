package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Base checked exception for all TLDRapi SDK failures. Every failure
 * the client produces subclasses this, so a caller who wants a blanket
 * safety net can do:
 *
 * <pre>{@code
 * try {
 *     client.summarize(text);
 * } catch (TldrapiException e) {
 *     // handle any SDK-originated failure
 * }
 * }</pre>
 *
 * <p>Specific subclasses map to specific server response shapes so
 * that callers can branch on failure mode without inspecting error
 * strings. The {@link #getResponseBody()} accessor holds the parsed
 * JSON body when the server sent one — useful for surfacing an
 * upgrade URL on {@link RateLimit} / {@link InsufficientCredits}.
 */
public class TldrapiException extends Exception {
    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String requestId;
    private final JsonNode responseBody;

    public TldrapiException(String message, int statusCode, String requestId, JsonNode responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.requestId = requestId == null ? "" : requestId;
        this.responseBody = responseBody;
    }

    public TldrapiException(String message, int statusCode, String requestId, JsonNode responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.requestId = requestId == null ? "" : requestId;
        this.responseBody = responseBody;
    }

    /** HTTP status the server returned. 0 if the failure was transport-level. */
    public int getStatusCode() { return statusCode; }

    /** X-Request-ID from the response headers. Empty when none. */
    public String getRequestId() { return requestId; }

    /** Parsed JSON error body, or null when the server returned no body. */
    public JsonNode getResponseBody() { return responseBody; }

    // ---------------- subclasses (kept in this file to keep the module small) ----------------

    /** 401 / 403 — API key missing, invalid, or lacks permission. */
    public static class Authentication extends TldrapiException {
        private static final long serialVersionUID = 1L;
        public Authentication(String m, int s, String r, JsonNode b) { super(m, s, r, b); }
    }

    /** 402 — insufficient credits. Response body includes downgrade +
     *  top-up options the caller can present to the user. */
    public static class InsufficientCredits extends TldrapiException {
        private static final long serialVersionUID = 1L;
        public InsufficientCredits(String m, int s, String r, JsonNode b) { super(m, s, r, b); }
    }

    /** 429 — plan rate-limit exceeded. {@link #getRetryAfterSeconds()}
     *  is set from the {@code Retry-After} response header when present. */
    public static class RateLimit extends TldrapiException {
        private static final long serialVersionUID = 1L;
        private final int retryAfterSeconds;
        public RateLimit(String m, int s, String r, JsonNode b, int retryAfter) {
            super(m, s, r, b);
            this.retryAfterSeconds = retryAfter;
        }
        public int getRetryAfterSeconds() { return retryAfterSeconds; }
    }

    /** 400 with error_code = LANGUAGE_NOT_SUPPORTED — input wasn't English
     *  (only supported language at launch). {@code detected_language} +
     *  {@code detected_language_name} are on {@link #getResponseBody()}. */
    public static class LanguageNotSupported extends TldrapiException {
        private static final long serialVersionUID = 1L;
        public LanguageNotSupported(String m, int s, String r, JsonNode b) { super(m, s, r, b); }
    }

    /** 400 — free-tier caller passed {@code X-Quality} other than 'quick'. */
    public static class QualitySelectionRequiresPaidPlan extends TldrapiException {
        private static final long serialVersionUID = 1L;
        public QualitySelectionRequiresPaidPlan(String m, int s, String r, JsonNode b) { super(m, s, r, b); }
    }

    /** 400 for other reasons — malformed body, missing input_text, etc. */
    public static class InvalidRequest extends TldrapiException {
        private static final long serialVersionUID = 1L;
        public InvalidRequest(String m, int s, String r, JsonNode b) { super(m, s, r, b); }
    }

    /** 5xx — provider outage, our bug, or a downstream failure the
     *  router couldn't work around. Retries are exhausted at this point. */
    public static class Server extends TldrapiException {
        private static final long serialVersionUID = 1L;
        public Server(String m, int s, String r, JsonNode b) { super(m, s, r, b); }
    }

    /** Transport-layer failure — DNS, connection reset, TLS handshake. */
    public static class Network extends TldrapiException {
        private static final long serialVersionUID = 1L;
        public Network(String m, Throwable cause) { super(m, 0, "", null, cause); }
    }

    /** The HTTP request exceeded the client's per-request timeout. Retry
     *  with a bumped {@code perCallTimeoutSeconds} if the input is
     *  legitimately long. */
    public static class Timeout extends TldrapiException {
        private static final long serialVersionUID = 1L;
        public Timeout(String m, Throwable cause) { super(m, 0, "", null, cause); }
    }

    // ---------------- server-response → typed exception mapper ----------------

    /** Central mapping from HTTP status + body → typed subclass. Every
     *  code path (sync summarize, rates, usage) goes through here so
     *  the same server shape always produces the same typed exception. */
    static TldrapiException fromResponse(int status, JsonNode body, String requestId, int retryAfterSeconds) {
        JsonNode safeBody = (body == null) ? JsonUtil.MAPPER.createObjectNode() : body;
        String message = extractMessage(safeBody, status);
        String errCode = extractErrCode(safeBody);

        if (status == 401 || status == 403) {
            return new Authentication(message, status, requestId, safeBody);
        }
        if (status == 402) {
            return new InsufficientCredits(message, status, requestId, safeBody);
        }
        if (status == 429) {
            return new RateLimit(message, status, requestId, safeBody, retryAfterSeconds);
        }
        if (status == 400) {
            if ("language_not_supported".equals(errCode) || errCode.contains("language")) {
                return new LanguageNotSupported(message, status, requestId, safeBody);
            }
            if ("quality_selection_requires_paid_plan".equals(errCode)) {
                return new QualitySelectionRequiresPaidPlan(message, status, requestId, safeBody);
            }
            return new InvalidRequest(message, status, requestId, safeBody);
        }
        if (status >= 500 && status < 600) {
            return new Server(message, status, requestId, safeBody);
        }
        return new TldrapiException(message, status, requestId, safeBody);
    }

    private static String extractMessage(JsonNode body, int status) {
        for (String k : new String[]{"message", "detail", "error", "reason"}) {
            JsonNode v = body.get(k);
            if (v != null && v.isTextual() && !v.asText().isEmpty()) {
                return v.asText();
            }
        }
        return "HTTP " + status;
    }

    private static String extractErrCode(JsonNode body) {
        for (String k : new String[]{"error_code", "error"}) {
            JsonNode v = body.get(k);
            if (v != null && v.isTextual()) {
                return v.asText().toLowerCase();
            }
        }
        return "";
    }
}
