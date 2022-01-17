package com.evolveum.midpoint.model.impl.correlator.idmatch.data.structure;

/**
 * Represents a JSON request to match a person (`Request`) or to force a reconciliation of
 * a person (`Forced Reconciliation Request`).
 *
 * TODO consider nullity of the properties
 */
public class JsonRequest {

    public JsonRequest(String sorLabel, String sorId, String objectToSend) {
        this.sorLabel = sorLabel;
        this.sorId = sorId;
        this.objectToSend = objectToSend;
    }

    /** Identifier of a SOR */
    private final String sorLabel;

    /** Identifier of a record within the SoR. */
    private final String sorId;

    /** JSON-serialized form of a message body. */
    private final String objectToSend;

    public String getSorLabel() {
        return sorLabel;
    }

    public String getSorId() {
        return sorId;
    }

    public String getObjectToSend() {
        return objectToSend;
    }
}
