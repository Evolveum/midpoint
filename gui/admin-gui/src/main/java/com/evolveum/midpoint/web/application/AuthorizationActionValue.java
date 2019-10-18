/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.util.DisplayableValue;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class AuthorizationActionValue implements DisplayableValue<String>, Serializable {
    private static final long serialVersionUID = 1L;

    private String value;
    private String label;
    private String description;

    public AuthorizationActionValue(String value, String label, String description) {
        this.value = value;
        this.label = label;
        this.description = description;
    }

    /**
     * @return actionURI
     */
    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "AuthorizationActionValue(value=" + value + ", label=" + label + ", description="
                + description + ")";
    }

}
