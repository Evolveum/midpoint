/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.identities.IdentityItemConfiguration;
import com.evolveum.midpoint.model.impl.lens.identities.IdentityManagementConfiguration;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusIdentitiesTypeUtil;
import com.evolveum.midpoint.schema.util.FocusIdentityTypeUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

/**
 * Normalizes focus identity data related to projections (resource objects).
 */
class FocusIdentityDataUpdater<F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(FocusIdentityDataUpdater.class);

    @NotNull private final PathKeyedMap<List<InboundMappingInContext<?, ?>>> mappingsMap;
    @NotNull private final IdentityManagementConfiguration configuration;
    @NotNull private final LensContext<F> context;
    @NotNull private final MappingEvaluationEnvironment env;
    @NotNull private final OperationResult result;
    @NotNull private final ModelBeans beans;
    @NotNull private final List<FocusIdentityType> identities = new ArrayList<>();

    FocusIdentityDataUpdater(
            @NotNull PathKeyedMap<List<InboundMappingInContext<?, ?>>> mappingsMap,
            @NotNull IdentityManagementConfiguration configuration,
            @NotNull LensContext<F> context,
            @NotNull MappingEvaluationEnvironment env,
            @NotNull OperationResult result,
            @NotNull ModelBeans beans) {
        this.mappingsMap = mappingsMap;
        this.configuration = configuration;
        this.context = context;
        this.env = env;
        this.result = result;
        this.beans = beans;
    }

    public void update() throws ConfigurationException, SchemaException, ExpressionEvaluationException {
        for (Map.Entry<ItemPath, List<InboundMappingInContext<?, ?>>> entry : mappingsMap.entrySet()) {
            ItemPath itemPath = entry.getKey();
            IdentityItemConfiguration itemConfiguration = configuration.getForPath(itemPath);
            if (itemConfiguration != null) {
                List<InboundMappingInContext<?, ?>> mappingInContextList = entry.getValue();
                LOGGER.trace("Processing {} mapping(s) for {}", mappingInContextList.size(), itemPath);
                new IdentityItemUpdater<>(itemPath, itemConfiguration, mappingInContextList)
                        .update();
            } else {
                LOGGER.trace("No item configuration for {}, skipping", itemPath);
            }
        }
        beans.identitiesManager.applyOnInbounds(identities, context);
    }

    /**
     * Assuming all mappings share the output value type `V` and definition `D`.
     */
    class IdentityItemUpdater<V extends PrismValue, D extends ItemDefinition<?>> {

        @NotNull private final ItemPath itemPath;
        @NotNull private final IdentityItemConfiguration itemConfiguration;

        /** All "qualified" mappings for the given target item */
        @NotNull private final List<InboundMappingInContext<V, D>> mappingInContextList;

        /** Identities to be filled-in (for this item), along with respective mappings. */
        @NotNull private final List<IdentityWithMappings> identitiesWithMappings = new ArrayList<>();

        @SuppressWarnings({ "rawtypes", "unchecked" })
        IdentityItemUpdater(
                @NotNull ItemPath itemPath,
                @NotNull IdentityItemConfiguration itemConfiguration,
                @NotNull List mappingInContextList) {
            this.itemPath = itemPath;
            this.itemConfiguration = itemConfiguration;
            this.mappingInContextList = mappingInContextList;
        }

        void update()
                throws SchemaException, ConfigurationException, ExpressionEvaluationException {
            for (InboundMappingInContext<V, D> mappingInContext : mappingInContextList) {
                if (!mappingInContext.isProjectionBeingDeleted() && !mappingInContext.isProjectionGone()) {
                    createOrUpdateIdentityWithMappings(mappingInContext);
                }
            }

            // TODO what about higher-order contexts? There may be duplicate mappings there.

            for (IdentityWithMappings identityWithMappings : identitiesWithMappings) {
                addIdentityItemValue(identityWithMappings);
            }
        }

        private void createOrUpdateIdentityWithMappings(InboundMappingInContext<V, D> mappingInContext) {
            FocusIdentitySourceType source = mappingInContext.getProjectionContextRequired().getFocusIdentitySource();
            for (IdentityWithMappings identityWithMappings : identitiesWithMappings) {
                if (FocusIdentityTypeUtil.matches(identityWithMappings.identityBean, source)) {
                    identityWithMappings.mappingInContextList.add(mappingInContext);
                    return;
                }
            }
            FocusIdentityType matchingGlobal = FocusIdentitiesTypeUtil.getMatchingIdentity(identities, source);
            FocusIdentityType global;
            if (matchingGlobal != null) {
                global = matchingGlobal;
            } else {
                global = new FocusIdentityType()
                        .source(source);
                identities.add(global);
            }

            identitiesWithMappings.add(
                    new IdentityWithMappings(
                            global,
                            new ArrayList<>(List.of(mappingInContext))));
        }

        /** Determines the (consolidated) value of given focus item and adds it to the identity items map. */
        private void addIdentityItemValue(IdentityWithMappings identityWithMappings)
                throws SchemaException, ConfigurationException, ExpressionEvaluationException {
            V value = consolidateMappingOutput(identityWithMappings.mappingInContextList);
            LOGGER.trace("Consolidation of '{}' yields: {}", itemPath, value);
            if (value != null) {
                beans.identitiesManager.addIdentityItem(identityWithMappings.identityBean, itemConfiguration, value);
            }
        }

        /**
         * A "micro-consolidation" of mappings related to the same projection context.
         * Usually there would be a single mapping. But it is possible that there are multiple ones,
         * e.g. a standard and a weak one.
         */
        private V consolidateMappingOutput(List<InboundMappingInContext<V, D>> mappings)
                throws SchemaException, ExpressionEvaluationException {

            assert !mappings.isEmpty();
            InboundMappingInContext<V, D> mappingInContext0 = mappings.get(0);
            D targetItemDefinition = mappingInContext0.getMapping().getOutputDefinition();
            assert targetItemDefinition != null;

            DeltaSetTriple<ItemValueWithOrigin<V, D>> deltaSetTriple = PrismContext.get().deltaFactory().createDeltaSetTriple();
            for (InboundMappingInContext<V, D> mapping : mappings) {
                if (!mapping.isProjectionBeingDeleted()) {
                    DeltaSetTriple<ItemValueWithOrigin<V, D>> ivwoTriple =
                            ItemValueWithOrigin.createOutputTriple(mapping.getMapping());
                    if (ivwoTriple != null) {
                        deltaSetTriple.merge(ivwoTriple);
                    }
                }
            }

            // We are not interested in real consolidation here. We want to get something we could index, in order to facilitate
            // the execution of correlation queries. In the future, though, we will use the identity data to derive the output
            // of inbound mappings. At that time, it will be quite important to compute these values precisely. But, at that time,
            // we will know e.g. a priori deltas, existing item values, and so on.
            try (IvwoConsolidator<V, D, ItemValueWithOrigin<V, D>> consolidator =
                    new IvwoConsolidatorBuilder<V, D, ItemValueWithOrigin<V, D>>()
                            .itemPath(itemPath)
                            .ivwoTriple(deltaSetTriple)
                            .itemDefinition(targetItemDefinition)
                            .aprioriItemDelta(null)
                            .itemDeltaExists(false) // TODO
                            .existingItem(null) // TODO
                            .valueMatcher(null)
                            .comparator(null)
                            .addUnchangedValues(true)
                            .addUnchangedValuesExceptForNormalMappings(true)
                            .existingItemKnown(true) // TODO
                            .contextDescription(env.contextDescription)
                            .strengthSelector(StrengthSelector.ALL)
                            .valueMetadataComputer(null)
                            .result(result)
                            .customize(null)
                            .build()) {
                ItemDelta<V, D> delta = consolidator.consolidateToDeltaNoMetadata();
                Supplier<String> contextSupplier = () -> "identity item '" + itemPath + "' in "
                        + mappingInContext0.getProjectionContextKeyRequired() + " in " + context;
                if (delta.isReplace()) {
                    return getSingleValue(delta.getValuesToReplace(), contextSupplier);
                } else {
                    return getSingleValue(delta.getValuesToAdd(), contextSupplier);
                }
            }
        }

        private V getSingleValue(Collection<V> values, Supplier<String> contextSupplier) {
            if (values.isEmpty()) {
                return null;
            }
            if (values.size() > 1) {
                LOGGER.warn("Couldn't determine value for {} because there are more of them: {}", contextSupplier.get(), values);
            }
            return values.iterator().next();
        }

        private class IdentityWithMappings {
            /** Identity being filled-in. */
            @NotNull private final FocusIdentityType identityBean;

            /** Relevant mappings. */
            @NotNull private final List<InboundMappingInContext<V, D>> mappingInContextList;

            private IdentityWithMappings(
                    @NotNull FocusIdentityType identityBean,
                    @NotNull List<InboundMappingInContext<V, D>> mappingInContextList) {
                this.identityBean = identityBean;
                this.mappingInContextList = mappingInContextList;
            }
        }
    }
}
