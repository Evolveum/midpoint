package com.evolveum.midpoint.model.impl.correlator.idmatch.data.structure;

public class JsonRequestList {


    public JsonRequestList(String sorLabel, String sorId, String objectToSend) {
        this.sorLabel = sorLabel;
        this.sorId = sorId;
        this.objectToSend = objectToSend;
    }

    String sorLabel;
    String sorId;
    String objectToSend;


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
