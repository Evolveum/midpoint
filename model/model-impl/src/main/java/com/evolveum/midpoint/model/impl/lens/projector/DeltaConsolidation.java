/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ValueMetadataComputer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Consolidates delta against current object.
 */
class DeltaConsolidation<F extends ObjectType>  {

    @NotNull private final LensContext<F> context;
    @NotNull private final PrismObject<F> currentObject;
    @NotNull private final Collection<? extends ItemDelta<?, ?>> itemDeltas;
    @NotNull private final List<ItemDelta<?, ?>> consolidatedItemDeltas = new ArrayList<>();
    @NotNull private final MappingEvaluationEnvironment env;
    @NotNull private final OperationResult result;
    @NotNull private final ModelBeans beans;
    @NotNull private final DeltaSetTriple<ItemValueWithOrigin<?, ?>> emptyTriple;

    DeltaConsolidation(@NotNull LensContext<F> context, @NotNull PrismObject<F> currentObject,
            @NotNull Collection<? extends ItemDelta<?,?>> itemDeltas,
            @NotNull MappingEvaluationEnvironment env, @NotNull OperationResult result, @NotNull ModelBeans beans) {
        this.context = context;
        this.currentObject = currentObject;
        this.itemDeltas = itemDeltas;
        this.env = env;
        this.result = result;
        this.beans = beans;
        this.emptyTriple = beans.prismContext.deltaFactory().createDeltaSetTriple();
    }

    ObjectDelta<F> consolidate() throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        for (ItemDelta<?, ?> itemDelta : itemDeltas) {
            new ItemDeltaConsolidation<>(itemDelta).consolidate();
        }
        return beans.prismContext.deltaFactory().object().createModifyDelta(
                currentObject.getOid(), consolidatedItemDeltas, currentObject.getCompileTimeClass());
    }

    private class ItemDeltaConsolidation<V extends PrismValue, D extends ItemDefinition> {

        @NotNull private final ItemDelta<V, D> itemDelta;

        private ItemDeltaConsolidation(@NotNull ItemDelta<V, D> itemDelta) {
            this.itemDelta = itemDelta;
        }

        private void consolidate() throws CommunicationException, ObjectNotFoundException, SchemaException,
                SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

            ItemPath itemPath = itemDelta.getPath();

            ValueMetadataComputer computer =
                    LensMetadataUtil.createValueMetadataConsolidationComputer(itemPath, context, beans, env, result);

            //noinspection unchecked
            DeltaSetTriple<ItemValueWithOrigin<V, D>> typedEmptyTriple = (DeltaSetTriple) emptyTriple; // hack to make Java happy

            try (IvwoConsolidator<V, D, ItemValueWithOrigin<V, D>> consolidator =
                    new IvwoConsolidatorBuilder<V, D, ItemValueWithOrigin<V, D>>()
                            .itemPath(itemPath)
                            .ivwoTriple(typedEmptyTriple)
                            .itemDefinition(itemDelta.getDefinition())
                            .aprioriItemDelta(itemDelta)
                            .itemContainer(currentObject)
                            .valueMatcher(null)
                            .comparator(null)
                            .existingItemKnown(true)
                            .contextDescription(env.contextDescription)
                            .strengthSelector(StrengthSelector.ALL)
                            .valueMetadataComputer(computer)
                            .result(result)
                            .build()) {

                ItemDelta<V, D> consolidatedItemDelta = consolidator.consolidateAPrioriDelta();
                if (!ItemDelta.isEmpty(consolidatedItemDelta)) {
                    consolidatedItemDeltas.add(consolidatedItemDelta);
                }
            }
        }
    }
}
