/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
