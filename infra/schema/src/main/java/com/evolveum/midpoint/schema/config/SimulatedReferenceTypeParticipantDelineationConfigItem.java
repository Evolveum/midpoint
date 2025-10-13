/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.schema.processor.SearchHierarchyScope;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.SimulatedReferenceTypeParticipantDelineationType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public class SimulatedReferenceTypeParticipantDelineationConfigItem
        extends ConfigurationItem<SimulatedReferenceTypeParticipantDelineationType> {

    @SuppressWarnings("unused") // called dynamically
    public SimulatedReferenceTypeParticipantDelineationConfigItem(
            @NotNull ConfigurationItem<SimulatedReferenceTypeParticipantDelineationType> original) {
        super(original);
    }

    public @NotNull QName getObjectClassName() throws ConfigurationException {
        return nonNull(value().getObjectClass(), "object class name");
    }

    public @Nullable ResourceObjectReferenceType getBaseContext() {
        return value().getBaseContext();
    }

    public SearchHierarchyScope getSearchHierarchyScope() {
        return SearchHierarchyScope.fromBeanValue(
                value().getSearchHierarchyScope());
    }

    public @Nullable QName getAuxiliaryObjectClassName() {
        return value().getAuxiliaryObjectClass();
    }
}
