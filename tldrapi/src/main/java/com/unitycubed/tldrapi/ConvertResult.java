package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;

/** Result of the text-body {@code /convert/*} endpoints. Shape mirrors
 *  {@code openapi.yaml ConvertTextResponse}. */
public final class ConvertResult {
    private final String output;
    private final String outputFormat;
    private final String inputFormat;
    private final long inputBytes;
    private final long outputChars;
    private final long elapsedMs;
    private final String requestId;
    private final List<JsonNode> warnings;
    private final JsonNode raw;

    ConvertResult(String output, String outputFormat, String inputFormat,
                  long inputBytes, long outputChars, long elapsedMs,
                  String requestId, List<JsonNode> warnings, JsonNode raw) {
        this.output = output;
        this.outputFormat = outputFormat;
        this.inputFormat = inputFormat;
        this.inputBytes = inputBytes;
        this.outputChars = outputChars;
        this.elapsedMs = elapsedMs;
        this.requestId = requestId;
        this.warnings = warnings == null ? Collections.emptyList() : Collections.unmodifiableList(warnings);
        this.raw = raw;
    }

    public String   getOutput()       { return output; }
    public String   getOutputFormat() { return outputFormat; }
    public String   getInputFormat()  { return inputFormat; }
    public long     getInputBytes()   { return inputBytes; }
    public long     getOutputChars()  { return outputChars; }
    public long     getElapsedMs()    { return elapsedMs; }
    public String   getRequestId()    { return requestId; }
    public List<JsonNode> getWarnings(){ return warnings; }
    public JsonNode getRaw()          { return raw; }

    static ConvertResult fromResponse(HttpTransport.Response r) throws TldrapiException {
        JsonNode body = r.jsonOrEmpty();
        java.util.ArrayList<JsonNode> warnings = new java.util.ArrayList<>();
        if (body.path("warnings").isArray()) body.path("warnings").forEach(warnings::add);
        String requestId = body.path("request_id").asText("");
        if (requestId.isEmpty()) requestId = r.headerFirst("x-request-id");
        return new ConvertResult(
            body.path("output").asText(""),
            body.path("output_format").asText(""),
            body.path("input_format").asText(""),
            body.path("input_bytes").asLong(0),
            body.path("output_chars").asLong(0),
            body.path("elapsed_ms").asLong(0),
            requestId,
            warnings,
            body
        );
    }
}
