package com.evolveum.midpoint.model.impl.correlator.idmatch.data;

public class ListResponse {


    String message;
    String entity;
    String responseCode;


    public ListResponse(String message, String entity, String responseCode) {
        this.message = message;
        this.entity = entity;
        this.responseCode = responseCode;
    }


    public String getMessage() {
        return message;
    }


    public String getEntity() {
        return entity;
    }


    public String getResponseCode() {
        return responseCode;
    }

}
