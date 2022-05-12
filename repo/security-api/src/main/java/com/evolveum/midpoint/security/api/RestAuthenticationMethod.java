/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.api;

import org.apache.commons.lang3.StringUtils;

public enum RestAuthenticationMethod {

    BASIC("Basic"),
    SECURITY_QUESTIONS("SecQ"),
    CLUSTER("Cluster");


    private String method;

    RestAuthenticationMethod(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

    protected boolean equals(String authenticationType) {
        if (StringUtils.isBlank(authenticationType)) {
            return false;
        }

        if (getMethod().equals(authenticationType)) {
            return true;
        }
        return false;
    }
}
