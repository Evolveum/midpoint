/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * "One stop shop" for accessing various aspects of a resource (defined by {@link ResourceType} object).
 *
 * HIGHLY EXPERIMENTAL (maybe not a good idea at all)
 */
@Experimental
public class Resource {

    @NotNull private final ResourceType resourceBean;

    private Resource(@NotNull ResourceType resourceBean) {
        this.resourceBean = resourceBean;
    }

    public static Resource of(@NotNull ResourceType resourceBean) {
        return new Resource(resourceBean);
    }

    public @Nullable ResourceSchema getRawSchema() throws SchemaException {
        return ResourceSchemaFactory.getRawSchema(resourceBean);
    }

    public @NotNull ResourceSchema getRawSchemaRequired() throws SchemaException, ConfigurationException {
        return ResourceSchemaFactory.getRawSchemaRequired(resourceBean);
    }

    public @Nullable ResourceSchema getCompleteSchema() throws SchemaException, ConfigurationException {
        return ResourceSchemaFactory.getCompleteSchema(resourceBean);
    }

    public @NotNull ResourceSchema getCompleteSchemaRequired() throws SchemaException, ConfigurationException {
        return ResourceSchemaFactory.getCompleteSchemaRequired(resourceBean);
    }
}
