/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation;

import static com.evolveum.midpoint.util.DebugUtil.debugDumpLazily;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.evolveum.midpoint.model.impl.lens.*;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.PathKeyedMap;

import com.evolveum.midpoint.repo.common.expression.ConsolidationValueMetadataComputer;

import org.apache.commons.collections4.CollectionUtils;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Responsible for consolidation of delta set triple map (plus, minus, zero sets for individual items) to item deltas.
 */
@Experimental
public class DeltaSetTripleMapConsolidation<T extends AssignmentHolderType> {

    private static final Trace LOGGER = TraceManager.getTrace(DeltaSetTripleMapConsolidation.class);

    private static final String OP_CONSOLIDATE = DeltaSetTripleMapConsolidation.class.getName() + ".consolidate";

    /**
     * Item path-keyed map of output delta set triples.
     */
    private final PathKeyedMap<DeltaSetTriple<ItemValueWithOrigin<?, ?>>> outputTripleMap;

    /**
     * Target object, for which deltas are to be produced.
     * It contains the newest state of the object (in the light of previous computations),
     * i.e. object with targetAPrioriDelta already applied.
     */
    private final PrismObject<T> targetObject;

    /**
     * Delta that has led to the current state of the target object.
     */
    private final ObjectDelta<T> targetAPrioriDelta;

    /**
     * Function that tells us whether any delta exists for given target item.
     */
    private final Function<ItemPath, Boolean> itemDeltaExistsProvider;

    /**
     * Provides the definition of the target item. (Normally by looking up by item path, but there are exceptions.)
     */
    private final ItemDefinitionProvider itemDefinitionProvider;

    /**
     * Should the values from zero set be transformed to delta ADD section?
     * This is the case when the whole object is being added.
     */
    private final boolean addUnchangedValues;

    /**
     * Additional settings for the consolidator.
     */
    @Experimental
    private final Consumer<IvwoConsolidatorBuilder> consolidatorCustomizer;

    /**
     * Mapping evaluation environment (context description, now, task).
     */
    private final MappingEvaluationEnvironment env;

    /**
     * Parent result.
     */
    private final OperationResult parentResult;

    /**
     * Child operation result. Present during computeItemDeltas call.
     *
     * Not very nice solution. Still looking for some great idea how to manage operation results
     * in contexts like this.
     */
    @Experimental
    private OperationResult result;

    /**
     * Useful beans.
     */
    private final ModelBeans beans;

    /**
     * Lens context used to determine metadata handling.
     * (Or should we use object template instead?)
     *
     * TODO reconsider this parameter - and don't use it for anything more than metadata affairs!
     */
    @Nullable private final LensContext<?> lensContext;

    /**
     * Result of the computation: the item deltas.
     */
    @NotNull private final Collection<ItemDelta<?,?>> itemDeltas = new ArrayList<>();

    public DeltaSetTripleMapConsolidation(
            PathKeyedMap<DeltaSetTriple<ItemValueWithOrigin<?, ?>>> outputTripleMap,
            PrismObject<T> targetObject,
            ObjectDelta<T> targetAPrioriDelta,
            Function<ItemPath, Boolean> itemDeltaExistsProvider,
            Boolean addUnchangedValuesOverride,
            Consumer<IvwoConsolidatorBuilder> consolidatorCustomizer,
            ItemDefinitionProvider itemDefinitionProvider,
            MappingEvaluationEnvironment env,
            ModelBeans beans,
            @Nullable LensContext<?> lensContext,
            OperationResult parentResult) {
        this.outputTripleMap = outputTripleMap;
        this.targetObject = targetObject;
        this.targetAPrioriDelta = targetAPrioriDelta;
        this.itemDeltaExistsProvider = itemDeltaExistsProvider;
        this.itemDefinitionProvider = itemDefinitionProvider;
        this.consolidatorCustomizer = consolidatorCustomizer;
        this.env = env;
        this.parentResult = parentResult;
        this.beans = beans;
        this.lensContext = lensContext;

        this.addUnchangedValues = addUnchangedValuesOverride != null ?
                addUnchangedValuesOverride : targetAPrioriDelta != null && targetAPrioriDelta.isAdd();
    }

    public void computeItemDeltas() throws ExpressionEvaluationException, SchemaException,
            ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException {

        if (outputTripleMap == null || outputTripleMap.isEmpty()) {
            // Besides other reasons, this is to avoid creating empty operation results,
            // cluttering the tracing output.
            return;
        }

        try {
            this.result = parentResult.createMinorSubresult(OP_CONSOLIDATE);
            LOGGER.trace("Computing deltas in {}, a priori delta:\n{}",
                    env.contextDescription, debugDumpLazily(targetAPrioriDelta));

            for (Map.Entry<ItemPath, DeltaSetTriple<ItemValueWithOrigin<?, ?>>> entry: outputTripleMap.entrySet()) {
                ItemPath itemPath = entry.getKey();
                ItemDelta<?, ?> aprioriItemDelta = LensUtil.getAprioriItemDelta(targetAPrioriDelta, itemPath);
                DeltaSetTriple<ItemValueWithOrigin<?, ?>> deltaSetTriple = entry.getValue();

                ConsolidationValueMetadataComputer valueMetadataComputer;
                if (lensContext != null) {
                    valueMetadataComputer = LensMetadataUtil.createValueMetadataConsolidationComputer(
                            itemPath, lensContext, beans, env, result);
                } else {
                    LOGGER.trace("No lens context -> no value metadata consolidation computer");
                    valueMetadataComputer = null;
                }

                DeltaSetTripleConsolidation<?, ?, ?> itemConsolidation =
                        new DeltaSetTripleConsolidation(itemPath, deltaSetTriple, aprioriItemDelta, valueMetadataComputer);
                CollectionUtils.addIgnoreNull(itemDeltas,
                        itemConsolidation.consolidateItem());
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
            result = null;
        }
    }

    public @NotNull Collection<ItemDelta<?, ?>> getItemDeltas() {
        return itemDeltas;
    }

    private class DeltaSetTripleConsolidation<V extends PrismValue, D extends ItemDefinition<?>, I extends ItemValueWithOrigin<V,D>> {

        /**
         * Path to item that is to be consolidated.
         */
        private final ItemPath itemPath;

        /**
         * Delta set triple that is to be consolidated.
         */
        private final DeltaSetTriple<I> deltaSetTriple;

        /**
         * Existing (apriori) delta that was specified by the caller and/or computed upstream.
         */
        private final ItemDelta<V, D> aprioriItemDelta;

        /**
         * Existing item values. Note: this is the state AFTER apriori item delta is applied!
         */
        private final Item<V, D> existingItem;

        /**
         * Definition of item to be consolidated.
         */
        private final D itemDefinition;

        /**
         * Metadata computer to be used during consolidation.
         */
        private final ConsolidationValueMetadataComputer valueMetadataComputer;

        private ItemDelta<V, D> itemDelta;

        private DeltaSetTripleConsolidation(ItemPath itemPath, DeltaSetTriple<I> deltaSetTriple, ItemDelta<V, D> aprioriItemDelta,
                ConsolidationValueMetadataComputer valueMetadataComputer) throws SchemaException {
            this.itemPath = itemPath;
            this.deltaSetTriple = deltaSetTriple;
            this.aprioriItemDelta = aprioriItemDelta;
            this.existingItem = targetObject != null ? targetObject.findItem(itemPath) : null;
            //noinspection unchecked
            this.itemDefinition = (D) itemDefinitionProvider.getDefinition(itemPath);
            this.valueMetadataComputer = valueMetadataComputer;
        }

        private ItemDelta<?, ?> consolidateItem() throws ExpressionEvaluationException, SchemaException,
                ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
            LOGGER.trace("Computing delta for {} with the delta set triple:\n{}", itemPath, deltaSetTriple.debugDumpLazily());

            computeItemDelta();
            cleanupItemDelta();

            return itemDelta;
        }

        private void computeItemDelta() throws ExpressionEvaluationException, SchemaException,
                ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException {
            try (IvwoConsolidator<V, D, I> consolidator = new IvwoConsolidatorBuilder<V, D, I>()
                    .itemPath(itemPath)
                    .ivwoTriple(deltaSetTriple)
                    .itemDefinition(itemDefinition)
                    .aprioriItemDelta(aprioriItemDelta)
                    .itemDeltaExists(itemDeltaExistsProvider.apply(itemPath))
                    .existingItem(existingItem)
                    .valueMatcher(null)
                    .comparator(null)
                    .addUnchangedValues(addUnchangedValues)
                    .addUnchangedValuesExceptForNormalMappings(true)
                    .existingItemKnown(true)
                    .contextDescription(env.contextDescription)
                    .strengthSelector(StrengthSelector.ALL)
                    .valueMetadataComputer(valueMetadataComputer)
                    .result(result)
                    .customize(consolidatorCustomizer)
                    .build()) {
                itemDelta = consolidator.consolidateTriples();
            }
        }

        private void cleanupItemDelta() throws SchemaException {
            itemDelta.simplify();
            itemDelta.validate(env.contextDescription);
            LOGGER.trace("Computed delta:\n{}", itemDelta.debugDumpLazily());
        }
    }

    public interface ItemDefinitionProvider {
        ItemDefinition<?> getDefinition(@NotNull ItemPath itemPath) throws SchemaException;

        static ItemDefinitionProvider forComplexType(@NotNull ComplexTypeDefinition complexTypeDefinition) {
            return complexTypeDefinition::findItemDefinition;
        }

        static ItemDefinitionProvider forObjectDefinition(@NotNull PrismObjectDefinition<?> objectDefinition) {
            return forComplexType(objectDefinition.getComplexTypeDefinition());
        }
    }
}
