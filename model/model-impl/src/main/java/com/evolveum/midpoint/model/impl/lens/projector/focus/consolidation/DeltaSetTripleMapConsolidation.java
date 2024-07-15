/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import com.evolveum.midpoint.model.impl.lens.projector.focus.DeltaSetTripleIvwoMap;

import com.evolveum.midpoint.prism.delta.ObjectDelta;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ConsolidationValueMetadataComputer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import static com.evolveum.midpoint.util.DebugUtil.debugDumpLazily;

/**
 * Responsible for consolidation of a {@link DeltaSetTripleIvwoMap} (plus, minus, zero sets for individual items) to item deltas.
 *
 * The consolidation itself is delegated to {@link IvwoConsolidator}.
 */
public class DeltaSetTripleMapConsolidation<T extends Containerable> {

    private static final Trace LOGGER = TraceManager.getTrace(DeltaSetTripleMapConsolidation.class);

    private static final String OP_CONSOLIDATE = DeltaSetTripleMapConsolidation.class.getName() + ".consolidate";

    /**
     * Item path-keyed map of output delta set triples.
     */
    private final DeltaSetTripleIvwoMap outputTripleMap;

    /**
     * Target PCV, for which deltas are to be produced.
     *
     * It contains the newest state of the object (in the light of previous computations),
     * i.e. value with `targetAPrioriDelta` already applied.
     *
     * Actually, it can be null, there's no problem in that.
     * We do not want to apply anything directly to it.
     * We need it only to know the a priori computed values, if there are any.
     */
    @Nullable private final PrismContainerValue<T> targetPcv;

    /** Provides a-priori deltas for items being consolidated. */
    private final APrioriDeltaProvider aPrioriItemDeltaProvider;

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
    private final Consumer<IvwoConsolidatorBuilder<?, ?, ?>> consolidatorCustomizer;

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
    private final ModelBeans beans = ModelBeans.get();

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
            @NotNull DeltaSetTripleIvwoMap outputTripleMap,
            @Nullable PrismContainerValue<T> targetPcv,
            @NotNull APrioriDeltaProvider aPrioriItemDeltaProvider,
            @NotNull Function<ItemPath, Boolean> itemDeltaExistsProvider,
            @Nullable Boolean addUnchangedValuesOverride,
            Consumer<IvwoConsolidatorBuilder<?, ?, ?>> consolidatorCustomizer,
            ItemDefinitionProvider itemDefinitionProvider,
            MappingEvaluationEnvironment env,
            @Nullable LensContext<?> lensContext,
            OperationResult parentResult) {
        this.outputTripleMap = outputTripleMap;
        this.targetPcv = targetPcv;
        this.aPrioriItemDeltaProvider = aPrioriItemDeltaProvider;
        this.itemDeltaExistsProvider = itemDeltaExistsProvider;
        this.itemDefinitionProvider = itemDefinitionProvider;
        this.consolidatorCustomizer = consolidatorCustomizer;
        this.env = env;
        this.parentResult = parentResult;
        this.lensContext = lensContext;
        this.addUnchangedValues = Objects.requireNonNullElseGet(addUnchangedValuesOverride, aPrioriItemDeltaProvider::isAdd);
    }

    public void computeItemDeltas() throws ExpressionEvaluationException, SchemaException,
            ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException {

        if (outputTripleMap == null || outputTripleMap.isEmpty()) {
            // Besides other reasons, this is to avoid creating empty operation results, cluttering the tracing output.
            return;
        }

        try {
            this.result = parentResult.createMinorSubresult(OP_CONSOLIDATE);
            LOGGER.trace("Computing deltas in {}", env.contextDescription);
            for (var entry: outputTripleMap.entrySet()) {
                consolidateItem(entry.getKey(), itemDefinitionProvider.getDefinition(entry.getKey()), entry.getValue());
            }
            LOGGER.trace("Computed deltas in {}:\n{}", env.contextDescription, debugDumpLazily(itemDeltas, 1));
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
            result = null;
        }
    }

    private void consolidateItem(ItemPath itemPath, ItemDefinition<?> itemDefinition, DeltaSetTriple<ItemValueWithOrigin<?, ?>> deltaSetTriple)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        ConsolidationValueMetadataComputer valueMetadataComputer;
        if (lensContext != null) {
            valueMetadataComputer = LensMetadataUtil.createValueMetadataConsolidationComputer(
                    itemPath, itemDefinition, lensContext, beans, env, result);
        } else {
            LOGGER.trace("No lens context -> no value metadata consolidation computer");
            valueMetadataComputer = null;
        }
        CollectionUtils.addIgnoreNull(
                itemDeltas,
                new DeltaSetTripleConsolidation<>(itemPath, deltaSetTriple, valueMetadataComputer)
                        .consolidateItem());
    }

    public @NotNull Collection<ItemDelta<?, ?>> getItemDeltas() {
        return itemDeltas;
    }

    private class DeltaSetTripleConsolidation<V extends PrismValue, D extends ItemDefinition<?>, I extends ItemValueWithOrigin<V,D>> {

        /**
         * Path to item that is to be consolidated.
         */
        @NotNull private final ItemPath itemPath;

        /**
         * Delta set triple that is to be consolidated.
         */
        private final DeltaSetTriple<ItemValueWithOrigin<?, ?>> deltaSetTriple;

        /**
         * Existing (apriori) delta that was specified by the caller and/or computed upstream.
         */
        @Nullable private final ItemDelta<V, D> aprioriItemDelta;

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

        private DeltaSetTripleConsolidation(
                @NotNull ItemPath itemPath,
                DeltaSetTriple<ItemValueWithOrigin<?, ?>> deltaSetTriple,
                ConsolidationValueMetadataComputer valueMetadataComputer) throws SchemaException {
            this.itemPath = itemPath;
            this.deltaSetTriple = deltaSetTriple;
            //noinspection unchecked
            this.aprioriItemDelta = (ItemDelta<V, D>) aPrioriItemDeltaProvider.apply(itemPath);
            this.existingItem = targetPcv != null ? targetPcv.findItem(itemPath) : null;
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

        @SuppressWarnings("unchecked")
        private void computeItemDelta() throws ExpressionEvaluationException, SchemaException,
                ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException {
            try (IvwoConsolidator<V, D, I> consolidator = new IvwoConsolidatorBuilder<V, D, I>()
                    .itemPath(itemPath)
                    .ivwoTriple((DeltaSetTriple<I>) deltaSetTriple)
                    .itemDefinition(itemDefinition)
                    .aprioriItemDelta(aprioriItemDelta)
                    .itemDeltaExists(itemDeltaExistsProvider.apply(itemPath))
                    .existingItem(existingItem)
                    .equalsChecker(null)
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

    /**
     * A priori delta is the one that led to the current state of the target object. This object provides a priori deltas
     * for items that are being consolidated.
     */
    public interface APrioriDeltaProvider extends Function<ItemPath, ItemDelta<?, ?>> {

        static @NotNull APrioriDeltaProvider forDelta(ObjectDelta<?> delta) {
            if (delta != null) {
                return new ForDelta(delta);
            } else {
                return none();
            }
        }

        //FIXME the original code was: ItemDelta<?, ?> aprioriItemDelta = LensUtil.getAprioriItemDelta(aPrioriItemDeltaProvider, itemPath);

        /** Was the whole object added? */
        boolean isAdd();

        static APrioriDeltaProvider none() {
            return None.INSTANCE;
        }

        /** Represents the situation when there's no a priori delta. */
        class None implements APrioriDeltaProvider {

            private static final None INSTANCE = new None();

            @Override
            public boolean isAdd() {
                return false;
            }

            @Override
            public ItemDelta<?, ?> apply(ItemPath itemPath) {
                return null;
            }
        }

        class ForDelta implements APrioriDeltaProvider {

            @NotNull private final ObjectDelta<?> delta;

            ForDelta(@NotNull ObjectDelta<?> delta) {
                this.delta = delta;
            }

            @Override
            public boolean isAdd() {
                return delta.isAdd();
            }

            @Override
            public ItemDelta<?, ?> apply(ItemPath itemPath) {
                return delta.findItemDelta(itemPath);
            }
        }
    }
}
