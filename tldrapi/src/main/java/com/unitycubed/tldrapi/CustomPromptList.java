package com.unitycubed.tldrapi;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of {@link Tldrapi#customPromptsList}. Instruction text is NOT
 *  included per row — call {@link Tldrapi#customPromptGet(String)} for
 *  the full detail. */
public final class CustomPromptList {
    private final String customerId;
    private final List<Summary> customPrompts;
    private final JsonNode raw;

    CustomPromptList(String customerId, List<Summary> customPrompts, JsonNode raw) {
        this.customerId = customerId;
        this.customPrompts = Collections.unmodifiableList(customPrompts);
        this.raw = raw;
    }

    public String getCustomerId()          { return customerId; }
    public List<Summary> getCustomPrompts(){ return customPrompts; }
    public JsonNode getRaw()               { return raw; }

    static CustomPromptList fromResponse(HttpTransport.Response r) throws TldrapiException {
        JsonNode body = r.jsonOrEmpty();
        List<Summary> list = new ArrayList<>();
        if (body.path("custom_prompts").isArray()) {
            for (JsonNode row : body.path("custom_prompts")) {
                list.add(new Summary(
                    row.path("id").asText(""),
                    row.path("voice_name").asText(""),
                    row.path("status").asText(""),
                    row.hasNonNull("voice_reference") ? row.path("voice_reference").asText(null) : null,
                    row.hasNonNull("approved_alias") ? row.path("approved_alias").asText(null) : null,
                    row.hasNonNull("rejection_reason") ? row.path("rejection_reason").asText(null) : null,
                    row.path("submitted_at").asText(""),
                    row.path("reviewed_at").asText("")
                ));
            }
        }
        return new CustomPromptList(body.path("customer_id").asText(""), list, body);
    }

    /** One row of {@link CustomPromptList#getCustomPrompts()}. */
    public static final class Summary {
        private final String id;
        private final String voiceName;
        private final String status;
        private final String voiceReference;
        private final String approvedAlias;
        private final String rejectionReason;
        private final String submittedAt;
        private final String reviewedAt;

        Summary(String id, String voiceName, String status, String voiceReference,
                String approvedAlias, String rejectionReason, String submittedAt,
                String reviewedAt) {
            this.id = id;
            this.voiceName = voiceName;
            this.status = status;
            this.voiceReference = voiceReference;
            this.approvedAlias = approvedAlias;
            this.rejectionReason = rejectionReason;
            this.submittedAt = submittedAt;
            this.reviewedAt = reviewedAt;
        }

        public String getId()              { return id; }
        public String getVoiceName()       { return voiceName; }
        public String getStatus()          { return status; }
        public String getVoiceReference()  { return voiceReference; }
        public String getApprovedAlias()   { return approvedAlias; }
        public String getRejectionReason() { return rejectionReason; }
        public String getSubmittedAt()     { return submittedAt; }
        public String getReviewedAt()      { return reviewedAt; }
    }
}
