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
    public static final String DEFAULT_RAPIDAPI_HOST = "tldrapi-summarizer.p.rapidapi.com";

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
        if (opts.getConfig() != null) {
            ObjectNode cfg = opts.getConfig().toJson();
            if (cfg != null) payload.set("config", cfg);
        }

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

    // ─── /convert/{json,html,md}-to-text (text-body) ─────────────────

    /** POST {@code /convert/json-to-text} — normalize JSON to plaintext. */
    public ConvertResult convertJsonToText(String text) throws TldrapiException {
        return convertText("/convert/json-to-text", text, false);
    }

    /** POST {@code /convert/html-to-text} — strip HTML to plaintext. */
    public ConvertResult convertHtmlToText(String text) throws TldrapiException {
        return convertText("/convert/html-to-text", text, false);
    }

    /** POST {@code /convert/md-to-text} — render Markdown to plaintext. */
    public ConvertResult convertMdToText(String text) throws TldrapiException {
        return convertText("/convert/md-to-text", text, false);
    }

    private ConvertResult convertText(String path, String text, boolean allowOverage) throws TldrapiException {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("text must be non-empty");
        }
        ObjectNode payload = JsonUtil.MAPPER.createObjectNode();
        payload.put("text", text);
        Map<String, String> headers = buildHeaders(null, null);
        if (allowOverage) headers.put("X-Allow-Overage", "true");
        HttpTransport.Response resp = transport.request("POST", path, payload, headers, 0);
        return ConvertResult.fromResponse(resp);
    }

    // ─── multipart /convert file endpoints ───────────────────────────

    /** POST {@code /convert/doc-to-text} — extract plaintext from a
     *  doc/docx/odt/rtf file. */
    public ConvertResult convertDocToText(byte[] fileBytes, String filename) throws TldrapiException {
        return convertFile("/convert/doc-to-text", fileBytes, filename, null);
    }

    /** POST {@code /convert/doc-to-latex} — extract LaTeX source. */
    public ConvertResult convertDocToLatex(byte[] fileBytes, String filename) throws TldrapiException {
        return convertFile("/convert/doc-to-latex", fileBytes, filename, null);
    }

    /** POST {@code /convert/docx-to-text} — deprecated alias of
     *  {@link #convertDocToText}. */
    public ConvertResult convertDocxToText(byte[] fileBytes, String filename) throws TldrapiException {
        return convertFile("/convert/docx-to-text", fileBytes, filename, null);
    }

    private ConvertResult convertFile(String path, byte[] fileBytes, String filename, String backend)
            throws TldrapiException {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("fileBytes must be non-empty");
        }
        Map<String, String> headers = buildHeaders(null, null);
        headers.remove("Content-Type"); // multipart owns Content-Type
        if (backend != null) headers.put("X-PDF-Backend", backend);
        HttpTransport.Response resp = transport.requestMultipart(
            path, fileBytes,
            (filename == null || filename.isEmpty()) ? "upload" : filename,
            null, headers, 0);
        return ConvertResult.fromResponse(resp);
    }

    /** POST {@code /convert/pdf-to-latex} — extract LaTeX from a PDF.
     *  On HTTP 202 the returned result carries jobId / pollUrl /
     *  status="queued"; poll {@link #pdfStatus(String)}.
     *  @param backend {@code "auto"}, {@code "text"}, or {@code "modal"};
     *                 null → server picks. */
    public PdfConvertResult convertPdfToLatex(byte[] fileBytes, String filename, String backend)
            throws TldrapiException {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("fileBytes must be non-empty");
        }
        if (backend != null && !backend.equals("auto") && !backend.equals("text") && !backend.equals("modal")) {
            throw new IllegalArgumentException("backend must be one of auto,text,modal (got " + backend + ")");
        }
        Map<String, String> headers = buildHeaders(null, null);
        headers.remove("Content-Type");
        if (backend != null) headers.put("X-PDF-Backend", backend);
        HttpTransport.Response resp = transport.requestMultipart(
            "/convert/pdf-to-latex", fileBytes,
            (filename == null || filename.isEmpty()) ? "upload.pdf" : filename,
            "application/pdf", headers, 0);
        return PdfConvertResult.fromResponse(resp);
    }

    /** GET {@code /convert/pdf-to-latex/status/:job_id} — poll async PDF. */
    public PdfConvertResult pdfStatus(String jobId) throws TldrapiException {
        if (jobId == null || jobId.isEmpty()) {
            throw new IllegalArgumentException("jobId required");
        }
        HttpTransport.Response resp = transport.request(
            "GET",
            "/convert/pdf-to-latex/status/" + urlPathEncode(jobId),
            null, buildHeaders(null, null), 0);
        return PdfConvertResult.fromResponse(resp);
    }

    // ─── rates history + usage range ────────────────────────────────

    /** GET {@code /rates/history}. */
    public RatesHistory ratesHistory() throws TldrapiException {
        HttpTransport.Response resp = transport.request(
            "GET", "/rates/history", null, buildHeaders(null, null), 0);
        return RatesHistory.fromResponse(resp);
    }

    /** GET {@code /usage/range?from=&to=} (YYYY-MM-DD). */
    public UsageRange usageRange(String from, String to) throws TldrapiException {
        if (from == null || from.isEmpty() || to == null || to.isEmpty()) {
            throw new IllegalArgumentException("from and to required (YYYY-MM-DD)");
        }
        String path = "/usage/range?from=" + urlQueryEncode(from) + "&to=" + urlQueryEncode(to);
        HttpTransport.Response resp = transport.request("GET", path, null, buildHeaders(null, null), 0);
        return UsageRange.fromResponse(resp);
    }

    // ─── custom prompts (Business/Enterprise) ───────────────────────

    /** POST {@code /custom-prompts/submit}. */
    public CustomPromptResult customPromptSubmit(String voiceName, String instruction,
                                                 String sessionIdOrNull, boolean allowOverage) throws TldrapiException {
        if (voiceName == null || voiceName.isEmpty() || instruction == null || instruction.isEmpty()) {
            throw new IllegalArgumentException("voiceName and instruction required");
        }
        ObjectNode body = JsonUtil.MAPPER.createObjectNode();
        body.put("voice_name", voiceName);
        body.put("instruction", instruction);
        if (sessionIdOrNull != null) body.put("session_id", sessionIdOrNull);
        Map<String, String> headers = buildHeaders(null, null);
        if (allowOverage) headers.put("X-Allow-Overage", "true");
        HttpTransport.Response resp = transport.request("POST", "/custom-prompts/submit", body, headers, 0);
        return CustomPromptResult.fromResponse(resp);
    }

    /** POST {@code /custom-prompts/list}. */
    public CustomPromptList customPromptsList(String sessionIdOrNull) throws TldrapiException {
        ObjectNode body = JsonUtil.MAPPER.createObjectNode();
        if (sessionIdOrNull != null) body.put("session_id", sessionIdOrNull);
        HttpTransport.Response resp = transport.request(
            "POST", "/custom-prompts/list", body, buildHeaders(null, null), 0);
        return CustomPromptList.fromResponse(resp);
    }

    /** GET {@code /custom-prompts/:id}. */
    public CustomPromptDetail customPromptGet(String promptId) throws TldrapiException {
        if (promptId == null || promptId.isEmpty()) {
            throw new IllegalArgumentException("promptId required");
        }
        HttpTransport.Response resp = transport.request(
            "GET", "/custom-prompts/" + urlPathEncode(promptId), null, buildHeaders(null, null), 0);
        return CustomPromptDetail.fromResponse(resp);
    }

    /** Percent-encode a single path segment (RFC 3986 unreserved set). */
    private static String urlPathEncode(String s) {
        try {
            // URLEncoder is form-encoding, which turns space into '+', not %20.
            // Fix that inline — paths want %20.
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
        } catch (Exception e) {
            return s;
        }
    }
    /** Percent-encode a query-string value (space → '+' is fine here). */
    private static String urlQueryEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
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
