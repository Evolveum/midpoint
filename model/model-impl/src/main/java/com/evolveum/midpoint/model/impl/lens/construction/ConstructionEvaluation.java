/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType.SKIP;

import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;

import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.NextRecompute;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * State of a construction evaluation. Consists of evaluations of individual attributes and associations.
 *
 * Intentionally not a public class.
 */
class ConstructionEvaluation<AH extends AssignmentHolderType, ROC extends ResourceObjectConstruction<AH, ?>> {

    /**
     * Reference to the parent (evaluated construction).
     */
    @NotNull final EvaluatedResourceObjectConstructionImpl<AH, ROC> evaluatedConstruction;

    /**
     * Reference to the grandparent (construction itself).
     */
    @NotNull final ROC construction;

    /**
     * Projection context - it might not be known for assigned constructions where the respective shadows
     * was not linked to the focus at the evaluation time.
     */
    @Nullable final LensProjectionContext projectionContext;

    /**
     * The task.
     */
    @NotNull final Task task;

    /**
     * The result. Everything is covered on single level of operation result - no subresults here.
     */
    @NotNull final OperationResult result;

    /**
     * Simple name describing the resource object operation: add, delete, modify (or null if not known).
     */
    @Nullable final String operation;

    /**
     * Loaded resource object, if/when needed and if possible.
     */
    @Nullable private ObjectDeltaObject<ShadowType> projectionOdo;

    /**
     * The "next recompute" information: updated gradually as individual mappings are evaluated.
     */
    private NextRecompute nextRecompute;

    /**
     * Was this evaluation already done? To avoid repeated runs.
     */
    private boolean evaluated;

    ConstructionEvaluation(
            @NotNull EvaluatedResourceObjectConstructionImpl<AH, ROC> evaluatedConstruction,
            @NotNull Task task, @NotNull OperationResult result) {
        this.evaluatedConstruction = evaluatedConstruction;
        this.construction = evaluatedConstruction.getConstruction();
        this.projectionContext = evaluatedConstruction.getProjectionContext();
        this.task = task;
        this.result = result;

        this.operation = projectionContext != null ? projectionContext.getOperation().getValue() : null;
    }

    public void evaluate() throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        checkNotEvaluatedTwice();

        projectionOdo = projectionContext != null ? projectionContext.getObjectDeltaObject() : null;

        if (isOutboundAllowed()) {
            evaluateAttributes();
            evaluateAssociations();
        }
    }

    private boolean isOutboundAllowed() {
        return projectionContext == null
                || projectionContext.getLensContext().getPartialProcessingOptions().getOutbound() != SKIP;
    }

    private void checkNotEvaluatedTwice() {
        if (evaluated) {
            throw new IllegalStateException();
        }
        evaluated = true;
    }

    private void evaluateAttributes()
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, CommunicationException {

        for (var attributeMapper : evaluatedConstruction.getAttributeMappers(this)) {
            if (attributeMapper.isEnabled()) {
                evaluatedConstruction.addAttributeTripleProducer(
                        attributeMapper.evaluate());
                updateNextRecompute(attributeMapper);
            }
        }
    }

    private void evaluateAssociations()
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, CommunicationException {

        for (var associationMapper : evaluatedConstruction.getAssociationMappers(this)) {
            if (associationMapper.isEnabled()) {
                //noinspection unchecked
                evaluatedConstruction.addAssociationTripleProducer(
                        (PrismValueDeltaSetTripleProducer<ShadowAssociationValue, ShadowAssociationDefinition>)
                                associationMapper.evaluate());
                updateNextRecompute(associationMapper);
            }
        }
    }

    void loadFullShadowIfNeeded(ShadowItemMapper<?, ?, ?> itemMapper)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        String loadReason = evaluatedConstruction.getFullShadowLoadReason(itemMapper);
        if (loadReason != null) {
            projectionOdo = evaluatedConstruction.loadFullShadow(loadReason, task, result);
        }
    }

    private void updateNextRecompute(ShadowItemMapper<?, ?, ?> itemMapper) {
        if (itemMapper.getTripleProducer() instanceof MappingImpl<?, ?> mapping) {
            nextRecompute = NextRecompute.update(mapping, nextRecompute);
        }
    }

    NextRecompute getNextRecompute() {
        return nextRecompute;
    }

    @Nullable ObjectDeltaObject<ShadowType> getProjectionOdo() {
        return projectionOdo;
    }
}
