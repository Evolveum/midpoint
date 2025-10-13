/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch.constants;

/**
 * TODO review
 */
public enum ResponseType {

    CREATED(200),
    EXISTING(201),
    ACCEPTED(202);

    private final int responseCode;

    ResponseType(int responseCode) {
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }
}
