/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;

import org.jetbrains.annotations.NotNull;

/**
 * Resource schema that is complete with regards to the resource definition, i.e., it contains the full `schemaHandling`,
 * including refined object types and object classes, and all the resource-level definitions, e.g., for shadow caching.
 *
 * This schema guarantees that even raw definitions have {@link BasicResourceInformation} filled-in.
 */
public interface CompleteResourceSchema extends ResourceSchema {

    boolean isCaseIgnoreAttributeNames();

    CompleteResourceSchema forLayerImmutable(LayerType layer);
}
