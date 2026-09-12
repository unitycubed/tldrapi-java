package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;

/** Result of {@link Tldrapi#convertPdfToLatex}. When the server chose
 *  the async path (HTTP 202), {@code jobId} / {@code pollUrl} /
 *  {@code status} are set and {@code output} is empty — poll
 *  {@link Tldrapi#pdfStatus(String)} until {@code status.equals("done")}. */
public final class PdfConvertResult {
    private final String output;
    private final String outputFormat;
    private final String inputFormat;
    private final long inputBytes;
    private final long pages;
    private final long elapsedMs;
    private final String backend;
    private final String requestId;
    private final List<JsonNode> warnings;
    private final String jobId;
    private final String status;
    private final String pollUrl;
    private final long estimatedSeconds;
    private final JsonNode raw;

    PdfConvertResult(String output, String outputFormat, String inputFormat,
                     long inputBytes, long pages, long elapsedMs, String backend,
                     String requestId, List<JsonNode> warnings, String jobId,
                     String status, String pollUrl, long estimatedSeconds,
                     JsonNode raw) {
        this.output = output;
        this.outputFormat = outputFormat;
        this.inputFormat = inputFormat;
        this.inputBytes = inputBytes;
        this.pages = pages;
        this.elapsedMs = elapsedMs;
        this.backend = backend;
        this.requestId = requestId;
        this.warnings = warnings == null ? Collections.emptyList() : Collections.unmodifiableList(warnings);
        this.jobId = jobId;
        this.status = status;
        this.pollUrl = pollUrl;
        this.estimatedSeconds = estimatedSeconds;
        this.raw = raw;
    }

    public String getOutput()           { return output; }
    public String getOutputFormat()     { return outputFormat; }
    public String getInputFormat()      { return inputFormat; }
    public long   getInputBytes()       { return inputBytes; }
    public long   getPages()            { return pages; }
    public long   getElapsedMs()        { return elapsedMs; }
    public String getBackend()          { return backend; }
    public String getRequestId()        { return requestId; }
    public List<JsonNode> getWarnings() { return warnings; }
    public String getJobId()            { return jobId; }
    public String getStatus()           { return status; }
    public String getPollUrl()          { return pollUrl; }
    public long   getEstimatedSeconds() { return estimatedSeconds; }
    public JsonNode getRaw()            { return raw; }

    static PdfConvertResult fromResponse(HttpTransport.Response r) throws TldrapiException {
        JsonNode body = r.jsonOrEmpty();
        java.util.ArrayList<JsonNode> warnings = new java.util.ArrayList<>();
        if (body.path("warnings").isArray()) body.path("warnings").forEach(warnings::add);
        String requestId = body.path("request_id").asText("");
        if (requestId.isEmpty()) requestId = r.headerFirst("x-request-id");
        // 202 → async (queued). 200 → sync (status defaults to "done").
        boolean async = r.getStatus() == 202
            || "queued".equals(body.path("status").asText(""))
            || "running".equals(body.path("status").asText(""));
        String defaultStatus = async ? "queued" : "done";
        String inputFormat = body.path("input_format").asText("pdf");
        return new PdfConvertResult(
            async ? "" : body.path("output").asText(""),
            async ? "" : body.path("output_format").asText(""),
            inputFormat,
            body.path("input_bytes").asLong(0),
            body.path("pages").asLong(0),
            async ? 0L : body.path("elapsed_ms").asLong(0),
            body.path("backend").asText(""),
            requestId,
            warnings,
            body.path("job_id").asText(""),
            body.path("status").asText(defaultStatus),
            body.path("poll_url").asText(""),
            body.path("estimated_seconds").asLong(0),
            body
        );
    }
}
