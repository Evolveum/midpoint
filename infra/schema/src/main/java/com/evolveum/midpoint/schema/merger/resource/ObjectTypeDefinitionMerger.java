/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.resource;

import com.evolveum.midpoint.schema.merger.OriginMarker;
import com.evolveum.midpoint.schema.merger.objdef.ResourceObjectTypeDefinitionMergeOperation;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.schema.merger.BaseItemMerger;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.schema.util.ShadowUtil.resolveDefault;

import java.util.stream.Collectors;

/**
 * A merger specific to resource definitions: creates inheritance relations between the same definitions
 * (matched by kind and intent).
 */
public class ObjectTypeDefinitionMerger extends BaseItemMerger<PrismContainer<ResourceObjectTypeDefinitionType>> {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectTypeDefinitionMerger.class);

    public ObjectTypeDefinitionMerger(@Nullable OriginMarker originMarker) {
        super(originMarker);
    }

    protected void mergeInternal(
            @NotNull PrismContainer<ResourceObjectTypeDefinitionType> target,
            @NotNull PrismContainer<ResourceObjectTypeDefinitionType> source)
            throws ConfigurationException, SchemaException {
        for (ResourceObjectTypeDefinitionType sourceDefinition : source.getRealValues()) {
            ResourceObjectTypeDefinitionType matching =
                    find(target, sourceDefinition.getKind(), sourceDefinition.getIntent());
            if (matching != null) {
                LOGGER.trace("Adding {}/{} (merged)", sourceDefinition.getKind(), sourceDefinition.getIntent());
                new ResourceObjectTypeDefinitionMergeOperation(
                        matching, sourceDefinition, originMarker)
                        .execute();
            } else {
                LOGGER.trace("Adding {}/{} (as is)", sourceDefinition.getKind(), sourceDefinition.getIntent());
                //noinspection unchecked
                target.add(
                        createMarkedClone(sourceDefinition)
                                .asPrismContainerValue());
            }
        }
    }

    /**
     * Finds a matching definition. Obviously, this must be called before the respective source definition is transferred
     * into the target. (Which is ensured, unless there are duplicates regarding kind and intent.)
     */
    private ResourceObjectTypeDefinitionType find(
            PrismContainer<ResourceObjectTypeDefinitionType> container, ShadowKindType kind, String intent) {
        var matching = container.getRealValues().stream()
                .filter(def -> matchesKindIntent(def, kind, intent))
                .collect(Collectors.toList());
        return MiscUtil.extractSingleton(matching,
                () -> new IllegalStateException("Multiple matching definitions for " + kind + "/" + intent + ": " + matching));
    }

    /** We take defaults for kind and intent into account here. */
    private boolean matchesKindIntent(ResourceObjectTypeDefinitionType def, ShadowKindType kind, String intent) {
        return resolveDefault(def.getKind()) == resolveDefault(kind)
                && resolveDefault(def.getIntent()).equals(resolveDefault(intent));
    }
}
