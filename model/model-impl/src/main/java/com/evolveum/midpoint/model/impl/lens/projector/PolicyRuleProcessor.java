/**
 * Copyright (c) 2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.*;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentTargetImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 *
 */
@Component
public class PolicyRuleProcessor {
	
	private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleProcessor.class);
	
	@Autowired
    private PrismContext prismContext;
	
	@Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;
	
	/**
	 * Evaluate the policies (policy rules, but also the legacy policies). Trigger the rules.
	 * But do not enforce anything and do not make any context changes.
	 */
	public <F extends FocusType> void processPolicies(LensContext<F> context,
			DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
			OperationResult result) throws PolicyViolationException, SchemaException {
		checkAssignmentRules(context, evaluatedAssignmentTriple, result);
        checkExclusionsLegacy(context, evaluatedAssignmentTriple.getPlusSet(), evaluatedAssignmentTriple.getNonNegativeValues());
        // in policy based situations, the comparison is not symmetric
        checkExclusionsRuleBased(context, evaluatedAssignmentTriple.getPlusSet(), evaluatedAssignmentTriple.getPlusSet());
        checkExclusionsRuleBased(context, evaluatedAssignmentTriple.getPlusSet(), evaluatedAssignmentTriple.getZeroSet());
        checkExclusionsRuleBased(context, evaluatedAssignmentTriple.getZeroSet(), evaluatedAssignmentTriple.getPlusSet());
        checkAssigneeConstraints(context, evaluatedAssignmentTriple, result);
        checkSecondaryConstraints(context, evaluatedAssignmentTriple, result);
	}

	private <F extends FocusType> void checkAssignmentRules(LensContext<F> context,
			DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
			OperationResult result) throws PolicyViolationException, SchemaException {
		checkAssignmentRules(context, evaluatedAssignmentTriple.getPlusSet(), PlusMinusZero.PLUS, result);
		checkAssignmentRules(context, evaluatedAssignmentTriple.getMinusSet(), PlusMinusZero.MINUS, result);
	}

	private <F extends FocusType> void checkAssignmentRules(LensContext<F> context,
			Collection<EvaluatedAssignmentImpl<F>> evaluatedAssignmentSet, PlusMinusZero whichSet,
			OperationResult result) throws PolicyViolationException, SchemaException {
		for (EvaluatedAssignmentImpl<F> evaluatedAssignment: evaluatedAssignmentSet) {
			Collection<EvaluatedPolicyRule> policyRules = evaluatedAssignment.getThisTargetPolicyRules();
			for (EvaluatedPolicyRule policyRule: policyRules) {
				PolicyConstraintsType policyConstraints = policyRule.getPolicyConstraints();
				if (policyConstraints == null) {
					continue;
				}
				for (AssignmentPolicyConstraintType assignmentConstraint: policyConstraints.getAssignment()) {
					if (matchesOperation(assignmentConstraint, whichSet)) {
						List<QName> relationsToCheck = assignmentConstraint.getRelation().isEmpty() ?
								Collections.singletonList(null) : assignmentConstraint.getRelation();
						for (QName constraintRelation : relationsToCheck) {
							if (MiscSchemaUtil.compareRelation(constraintRelation, evaluatedAssignment.getRelation())) {
								EvaluatedPolicyRuleTrigger trigger = new EvaluatedPolicyRuleTrigger<>(
										PolicyConstraintKindType.ASSIGNMENT,
										assignmentConstraint, "Assignment of " + evaluatedAssignment.getTarget());
								evaluatedAssignment.triggerConstraint(policyRule, trigger);
							}
						}
					}
				}
			}
		}
	}

	private boolean matchesOperation(AssignmentPolicyConstraintType constraint, PlusMinusZero whichSet) {
		List<ModificationTypeType> operations = constraint.getOperation();
		if (operations.isEmpty()) {
			return true;
		}
		switch (whichSet) {
			case PLUS: return operations.contains(ModificationTypeType.ADD);
			case MINUS: return operations.contains(ModificationTypeType.DELETE);
			case ZERO: return operations.contains(ModificationTypeType.REPLACE);
			default: throw new IllegalArgumentException("whichSet: " + whichSet);
		}
	}

	private <F extends FocusType> void checkExclusionsLegacy(LensContext<F> context, Collection<EvaluatedAssignmentImpl<F>> assignmentsA,
			Collection<EvaluatedAssignmentImpl<F>> assignmentsB) throws PolicyViolationException {
		for (EvaluatedAssignmentImpl<F> assignmentA: assignmentsA) {
			for (EvaluatedAssignmentImpl<F> assignmentB: assignmentsB) {
				if (assignmentA == assignmentB) {
					continue;	// Same thing, this cannot exclude itself
				}
				for (EvaluatedAssignmentTargetImpl eRoleA : assignmentA.getRoles().getAllValues()) {
					if (eRoleA.appliesToFocus()) {
						for (EvaluatedAssignmentTargetImpl eRoleB : assignmentB.getRoles().getAllValues()) {
							if (eRoleB.appliesToFocus()) {
								checkExclusionLegacy(assignmentA, assignmentB, eRoleA, eRoleB);
							}
						}
					}
				}
			}
		}
	}

	private <F extends FocusType> void checkExclusionLegacy(EvaluatedAssignmentImpl<F> assignmentA, EvaluatedAssignmentImpl<F> assignmentB,
			EvaluatedAssignmentTargetImpl roleA, EvaluatedAssignmentTargetImpl roleB) throws PolicyViolationException {
		checkExclusionOneWayLegacy(assignmentA, assignmentB, roleA, roleB);
		checkExclusionOneWayLegacy(assignmentB, assignmentA, roleB, roleA);
	}

	private <F extends FocusType> void checkExclusionOneWayLegacy(EvaluatedAssignmentImpl<F> assignmentA, EvaluatedAssignmentImpl<F> assignmentB,
			EvaluatedAssignmentTargetImpl roleA, EvaluatedAssignmentTargetImpl roleB) throws PolicyViolationException {
		for (ExclusionPolicyConstraintType exclusionA : roleA.getExclusions()) {
			checkAndTriggerExclusionConstraintViolationLegacy(assignmentA, assignmentB, roleA, roleB, exclusionA);
		}
	}

	private <F extends FocusType> void checkAndTriggerExclusionConstraintViolationLegacy(EvaluatedAssignmentImpl<F> assignmentA,
			@NotNull EvaluatedAssignmentImpl<F> assignmentB, EvaluatedAssignmentTargetImpl roleA, EvaluatedAssignmentTargetImpl roleB,
			ExclusionPolicyConstraintType constraint)
			throws PolicyViolationException {
		ObjectReferenceType targetRef = constraint.getTargetRef();
		if (roleB.getOid().equals(targetRef.getOid())) {
			EvaluatedExclusionTrigger trigger = new EvaluatedExclusionTrigger(
					constraint, "Violation of SoD policy: " + roleA.getTarget() + " excludes " + roleB.getTarget() +
					", they cannot be assigned at the same time", assignmentB,
					roleA.getTarget() != null ? roleA.getTarget().asObjectable() : null,
					roleB.getTarget() != null ? roleB.getTarget().asObjectable() : null,
					roleA.getAssignmentPath(), roleB.getAssignmentPath());
			assignmentA.triggerConstraint(null, trigger);
		}
	}

	private <F extends FocusType> void checkExclusionsRuleBased(LensContext<F> context,
			Collection<EvaluatedAssignmentImpl<F>> assignmentsA, Collection<EvaluatedAssignmentImpl<F>> assignmentsB)
			throws PolicyViolationException {
		for (EvaluatedAssignmentImpl<F> assignmentA : assignmentsA) {
			checkExclusionsRuleBased(context, assignmentA, assignmentsB);
		}
	}

	private <F extends FocusType> void checkExclusionsRuleBased(LensContext<F> context, EvaluatedAssignmentImpl<F> assignmentA,
			Collection<EvaluatedAssignmentImpl<F>> assignmentsB) throws PolicyViolationException {

		// We consider all policy rules, i.e. also from induced targets. (It is not possible to collect local
		// rules for individual targets in the chain - rules are computed only for directly evaluated assignments.)
		for (EvaluatedPolicyRule policyRule : assignmentA.getTargetPolicyRules()) {
			if (policyRule.getPolicyConstraints() == null || policyRule.getPolicyConstraints().getExclusion().isEmpty()) {
				continue;
			}
			// In order to avoid false positives, we consider all targets from the current assignment as "allowed"
			Set<String> allowedTargetOids = assignmentA.getNonNegativeTargets().stream()
					.filter(t -> t.appliesToFocus())
					.map(t -> t.getOid())
					.collect(Collectors.toSet());

			for (EvaluatedAssignmentImpl<F> assignmentB : assignmentsB) {
				for (EvaluatedAssignmentTargetImpl targetB : assignmentB.getNonNegativeTargets()) {
					if (!targetB.appliesToFocus() || allowedTargetOids.contains(targetB.getOid())) {
						continue;
					}
					for (ExclusionPolicyConstraintType exclusionConstraint : policyRule.getPolicyConstraints().getExclusion()) {
						if (excludes(exclusionConstraint, targetB)) {
							triggerExclusionConstraintViolation(assignmentA, assignmentB, targetB, exclusionConstraint, policyRule);
						}
					}
				}
			}
		}
	}

	private boolean excludes(ExclusionPolicyConstraintType constraint, EvaluatedAssignmentTargetImpl target) {
		if (constraint.getTargetRef() == null || target.getOid() == null) {
			return false;		// shouldn't occur
		} else {
			return target.getOid().equals(constraint.getTargetRef().getOid());
		}
		// We could speculate about resolving targets at runtime, but that's inefficient. More appropriate is
		// to specify an expression, and evaluate (already resolved) target with regards to this expression.
	}

	private <F extends FocusType> void triggerExclusionConstraintViolation(EvaluatedAssignmentImpl<F> assignmentA,
			@NotNull EvaluatedAssignmentImpl<F> assignmentB, EvaluatedAssignmentTargetImpl targetB,
			ExclusionPolicyConstraintType constraint, EvaluatedPolicyRule policyRule)
			throws PolicyViolationException {

		AssignmentPath pathA = policyRule.getAssignmentPath();
		AssignmentPath pathB = targetB.getAssignmentPath();
		String infoA = computeAssignmentInfo(pathA, assignmentA.getTarget());
		String infoB = computeAssignmentInfo(pathB, targetB.getTarget());
		ObjectType objectA = getConflictingObject(pathA, assignmentA.getTarget());
		ObjectType objectB = getConflictingObject(pathB, targetB.getTarget());
		EvaluatedExclusionTrigger trigger = new EvaluatedExclusionTrigger(
				constraint, "Violation of SoD policy: " + infoA + " excludes " + infoB +
				", they cannot be assigned at the same time", assignmentB, objectA, objectB, pathA, pathB);
		assignmentA.triggerConstraint(policyRule, trigger);
	}

	private ObjectType getConflictingObject(AssignmentPath path, PrismObject<?> defaultObject) {
		if (path == null) {
			return ObjectTypeUtil.toObjectable(defaultObject);
		}
		List<ObjectType> objects = path.getFirstOrderChain();
		return objects.isEmpty() ?
				ObjectTypeUtil.toObjectable(defaultObject) : objects.get(objects.size()-1);
	}

	private String computeAssignmentInfo(AssignmentPath path, PrismObject<?> defaultObject) {
		if (path == null) {
			return String.valueOf(defaultObject);	// shouldn't occur
		}
		List<ObjectType> objects = path.getFirstOrderChain();
		if (objects.isEmpty()) {		// shouldn't occur
			return String.valueOf(defaultObject);
		}
		ObjectType last = objects.get(objects.size()-1);
		StringBuilder sb = new StringBuilder();
		sb.append(last);
		if (objects.size() > 1) {
			sb.append(objects.stream()
					.map(o -> PolyString.getOrig(o.getName()))
					.collect(Collectors.joining("->", " (", ")")));
		}
		return sb.toString();
	}

	private <F extends FocusType> void checkAssigneeConstraints(LensContext<F> context,
			DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
			OperationResult result) throws PolicyViolationException, SchemaException {
		for (EvaluatedAssignmentImpl<F> assignment: evaluatedAssignmentTriple.union()) {
			if (evaluatedAssignmentTriple.presentInPlusSet(assignment)) {
				if (!assignment.isPresentInCurrentObject()) {
					checkAssigneeConstraints(context, assignment, PlusMinusZero.PLUS, result);		// only really new assignments
				}
			} else if (evaluatedAssignmentTriple.presentInZeroSet(assignment)) {
				// No need to check anything here. Maintain status quo.
			} else {
				if (assignment.isPresentInCurrentObject()) {
					checkAssigneeConstraints(context, assignment, PlusMinusZero.MINUS, result);		// only assignments that are really deleted
				}
			}
		}
	}
	
	private <F extends FocusType> void checkAssigneeConstraints(LensContext<F> context, EvaluatedAssignment<F> assignment, PlusMinusZero plusMinus, OperationResult result) throws PolicyViolationException, SchemaException {
		PrismObject<?> target = assignment.getTarget();
		if (target != null) {
			Objectable targetType = target.asObjectable();
			if (targetType instanceof AbstractRoleType) {
				Collection<EvaluatedPolicyRule> policyRules = assignment.getThisTargetPolicyRules();
				for (EvaluatedPolicyRule policyRule: policyRules) {
					PolicyConstraintsType policyConstraints = policyRule.getPolicyConstraints();
					if (policyConstraints != null && (!policyConstraints.getMinAssignees().isEmpty() || !policyConstraints.getMaxAssignees().isEmpty())) {
						String focusOid = null;
						if (context.getFocusContext() != null) {
							focusOid = context.getFocusContext().getOid();
						}
						int numberOfAssigneesExceptMyself = countAssignees((PrismObject<? extends AbstractRoleType>)target, focusOid, result);
						if (plusMinus == PlusMinusZero.PLUS) {
							numberOfAssigneesExceptMyself++;
						}
						for (MultiplicityPolicyConstraintType constraint: policyConstraints.getMinAssignees()) {
							Integer multiplicity = XsdTypeMapper.multiplicityToInteger(constraint.getMultiplicity());
							// Complain only if the situation is getting worse
							if (multiplicity >= 0 && numberOfAssigneesExceptMyself < multiplicity && plusMinus == PlusMinusZero.MINUS) {
								EvaluatedPolicyRuleTrigger trigger = new EvaluatedPolicyRuleTrigger(PolicyConstraintKindType.MIN_ASSIGNEES,
										constraint, ""+target+" requires at least "+multiplicity+
										" assignees. The operation would result in "+numberOfAssigneesExceptMyself+" assignees.");
								assignment.triggerConstraint(policyRule, trigger);
										
							}
						}
						for (MultiplicityPolicyConstraintType constraint: policyConstraints.getMaxAssignees()) {
							Integer multiplicity = XsdTypeMapper.multiplicityToInteger(constraint.getMultiplicity());
							// Complain only if the situation is getting worse
							if (multiplicity >= 0 && numberOfAssigneesExceptMyself > multiplicity && plusMinus == PlusMinusZero.PLUS) {
								EvaluatedPolicyRuleTrigger trigger = new EvaluatedPolicyRuleTrigger(PolicyConstraintKindType.MAX_ASSIGNEES,
										constraint, ""+target+" requires at most "+multiplicity+
										" assignees. The operation would result in "+numberOfAssigneesExceptMyself+" assignees.");
								assignment.triggerConstraint(policyRule, trigger);
										
							}
						}
					}
				}
			}
		}
	}

	private int countAssignees(PrismObject<? extends AbstractRoleType> target, String selfOid, OperationResult result) throws SchemaException {
		S_AtomicFilterExit q = QueryBuilder.queryFor(FocusType.class, prismContext)
				.item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(target.getOid());
		if (selfOid != null) {
			q = q.and().not().id(selfOid);
		}
		ObjectQuery query = q.build();
		return repositoryService.countObjects(FocusType.class, query, result);
	}
	

	public <F extends FocusType> boolean processPruning(LensContext<F> context,
			DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
			OperationResult result) throws PolicyViolationException, SchemaException {
		Collection<EvaluatedAssignmentImpl<F>> plusSet = evaluatedAssignmentTriple.getPlusSet();
		if (plusSet == null) {
			return false;
		}
		boolean needToReevaluateAssignments = false;
		for (EvaluatedAssignmentImpl<F> plusAssignment: plusSet) {
			for (EvaluatedPolicyRule targetPolicyRule: plusAssignment.getTargetPolicyRules()) {
				for (EvaluatedPolicyRuleTrigger trigger: targetPolicyRule.getTriggers()) {
					if (!(trigger instanceof EvaluatedExclusionTrigger)) {
						continue;
					}
					EvaluatedExclusionTrigger exclTrigger = (EvaluatedExclusionTrigger) trigger;
					PolicyActionsType actions = targetPolicyRule.getActions();
					if (actions == null || actions.getPrune() == null) {
						continue;
					}
					EvaluatedAssignment<FocusType> conflictingAssignment = exclTrigger.getConflictingAssignment();
					if (conflictingAssignment == null) {
						throw new SystemException("Added assignment "+plusAssignment
								+", the exclusion prune rule was triggered but there is no conflicting assignment in the trigger");
					}
					LOGGER.debug("Pruning assignment {} because it conflicts with added assignment {}", conflictingAssignment, plusAssignment);
					
					PrismContainerValue<AssignmentType> assignmentValueToRemove = conflictingAssignment.getAssignmentType().asPrismContainerValue().clone();
					PrismObjectDefinition<F> focusDef = context.getFocusContext().getObjectDefinition();
					ContainerDelta<AssignmentType> assignmentDelta = ContainerDelta.createDelta(FocusType.F_ASSIGNMENT, focusDef);
					assignmentDelta.addValuesToDelete(assignmentValueToRemove);
					context.getFocusContext().swallowToSecondaryDelta(assignmentDelta);
					
					needToReevaluateAssignments = true;
				}
			}
		}
		
		return needToReevaluateAssignments;
	}

	private <F extends FocusType> void checkSecondaryConstraints(LensContext<F> context,
			DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple, OperationResult result)
			throws SchemaException, PolicyViolationException {
		checkSecondaryConstraints(context, evaluatedAssignmentTriple.getPlusSet(), result);
		checkSecondaryConstraints(context, evaluatedAssignmentTriple.getZeroSet(), result);
		checkSecondaryConstraints(context, evaluatedAssignmentTriple.getMinusSet(), result);
	}

	private <F extends FocusType> void checkSecondaryConstraints(LensContext<F> context,
			Collection<EvaluatedAssignmentImpl<F>> evaluatedAssignmentSet,
			OperationResult result) throws PolicyViolationException, SchemaException {
		for (EvaluatedAssignmentImpl<F> evaluatedAssignment : evaluatedAssignmentSet) {
			checkSecondaryConstraints(evaluatedAssignment, result);
		}
	}

	private <F extends FocusType> void checkSecondaryConstraints(EvaluatedAssignmentImpl<F> evaluatedAssignment,
			OperationResult result) throws PolicyViolationException, SchemaException {

		// Single pass only (for the time being)
		// We consider only directly attached "situation" policy rules. In the future, we might configure this.
		// So, if someone wants to report (forward) triggers from a target, he must ensure that a particular
		// "situation" constraint is present directly on it.
		for (EvaluatedPolicyRule policyRule: evaluatedAssignment.getThisTargetPolicyRules()) {
			if (policyRule.getPolicyConstraints() == null) {
				continue;
			}
			for (PolicySituationPolicyConstraintType situationConstraint : policyRule.getPolicyConstraints().getSituation()) {
				Collection<EvaluatedPolicyRule> sourceRules =
						selectTriggeredRules(evaluatedAssignment, situationConstraint.getSituation());
				if (sourceRules.isEmpty()) {
					continue;
				}
				String message =
						sourceRules.stream()
								.flatMap(r -> r.getTriggers().stream().map(EvaluatedPolicyRuleTrigger::getMessage))
								.distinct()
								.collect(Collectors.joining("; "));
				EvaluatedSituationTrigger trigger = new EvaluatedSituationTrigger(situationConstraint, message, sourceRules);
				evaluatedAssignment.triggerConstraint(policyRule, trigger);
			}
		}
	}

	private <F extends FocusType> Collection<EvaluatedPolicyRule> selectTriggeredRules(
			EvaluatedAssignmentImpl<F> evaluatedAssignment, List<String> situations) {
		// We consider all rules here, i.e. also those that are triggered on targets induced by this one.
		// Decision whether to trigger such rules lies on "primary" policy constraints. (E.g. approvals would
		// not trigger, whereas exclusions probably yes.) Overall, our responsibility is simply to collect
		// all triggered rules.
		return evaluatedAssignment.getTargetPolicyRules().stream()
				.filter(r -> !r.getTriggers().isEmpty() && situations.contains(r.getPolicySituation()))
				.collect(Collectors.toList());
	}

	public <F extends FocusType> void storeAssignmentPolicySituation(LensContext<F> context,
			DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple, OperationResult result) throws SchemaException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			return;
		}
		for (EvaluatedAssignmentImpl<F> evaluatedAssignment : evaluatedAssignmentTriple.getNonNegativeValues()) {
			storeAssignmentPolicySituation(focusContext, evaluatedAssignment, result);
		}
	}

	private <F extends FocusType> void storeAssignmentPolicySituation(LensFocusContext<F> focusContext,
			EvaluatedAssignmentImpl<F> evaluatedAssignment,
			OperationResult result) throws SchemaException {
		Long id = evaluatedAssignment.getAssignmentType().getId();
		if (id == null) {
			throw new IllegalStateException("Help! Help! Assignment with no ID: " + evaluatedAssignment);
		}
		Set<String> currentSituations = new HashSet<>(evaluatedAssignment.getAssignmentType().getPolicySituation());
		Set<String> newSituations = new HashSet<>(evaluatedAssignment.getPolicySituations());
		PropertyDelta<String> situationsDelta = createSituationDelta(
				new ItemPath(FocusType.F_ASSIGNMENT, id, AssignmentType.F_POLICY_SITUATION), currentSituations, newSituations);
		if (situationsDelta != null) {
			focusContext.swallowToProjectionWaveSecondaryDelta(situationsDelta);
		}
	}

	public <F extends FocusType> void storeFocusPolicySituation(LensContext<F> context, Task task, OperationResult result)
			throws SchemaException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			return;
		}
		Set<String> currentSituations = focusContext.getObjectCurrent() != null ?
				new HashSet<>(focusContext.getObjectCurrent().asObjectable().getPolicySituation()) : Collections.emptySet();
		Set<String> newSituations = new HashSet<>(focusContext.getPolicySituations());
		PropertyDelta<String> situationsDelta = createSituationDelta(
				new ItemPath(FocusType.F_POLICY_SITUATION), currentSituations, newSituations);
		if (situationsDelta != null) {
			focusContext.swallowToProjectionWaveSecondaryDelta(situationsDelta);
		}
	}

	@Nullable
	private PropertyDelta<String> createSituationDelta(ItemPath path, Set<String> currentSituations, Set<String> newSituations)
			throws SchemaException {
		if (newSituations.equals(currentSituations)) {
			return null;
		}
		PropertyDelta<String> situationsDelta = (PropertyDelta<String>) DeltaBuilder.deltaFor(FocusType.class, prismContext)
				.item(path)
						.add(CollectionUtils.subtract(newSituations, currentSituations))
						.delete(CollectionUtils.subtract(currentSituations, newSituations))
				.asItemDelta();
		situationsDelta.setEstimatedOldValues(PrismPropertyValue.wrap(currentSituations));
		return situationsDelta;
	}
}
