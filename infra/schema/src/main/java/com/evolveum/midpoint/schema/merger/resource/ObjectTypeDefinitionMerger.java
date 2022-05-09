/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.resource;

import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.schema.merger.BaseCustomItemMerger;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SuperObjectTypeReferenceType;

/**
 * A merger specific to resource definitions: creates inheritance relations between the same definitions
 * (matched by kind and intent).
 */
class ObjectTypeDefinitionMerger extends BaseCustomItemMerger<PrismContainer<ResourceObjectTypeDefinitionType>> {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectTypeDefinitionMerger.class);

    protected void mergeInternal(
            @NotNull PrismContainer<ResourceObjectTypeDefinitionType> target,
            @NotNull PrismContainer<ResourceObjectTypeDefinitionType> source)
            throws ConfigurationException {
        DefinitionsTarget definitionsTarget = new DefinitionsTarget(target);
        for (ResourceObjectTypeDefinitionType sourceDefinition : source.getRealValues()) {
            if (sourceDefinition.getInternalId() != null) {
                // This one must come from super-super-definition, because it's already linked to some descendants.
                // We must copy it as-is.
                LOGGER.trace("Adding {}/{} ({}) (as is, as it's already a dynamic parent)",
                        sourceDefinition.getKind(), sourceDefinition.getIntent(), sourceDefinition.getInternalId());
                definitionsTarget.addAsIs(sourceDefinition);
            } else {
                ResourceObjectTypeDefinitionType matching =
                        definitionsTarget.find(sourceDefinition.getKind(), sourceDefinition.getIntent());
                if (matching != null) {
                    LOGGER.trace("Adding {}/{} (linked)", sourceDefinition.getKind(), sourceDefinition.getIntent());
                    long newId = createNewId(target, source);
                    definitionsTarget.addLinked(sourceDefinition, matching, newId);
                } else {
                    LOGGER.trace("Adding {}/{} (as is)", sourceDefinition.getKind(), sourceDefinition.getIntent());
                    definitionsTarget.addAsIs(sourceDefinition);
                }
            }
        }
    }

    @SafeVarargs
    private long createNewId(PrismContainer<ResourceObjectTypeDefinitionType>... containers) {
        long max = 0;
        for (PrismContainer<ResourceObjectTypeDefinitionType> container : containers) {
            for (ResourceObjectTypeDefinitionType definition : container.getRealValues()) {
                Long internalId = definition.getInternalId();
                if (internalId != null && internalId > max) {
                    max = internalId;
                }
            }
        }
        return max + 1;
    }

    private static class DefinitionsTarget {
        @NotNull private final PrismContainer<ResourceObjectTypeDefinitionType> container;

        DefinitionsTarget(@NotNull PrismContainer<ResourceObjectTypeDefinitionType> container) {
            this.container = container;
        }

        public void addAsIs(@NotNull ResourceObjectTypeDefinitionType definition) {
            try {
                //noinspection unchecked
                container.add(
                        definition.cloneWithoutId().asPrismContainerValue());
            } catch (SchemaException e) {
                throw SystemException.unexpected(e, "when adding object definition");
            }
        }

        void addLinked(
                @NotNull ResourceObjectTypeDefinitionType sourceDefinition,
                @NotNull ResourceObjectTypeDefinitionType targetDefinition,
                long newId)
                throws ConfigurationException {
            if (targetDefinition.getSuper() != null) {
                throw new ConfigurationException("Both explicit and implicit inheritance is not currently supported, for "
                        + targetDefinition);
            }

            ResourceObjectTypeDefinitionType sourceClone = sourceDefinition.cloneWithoutId();
            sourceClone.setInternalId(newId);
            sourceClone.setAbstract(true); // We must not allow this to be instantiated (there would be a conflict then).
            addAsIs(sourceClone);

            targetDefinition.setSuper(
                    new SuperObjectTypeReferenceType()
                            .internalId(newId));
        }

        /**
         * Finds a matching definition; ignoring already linked ones. Obviously, this must be called before
         * source definitions are transferred into the target. (Which is ensured, unless there are duplicates regarding
         * kind and intent.)
         */
        public ResourceObjectTypeDefinitionType find(ShadowKindType kind, String intent) {
            var matching = container.getRealValues().stream()
                    .filter(def -> def.getInternalId() == null
                            && matchesKindIntent(def, kind, intent))
                    .collect(Collectors.toList());
            return MiscUtil.extractSingleton(matching,
                    () -> new IllegalStateException("Multiple matching definitions for " + kind + "/" + intent + ": " + matching));
        }

        /**
         * TODO We intentionally ignore defaults for kind and intent here. Is that OK? (Maybe not!)
         */
        private boolean matchesKindIntent(ResourceObjectTypeDefinitionType def, ShadowKindType kind, String intent) {
            return def.getKind() == kind
                    && Objects.equals(def.getIntent(), intent);
        }
    }
}
