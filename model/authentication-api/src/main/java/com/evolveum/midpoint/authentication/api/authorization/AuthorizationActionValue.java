/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api.authorization;

import com.evolveum.midpoint.util.DisplayableValue;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class AuthorizationActionValue implements DisplayableValue<String>, Serializable {
    private static final long serialVersionUID = 1L;

    private final String value;
    private final String label;
    private final String description;

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
