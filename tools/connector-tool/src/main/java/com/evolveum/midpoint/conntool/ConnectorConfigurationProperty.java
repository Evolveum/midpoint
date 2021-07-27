/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.conntool;

import com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdCapabilitiesAndSchemaParser;

import org.identityconnectors.framework.api.ConfigurationProperty;
import org.jetbrains.annotations.NotNull;

/**
 * Wrapper around {@link ConfigurationProperty} that provides some additional information, e.g. {@link #isMultiValued()}.
 */
@SuppressWarnings("WeakerAccess") // called from velocity templates
public class ConnectorConfigurationProperty {

    @NotNull private final ConfigurationProperty property;

    ConnectorConfigurationProperty(@NotNull ConfigurationProperty configurationProperty) {
        this.property = configurationProperty;
    }

    public String getName() {
        return property.getName();
    }

    public String getDisplayName() {
        return property.getDisplayName(null);
    }

    public String getTypeName() {
        return property.getType().getSimpleName();
    }

    public boolean isRequired() {
        return property.isRequired();
    }

    public boolean isConfidential() {
        return property.isConfidential();
    }

    public boolean isMultiValued() {
        return ConnIdCapabilitiesAndSchemaParser.isMultivaluedType(property.getType());
    }

    // Temporary implementation
    @Override
    public String toString() {
        return String.format("%s (%s): %s%s%s%s",
                getDisplayName(), getName(), getTypeName(),
                isMultiValued() ? " multivalued" : "",
                isRequired() ? " required" : "",
                isConfidential() ? " confidential" : "");
    }
}
