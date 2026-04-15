/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ComplexAttributeTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Defines "object type"-like configuration item: object type, complex attribute type, or associated object type (deprecated).
 */
public interface ResourceDataTypeDefinitionConfigItem<T extends Serializable & Cloneable> extends ConfigurationItemable<T> {

    boolean isAbstract();

    void checkSyntaxOfAttributeNames() throws ConfigurationException;

    @NotNull ResourceObjectTypeIdentification getTypeIdentification() throws ConfigurationException;

    /**
     * The {@link ResourceObjectTypeDefinition} is currently hardcoded to contain {@link ResourceObjectTypeDefinitionType}.
     * It won't accept e.g. {@link ComplexAttributeTypeDefinitionType} (or any similar alternative). So, this method converts
     * such types to {@link ResourceObjectTypeDefinitionType} on the fly.
     *
     * Obviously, this is an ugly hack. We'll sort that out later.
     */
    @NotNull ResourceObjectTypeDefinitionType getObjectTypeDefinitionBean() throws ConfigurationException;
}
