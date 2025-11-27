/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

public class ResourceObjectClassDefinitionConfigItem
        extends ResourceObjectDefinitionConfigItem<ResourceObjectTypeDefinitionType> {

    @SuppressWarnings("unused") // called dynamically
    public ResourceObjectClassDefinitionConfigItem(@NotNull ConfigurationItem<ResourceObjectTypeDefinitionType> original) {
        super(original);
    }

    public @NotNull QName getObjectClassName() throws ConfigurationException {
        return nonNull(value().getObjectClass(), "object class name");
    }

    @Override
    public @NotNull String localDescription() {
        return "refined object class '%s' definition"
                .formatted(value().getObjectClass());
    }
}
