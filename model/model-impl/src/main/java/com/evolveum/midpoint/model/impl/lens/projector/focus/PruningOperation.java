/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedExclusionTrigger;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PrunePolicyActionType;

import java.util.Collection;

public class PruningOperation<F extends AssignmentHolderType> {

    private static final Trace LOGGER = TraceManager.getTrace(PruningOperation.class);

    private static final String OP_EXECUTE = PruningOperation.class.getName() + ".execute";

    private final LensContext<F> context;
    private final DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple;
    private final ModelBeans beans;

    /**
     * Was at least one assignment pruned (using secondary delta)?
     *
     * If yes, we should re-evaluate the assignments.
     */
    private boolean prunedViaSecondaryDelta;

    /**
     * Was "enforcement override" trigger generated?
     *
     * If yes, we should *NOT* re-evaluate the assignments because we will signal violation exception later anyway.
     * Moreover, it is crucial that we will not prune conflicting assignments away, because it would mean that
     * these enforcement triggers would go away with them.
     */
    private boolean enforcementOverrideGenerated;

    public PruningOperation(LensContext<F> context, DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
            ModelBeans beans) {
        this.context = context;
        this.evaluatedAssignmentTriple = evaluatedAssignmentTriple;
        this.beans = beans;
    }

    /**
     * @return true if the assignments should be re-evaluated
     */
    public boolean execute(OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createMinorSubresult(OP_EXECUTE);
        try {
            for (EvaluatedAssignmentImpl<F> existingOrNewAssignment : evaluatedAssignmentTriple.getNonNegativeValues()) { // MID-6403
                // Note that we take assignments on "being added" condition i.e. ones which are added since objectOld.
                // Taking simple "plus set" is not sufficient because of situations after wave 0 when all assignments
                // look like being in zero set.
                if (existingOrNewAssignment.getOrigin().isBeingAdded()) {
                    pruneNewAssignment(existingOrNewAssignment);
                }
            }

            return prunedViaSecondaryDelta && !enforcementOverrideGenerated;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void pruneNewAssignment(EvaluatedAssignmentImpl<F> newAssignment) {
        LOGGER.trace("Checking for pruning of new assignment: {}", newAssignment);
        for (EvaluatedPolicyRuleImpl newAssignmentRule : newAssignment.getAllTargetsPolicyRules()) {
            if (newAssignmentRule.containsEnabledAction(PrunePolicyActionType.class)) {
                Collection<EvaluatedExclusionTrigger> exclusionTriggers =
                        newAssignmentRule.getAllTriggers(EvaluatedExclusionTrigger.class);
                LOGGER.trace("Exclusion triggers: {}", exclusionTriggers);
                for (EvaluatedExclusionTrigger exclusionTrigger : exclusionTriggers) {
                    processPruneRuleExclusionTrigger(newAssignment, newAssignmentRule, exclusionTrigger);
                }
            }
        }
    }

    private void processPruneRuleExclusionTrigger(EvaluatedAssignmentImpl<F> newAssignment, EvaluatedPolicyRuleImpl pruneRule,
            EvaluatedExclusionTrigger exclusionTrigger) {
        EvaluatedAssignment conflictingAssignment = exclusionTrigger.getConflictingAssignment();
        if (conflictingAssignment == null) {
            throw new SystemException("Added assignment " + newAssignment
                    + ", the exclusion prune rule was triggered but there is no conflicting assignment in the trigger");
        }
        LOGGER.debug("Pruning assignment {} because it conflicts with added assignment {}", conflictingAssignment, newAssignment);
        if (conflictingAssignment.isPresentInOldObject()) {
            // This is the usual (good) case. The conflicting assignment was present in the old object so we can remove it
            // by means of secondary delta.

            //noinspection unchecked
            PrismContainerValue<AssignmentType> assignmentValueToRemove = conflictingAssignment.getAssignment()
                    .asPrismContainerValue().clone();
            PrismObjectDefinition<F> focusDef = context.getFocusContext().getObjectDefinition();
            ContainerDelta<AssignmentType> assignmentDelta = beans.prismContext.deltaFactory().container()
                    .createDelta(FocusType.F_ASSIGNMENT, focusDef);
            //noinspection unchecked
            assignmentDelta.addValuesToDelete(assignmentValueToRemove);
            context.getFocusContext().swallowToSecondaryDeltaUnchecked(assignmentDelta);
            prunedViaSecondaryDelta = true;
        } else {
            // Conflicting assignment was not present in old object i.e. it was added in the meanwhile into secondary delta.
            // We create trigger for this with enforcementOverride = true, so it will be reported as policy violation
            // even if not enforcement policy action is present. See also MID-4766.
            SingleLocalizableMessage message = new LocalizableMessageBuilder()
                    .key("PolicyViolationException.message.prunedRolesAssigned")
                    .arg(ObjectTypeUtil.createDisplayInformation(newAssignment.getTarget(), false))
                    .arg(ObjectTypeUtil.createDisplayInformation(conflictingAssignment.getTarget(), false))
                    .build();
            pruneRule.addTrigger(
                    new EvaluatedExclusionTrigger(exclusionTrigger.getConstraint(),
                            message, null, exclusionTrigger.getConflictingAssignment(),
                            exclusionTrigger.getConflictingTarget(), exclusionTrigger.getConflictingPath(), true)
            );
            enforcementOverrideGenerated = true;
        }
    }
}
