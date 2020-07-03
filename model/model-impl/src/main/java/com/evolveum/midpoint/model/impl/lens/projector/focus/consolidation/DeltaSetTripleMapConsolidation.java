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

import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;

import org.apache.commons.collections4.CollectionUtils;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.projector.focus.ObjectTemplateProcessor;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateItemDefinitionType;

/**
 * Responsible for consolidation of delta set triple map (plus, minus, zero sets for individual items) to item deltas.
 */
@Experimental
public class DeltaSetTripleMapConsolidation<T extends AssignmentHolderType> {

    // The logger name is intentionally different because of the backward compatibility.
    private static final Trace LOGGER = TraceManager.getTrace(ObjectTemplateProcessor.class);

    /**
     * Item path-keyed map of output delta set triples.
     */
    private final Map<UniformItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> outputTripleMap;

    /**
     * Item path-keyed map of item definitions (taken from object template).
     */
    private final Map<UniformItemPath, ObjectTemplateItemDefinitionType> itemDefinitionsMap;

    /**
     * Target object, for which deltas are to be produced.
     * It contains the _current_ (latest) state of the object (in the light of previous computations),
     * i.e. object with targetAPrioriDelta already applied.
     */
    final PrismObject<T> targetObject;

    /**
     * Delta that lead to the current state of the target object.
     */
    private final ObjectDelta<T> targetAPrioriDelta;

    /**
     * Definition of the target object.
     */
    private final PrismObjectDefinition<T> targetDefinition;

    /**
     * Context description.
     */
    final String contextDescription;

    /**
     * Useful beans.
     */
    final ModelBeans beans;

    /**
     * Should the values from zero set be transformed to delta ADD section?
     * This is the case when the whole object is being added.
     */
    final boolean addUnchangedValues;

    /**
     * Result of the computation: the item deltas.
     */
    private final Collection<ItemDelta<?,?>> itemDeltas = new ArrayList<>();

    public DeltaSetTripleMapConsolidation(Map<UniformItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> outputTripleMap,
            Map<UniformItemPath, ObjectTemplateItemDefinitionType> itemDefinitionsMap, PrismObject<T> targetObject,
            ObjectDelta<T> targetAPrioriDelta, PrismObjectDefinition<T> targetDefinition, String contextDescription,
            ModelBeans beans) {
        this.outputTripleMap = outputTripleMap;
        this.itemDefinitionsMap = itemDefinitionsMap;
        this.targetObject = targetObject;
        this.targetAPrioriDelta = targetAPrioriDelta;
        this.targetDefinition = targetDefinition;
        this.contextDescription = contextDescription;
        this.beans = beans;

        this.addUnchangedValues = targetAPrioriDelta != null && targetAPrioriDelta.isAdd();
    }

    public void computeItemDeltas() throws ExpressionEvaluationException, PolicyViolationException, SchemaException {
        LOGGER.trace("Computing deltas in {}, a priori delta:\n{}", contextDescription, debugDumpLazily(targetAPrioriDelta));

        for (Map.Entry<UniformItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?,?>>> entry: outputTripleMap.entrySet()) {
            UniformItemPath itemPath = entry.getKey();
            ItemDelta aprioriItemDelta = LensUtil.getAprioriItemDelta(targetAPrioriDelta, itemPath);
            DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>> deltaSetTriple = entry.getValue();

            DeltaSetTripleConsolidation<T> itemConsolidation =
                    new DeltaSetTripleConsolidation<>(this, itemPath,
                            targetDefinition.findItemDefinition(itemPath),
                            getTemplateItemDefinition(itemPath), aprioriItemDelta, deltaSetTriple);
            CollectionUtils.addIgnoreNull(itemDeltas, itemConsolidation.consolidateItem());
        }
    }

    private ObjectTemplateItemDefinitionType getTemplateItemDefinition(UniformItemPath itemPath) {
        if (itemDefinitionsMap != null) {
            return ItemPathCollectionsUtil.getFromMap(itemDefinitionsMap, itemPath);
        } else {
            return null;
        }
    }

    public Collection<ItemDelta<?, ?>> getItemDeltas() {
        return itemDeltas;
    }
}
