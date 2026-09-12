package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of {@link Tldrapi#customPromptSubmit}. On rejection,
 *  {@link #getVoiceReference()} is {@code null}; on approval it's the
 *  string to pass as {@code voice=} on future summarize calls. */
public final class CustomPromptResult {
    private final String id;
    private final String voiceName;
    private final String status;
    private final boolean approved;
    private final String voiceReference;
    private final String rejectionReason;
    private final boolean updatedInPlace;
    private final List<String> supersededIds;
    private final JsonNode raw;

    CustomPromptResult(String id, String voiceName, String status, boolean approved,
                       String voiceReference, String rejectionReason, boolean updatedInPlace,
                       List<String> supersededIds, JsonNode raw) {
        this.id = id;
        this.voiceName = voiceName;
        this.status = status;
        this.approved = approved;
        this.voiceReference = voiceReference;
        this.rejectionReason = rejectionReason;
        this.updatedInPlace = updatedInPlace;
        this.supersededIds = Collections.unmodifiableList(supersededIds);
        this.raw = raw;
    }

    public String  getId()              { return id; }
    public String  getVoiceName()       { return voiceName; }
    public String  getStatus()          { return status; }
    public boolean isApproved()         { return approved; }
    public String  getVoiceReference()  { return voiceReference; }
    public String  getRejectionReason() { return rejectionReason; }
    public boolean isUpdatedInPlace()   { return updatedInPlace; }
    public List<String> getSupersededIds(){ return supersededIds; }
    public JsonNode getRaw()            { return raw; }

    static CustomPromptResult fromResponse(HttpTransport.Response r) throws TldrapiException {
        JsonNode body = r.jsonOrEmpty();
        String status = body.path("status").asText("");
        boolean approvedFlag = body.path("approved").asBoolean(false);
        List<String> superseded = new ArrayList<>();
        if (body.path("superseded_ids").isArray()) {
            for (JsonNode n : body.path("superseded_ids")) {
                if (n.isTextual()) superseded.add(n.asText());
            }
        }
        return new CustomPromptResult(
            body.path("id").asText(""),
            body.path("voice_name").asText(""),
            status,
            approvedFlag || "approved".equals(status),
            body.hasNonNull("voice_reference") ? body.path("voice_reference").asText(null) : null,
            body.hasNonNull("rejection_reason") ? body.path("rejection_reason").asText(null) : null,
            body.path("updated_in_place").asBoolean(false),
            superseded,
            body
        );
    }
}
