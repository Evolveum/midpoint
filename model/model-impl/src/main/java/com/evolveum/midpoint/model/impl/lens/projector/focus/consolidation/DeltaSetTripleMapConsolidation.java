/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation;

import static com.evolveum.midpoint.util.DebugUtil.debugDumpLazily;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataMappingScopeType.CONSOLIDATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.evolveum.midpoint.model.impl.lens.LensContext;

import org.apache.commons.collections4.CollectionUtils;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.common.mapping.metadata.ValueMetadataComputation;
import com.evolveum.midpoint.model.common.mapping.metadata.ValueMetadataProcessingSpec;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.focus.ObjectTemplateProcessor;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.repo.common.expression.ValueMetadataComputer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

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
     * Target object, for which deltas are to be produced.
     * It contains the _current_ (latest) state of the object (in the light of previous computations),
     * i.e. object with targetAPrioriDelta already applied.
     */
    private final PrismObject<T> targetObject;

    /**
     * Delta that lead to the current state of the target object.
     */
    private final ObjectDelta<T> targetAPrioriDelta;

    /**
     * Definition of the target object.
     */
    private final PrismObjectDefinition<T> targetDefinition;

    /**
     * Should the values from zero set be transformed to delta ADD section?
     * This is the case when the whole object is being added.
     */
    private final boolean addUnchangedValues;

    /**
     * Mapping evaluation environment (context description, now, task).
     */
    private final MappingEvaluationEnvironment env;

    /**
     * Operation result (currently needed for value metadata computation).
     */
    private final OperationResult result;

    /**
     * Useful beans.
     */
    private final ModelBeans beans;

    /**
     * Lens context used to determine metadata handling.
     * (Or should we use object template instead?)
     */
    private final LensContext<?> lensContext;

    /**
     * Result of the computation: the item deltas.
     */
    private final Collection<ItemDelta<?,?>> itemDeltas = new ArrayList<>();

    public DeltaSetTripleMapConsolidation(Map<UniformItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> outputTripleMap,
            PrismObject<T> targetObject, ObjectDelta<T> targetAPrioriDelta, PrismObjectDefinition<T> targetDefinition,
            MappingEvaluationEnvironment env, ModelBeans beans, LensContext<?> lensContext, OperationResult result) {
        this.outputTripleMap = outputTripleMap;
        this.targetObject = targetObject;
        this.targetAPrioriDelta = targetAPrioriDelta;
        this.targetDefinition = targetDefinition;
        this.env = env;
        this.result = result;
        this.beans = beans;
        this.lensContext = lensContext;

        this.addUnchangedValues = targetAPrioriDelta != null && targetAPrioriDelta.isAdd();
    }

    public void computeItemDeltas() throws ExpressionEvaluationException, PolicyViolationException, SchemaException,
            ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException {
        LOGGER.trace("Computing deltas in {}, a priori delta:\n{}", env.contextDescription, debugDumpLazily(targetAPrioriDelta));

        for (Map.Entry<UniformItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?,?>>> entry: outputTripleMap.entrySet()) {
            UniformItemPath itemPath = entry.getKey();
            ItemDelta aprioriItemDelta = LensUtil.getAprioriItemDelta(targetAPrioriDelta, itemPath);
            DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>> deltaSetTriple = entry.getValue();

            ValueMetadataComputer valueMetadataComputer = createValueMetadataComputer(itemPath);

            //noinspection unchecked
            DeltaSetTripleConsolidation itemConsolidation =
                    new DeltaSetTripleConsolidation(itemPath,
                            deltaSetTriple, aprioriItemDelta, targetObject, targetDefinition.findItemDefinition(itemPath),
                            addUnchangedValues, valueMetadataComputer, env, result);
            CollectionUtils.addIgnoreNull(itemDeltas, itemConsolidation.consolidateItem());
        }
    }

    private ValueMetadataComputer createValueMetadataComputer(ItemPath itemPath) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        ValueMetadataProcessingSpec processingSpec = createProcessingSpec(itemPath);
        if (processingSpec.isEmpty()) {
            return null;
        } else {
            return (inputValues, computationOpResult) ->
                    ValueMetadataComputation
                            .forConsolidation(inputValues, processingSpec, beans.commonBeans, env, computationOpResult)
                            .execute();
        }
    }

    private ValueMetadataProcessingSpec createProcessingSpec(ItemPath itemPath) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        ValueMetadataProcessingSpec processingSpec = ValueMetadataProcessingSpec.forScope(CONSOLIDATION);
        // TODO What about persona mappings? outbound mappings? We should not use object template for that.
        processingSpec.populateFromCurrentFocusTemplate(lensContext, itemPath, beans.modelObjectResolver,
                env.contextDescription, env.task, result);
        return processingSpec;
    }

    public Collection<ItemDelta<?, ?>> getItemDeltas() {
        return itemDeltas;
    }
}
