/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * Attributes (to be) evaluated.
     */
    @NotNull private final List<AttributeEvaluation<AH>> attributeEvaluations = new ArrayList<>();

    /**
     * Associations (to be) evaluated.
     */
    @NotNull private final List<AssociationEvaluation<AH>> associationEvaluations = new ArrayList<>();

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
     * Loaded resource object, if needed and if possible.
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

    public ConstructionEvaluation(@NotNull EvaluatedResourceObjectConstructionImpl<AH, ROC> evaluatedConstruction,
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
        attributeEvaluations.addAll(evaluatedConstruction.collectAttributesToEvaluate(this));
        associationEvaluations.addAll(evaluatedConstruction.collectAssociationsToEvaluate(this));

        projectionOdo = projectionContext != null ? projectionContext.getObjectDeltaObject() : null;

        evaluateAttributes();
        evaluateAssociations();
    }

    private void checkNotEvaluatedTwice() {
        if (evaluated) {
            throw new IllegalStateException();
        }
        evaluated = true;
    }

    protected void evaluateAttributes() throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, CommunicationException {

        for (AttributeEvaluation<AH> attributeEvaluation : attributeEvaluations) {
            loadFullShadowIfNeeded(attributeEvaluation);

            attributeEvaluation.evaluate();
            if (attributeEvaluation.hasEvaluatedMapping()) {
                evaluatedConstruction.addAttributeMapping(attributeEvaluation.getEvaluatedMapping());
                updateNextRecompute(attributeEvaluation);
            }
        }
    }

    protected void evaluateAssociations() throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, CommunicationException {

        for (AssociationEvaluation<AH> associationEvaluation : associationEvaluations) {
            loadFullShadowIfNeeded(associationEvaluation);

            associationEvaluation.evaluate();
            if (associationEvaluation.hasEvaluatedMapping()) {
                evaluatedConstruction.addAssociationMapping(associationEvaluation.getEvaluatedMapping());
                updateNextRecompute(associationEvaluation);
            }
        }
    }

    private void loadFullShadowIfNeeded(ItemEvaluation itemEvaluation) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        String loadReason = evaluatedConstruction.getFullShadowLoadReason(projectionContext, itemEvaluation.getMappingBean());
        if (loadReason != null) {
            projectionOdo = evaluatedConstruction.loadFullShadow(projectionContext, loadReason, task, result);
        }
    }

    private void updateNextRecompute(ItemEvaluation itemEvaluation) {
        nextRecompute = NextRecompute.update(itemEvaluation.getEvaluatedMapping(), nextRecompute);
    }

    public NextRecompute getNextRecompute() {
        return nextRecompute;
    }

    public @Nullable ObjectDeltaObject<ShadowType> getProjectionOdo() {
        return projectionOdo;
    }
}
