package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * Official Java client for the TLDRapi text-summarization API.
 *
 * <p>Zero external HTTP client: uses {@link java.net.http.HttpClient} from
 * the JDK. JSON via Jackson. Thread-safe — construct once, share widely.
 *
 * <p>Auth at launch is RapidAPI-only. Subscribe to the TLDRapi listing on
 * RapidAPI, get an X-RapidAPI-Key, and pass it to the builder:
 *
 * <pre>{@code
 * Tldrapi client = Tldrapi.builder()
 *     .rapidApiKey(System.getenv("TLDRAPI_RAPIDAPI_KEY"))
 *     .build();
 *
 * SummarizeResult r = client.summarize("Long text goes here.",
 *     SummarizeOptions.builder().tier("standard").build());
 * System.out.println(r.getSummary());
 * }</pre>
 *
 * <p>Direct signup (bypassing RapidAPI) is a post-launch feature; a
 * second builder path will land when it ships.
 *
 * <p>Retry policy: 5xx and transport errors retry 3× by default with
 * exponential backoff plus jitter. 4xx (including 429) is <b>never</b>
 * auto-retried — that would either burn credits or worsen a throttle.
 */
public final class Tldrapi {

    /** SDK version, emitted in the User-Agent header. */
    public static final String VERSION = "0.1.0";

    /** RapidAPI hostname for the TLDRapi listing. */
    public static final String DEFAULT_RAPIDAPI_HOST = "tldrapi.p.rapidapi.com";

    private final TldrapiOptions options;
    private final HttpTransport transport;

    private Tldrapi(TldrapiOptions options) {
        this.options = options;
        this.transport = new HttpTransport(options);
    }

    /** Start building a client. RapidAPI key is required; everything else
     * has a sensible default. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Send text to {@code POST /summarize} and return the summary +
     * usage details. Blocking call — use a background executor if you
     * need concurrency.
     *
     * @param inputText  the source text; must be non-empty
     * @param opts       per-call options (tier, session, model-alias, ...)
     * @return           parsed result including summary, session id, usage
     * @throws TldrapiException on any non-2xx or transport failure
     */
    public SummarizeResult summarize(String inputText, SummarizeOptions opts) throws TldrapiException {
        if (inputText == null || inputText.isEmpty()) {
            throw new IllegalArgumentException("inputText must be non-empty");
        }
        if (opts == null) opts = SummarizeOptions.builder().build();
        if (opts.getTier() != null && !VALID_TIERS.contains(opts.getTier())) {
            throw new IllegalArgumentException(
                "invalid tier '" + opts.getTier() + "' — must be one of quick, standard, deep, premium, ultra");
        }

        ObjectNode payload = JsonUtil.MAPPER.createObjectNode();
        payload.put("input_text", inputText);
        if (opts.getSessionId() != null) payload.put("session_id", opts.getSessionId());
        if (opts.getModelAlias() != null) payload.put("model_alias", opts.getModelAlias());

        Map<String, String> headers = buildHeaders(opts.getTier(), opts.getExtraHeaders());
        if (opts.isAllowOverage()) {
            headers.put("X-Allow-Overage", "true");
        }

        HttpTransport.Response resp = transport.request(
            "POST", "/summarize", payload, headers, opts.getPerCallTimeoutSeconds());
        return SummarizeResult.fromResponse(resp);
    }

    /** Convenience overload: default options (no tier, no session, etc.). */
    public SummarizeResult summarize(String inputText) throws TldrapiException {
        return summarize(inputText, null);
    }

    /**
     * Fetch {@code GET /rates} — the credits-per-call table for every
     * quality tier. Useful for surfacing "this call will cost N credits"
     * before making it.
     */
    public Rates rates() throws TldrapiException {
        HttpTransport.Response resp = transport.request(
            "GET", "/rates", null, buildHeaders(null, null), 0);
        return Rates.fromResponse(resp);
    }

    /**
     * Fetch {@code GET /usage} — the caller's aggregate usage stats:
     * calls made, credits charged, credits remaining, billing period.
     */
    public UsageStats usage() throws TldrapiException {
        HttpTransport.Response resp = transport.request(
            "GET", "/usage", null, buildHeaders(null, null), 0);
        return UsageStats.fromResponse(resp);
    }

    private Map<String, String> buildHeaders(String tier, Map<String, String> extra) {
        Map<String, String> h = new java.util.LinkedHashMap<>();
        h.put("Content-Type", "application/json");
        h.put("User-Agent", options.getUserAgent());
        h.put("X-RapidAPI-Key", options.getRapidApiKey());
        h.put("X-RapidAPI-Host", options.getRapidApiHost());
        if (tier != null && !tier.isEmpty()) {
            h.put("X-Quality", tier);
        }
        if (extra != null) h.putAll(extra);
        return h;
    }

    private static final java.util.Set<String> VALID_TIERS = new java.util.HashSet<>(
        java.util.Arrays.asList("quick", "standard", "deep", "premium", "ultra"));

    // ---------------- builder ----------------

    /**
     * Builder for {@link Tldrapi}. The only required field is
     * {@code rapidApiKey}; everything else has a working default.
     */
    public static final class Builder {
        private String rapidApiKey;
        private String rapidApiHost = DEFAULT_RAPIDAPI_HOST;
        private String baseUrl;
        private int timeoutSeconds = 60;
        private int retries = 3;
        private String userAgent;
        private java.net.http.HttpClient httpClient;

        /** RapidAPI key (X-RapidAPI-Key). Required. */
        public Builder rapidApiKey(String v) { this.rapidApiKey = v; return this; }
        /** RapidAPI host (X-RapidAPI-Host). Defaults to
         *  {@value Tldrapi#DEFAULT_RAPIDAPI_HOST}. */
        public Builder rapidApiHost(String v) { this.rapidApiHost = v; return this; }
        /** Base URL override. Defaults to https://{rapidApiHost}. Useful
         *  for staging or mock-server testing. */
        public Builder baseUrl(String v) { this.baseUrl = v; return this; }
        /** Per-request timeout, in seconds. Defaults to 60. */
        public Builder timeoutSeconds(int v) { this.timeoutSeconds = v; return this; }
        /** Retry count for 5xx + transport errors. Defaults to 3.
         *  Set to 0 to disable retries. 4xx and 429 are never retried. */
        public Builder retries(int v) { this.retries = v; return this; }
        /** Override the User-Agent header. Default is
         *  {@code tldrapi-java/<VERSION>}. */
        public Builder userAgent(String v) { this.userAgent = v; return this; }
        /** Inject a custom HttpClient (proxy, custom SSL, etc.). */
        public Builder httpClient(java.net.http.HttpClient v) { this.httpClient = v; return this; }

        public Tldrapi build() {
            if (rapidApiKey == null || rapidApiKey.isEmpty()) {
                throw new IllegalArgumentException(
                    "rapidApiKey is required (subscribe on RapidAPI to obtain one)");
            }
            String base = baseUrl;
            if (base == null || base.isEmpty()) base = "https://" + rapidApiHost;
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            String ua = userAgent;
            if (ua == null || ua.isEmpty()) ua = "tldrapi-java/" + VERSION;
            int r = Math.max(0, retries);
            int t = timeoutSeconds > 0 ? timeoutSeconds : 60;
            TldrapiOptions opts = new TldrapiOptions(
                rapidApiKey, rapidApiHost, base, t, r, ua, httpClient);
            return new Tldrapi(opts);
        }
    }
}
