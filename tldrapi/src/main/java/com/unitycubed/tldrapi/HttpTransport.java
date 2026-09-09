package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Retry-aware HTTP transport. Package-private — the public surface is
 * {@link Tldrapi}, this is its guts.
 *
 * <p>Retry policy: 5xx and IOException/HttpTimeoutException retry up to
 * {@code retries} times with exponential backoff + jitter. 4xx (including
 * 429) is never retried — that would either burn credits or worsen a
 * throttle. Callers who want to handle 429 with a wait/replay do that
 * on {@link TldrapiException.RateLimit#getRetryAfterSeconds()}.
 */
final class HttpTransport {
    private static final long BASE_BACKOFF_MILLIS = 500L;

    private final TldrapiOptions options;
    private final HttpClient http;

    HttpTransport(TldrapiOptions options) {
        this.options = options;
        this.http = options.getHttpClient() != null
            ? options.getHttpClient()
            : HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Fire the request and either return a parsed Response (2xx) or
     *  throw a typed {@link TldrapiException}. */
    Response request(String method, String path, JsonNode jsonBody,
                     Map<String, String> headers, int perCallTimeoutSeconds) throws TldrapiException {
        int retries = options.getRetries();
        int effectiveTimeout = perCallTimeoutSeconds > 0
            ? perCallTimeoutSeconds
            : options.getTimeoutSeconds();

        TldrapiException lastFailure = null;

        for (int attempt = 0; attempt <= retries; attempt++) {
            HttpRequest req = buildRequest(method, path, jsonBody, headers, effectiveTimeout);
            HttpResponse<byte[]> resp;
            try {
                resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            } catch (HttpTimeoutException e) {
                lastFailure = new TldrapiException.Timeout(
                    "request timed out after " + effectiveTimeout + "s", e);
                if (attempt < retries) { sleepBackoff(attempt); continue; }
                throw lastFailure;
            } catch (IOException e) {
                lastFailure = new TldrapiException.Network(
                    "transport error: " + e.getMessage(), e);
                if (attempt < retries) { sleepBackoff(attempt); continue; }
                throw lastFailure;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TldrapiException.Network("interrupted", e);
            }

            int status = resp.statusCode();
            if (status >= 200 && status < 300) {
                return new Response(status, resp.headers().map(), resp.body());
            }
            if (status >= 500 && attempt < retries) {
                sleepBackoff(attempt);
                continue;
            }
            // 4xx (including 429) — never retried. 5xx after retries exhausted — raise.
            JsonNode body = JsonUtil.parseOrMissing(resp.body());
            int retryAfter = parseRetryAfter(firstHeader(resp.headers().map(), "retry-after"));
            String requestId = firstHeader(resp.headers().map(), "x-request-id");
            throw TldrapiException.fromResponse(status, body, requestId, retryAfter);
        }
        // Unreachable — the loop always returns or throws.
        throw lastFailure != null
            ? lastFailure
            : new TldrapiException.Network("unknown transport failure", null);
    }

    private HttpRequest buildRequest(String method, String path, JsonNode jsonBody,
                                     Map<String, String> headers, int timeoutSeconds) throws TldrapiException {
        URI uri = URI.create(options.getBaseUrl() + path);
        HttpRequest.Builder b = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(timeoutSeconds));
        for (Map.Entry<String, String> e : headers.entrySet()) {
            b.header(e.getKey(), e.getValue());
        }

        byte[] payload = null;
        if (jsonBody != null) {
            try {
                payload = JsonUtil.MAPPER.writeValueAsBytes(jsonBody);
            } catch (Exception e) {
                throw new TldrapiException.Network(
                    "failed to serialize request body: " + e.getMessage(), e);
            }
        }

        switch (method) {
            case "GET":
                b.GET();
                break;
            case "POST":
                b.POST(payload == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofByteArray(payload));
                break;
            case "PUT":
                b.PUT(payload == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofByteArray(payload));
                break;
            case "DELETE":
                b.DELETE();
                break;
            default:
                throw new IllegalArgumentException("unsupported HTTP method: " + method);
        }
        return b.build();
    }

    private static void sleepBackoff(int attempt) {
        long base = BASE_BACKOFF_MILLIS * (1L << attempt);
        long jitter = ThreadLocalRandom.current().nextLong(200L);
        try {
            Thread.sleep(base + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static int parseRetryAfter(String v) {
        if (v == null || v.isEmpty()) return 0;
        try {
            int n = Integer.parseInt(v.trim());
            return Math.max(0, n);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String firstHeader(Map<String, List<String>> headers, String nameLower) {
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            if (e.getKey().equalsIgnoreCase(nameLower)) {
                List<String> vs = e.getValue();
                if (vs != null && !vs.isEmpty()) return vs.get(0);
            }
        }
        return "";
    }

    /** Successful response envelope handed to the model-building code. */
    static final class Response {
        private final int status;
        private final Map<String, List<String>> headers;
        private final byte[] body;

        Response(int status, Map<String, List<String>> headers, byte[] body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
        }

        int getStatus() { return status; }

        String headerFirst(String nameLower) {
            return firstHeader(headers, nameLower);
        }

        JsonNode jsonOrEmpty() {
            return JsonUtil.parseOrMissing(body);
        }
    }
}
