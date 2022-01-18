/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch.data;

public class ListResponse {


    String message;
    String entity;
    int responseCode;


    public ListResponse(String message, String entity, int responseCode) {
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


    public int getResponseCode() {
        return responseCode;
    }

}
