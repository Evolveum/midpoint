/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.resource;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType.*;

import java.util.Map;

import com.evolveum.midpoint.schema.merger.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.merger.key.DefaultNaturalKeyImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorInstanceSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;

/**
 * Merges {@link ResourceType} objects.
 */
public class ResourceMergeOperation extends BaseMergeOperation<ResourceType> {

    public ResourceMergeOperation(
            @NotNull ResourceType target,
            @NotNull ResourceType source) {

        super(target,
                source,
                new GenericItemMerger(createPathMap(Map.of(
                        F_NAME, RequiredItemMerger.INSTANCE,
                        // connectorRef - default
                        // connectorConfiguration - default
                        F_ABSTRACT, IgnoreSourceItemMerger.INSTANCE,
                        F_ADDITIONAL_CONNECTOR, new GenericItemMerger(
                                DefaultNaturalKeyImpl.of(ConnectorInstanceSpecificationType.F_NAME),
                                emptyPathMap())
                ))));
    }
}
