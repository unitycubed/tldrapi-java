package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/** Small Jackson-mapper singleton so we don't allocate one per call.
 *  Kept package-private — consumers shouldn't need to reach for this. */
final class JsonUtil {
    static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() {}

    /** Parse or return a MissingNode for graceful handling of empty
     *  / non-JSON responses. Never throws — the caller decides how
     *  to react to missing fields via JsonNode.path(). */
    static JsonNode parseOrMissing(byte[] body) {
        if (body == null || body.length == 0) return MissingNode.getInstance();
        try {
            return MAPPER.readTree(body);
        } catch (Exception e) {
            return MissingNode.getInstance();
        }
    }
}
