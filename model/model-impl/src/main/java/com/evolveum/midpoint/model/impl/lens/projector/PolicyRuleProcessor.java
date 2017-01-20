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

import java.util.Collection;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentTargetImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExclusionPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MultiplicityPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;

/**
 * @author semancik
 *
 */
@Component
public class PolicyRuleProcessor {
	
	private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleProcessor.class);
	
	@Autowired(required = true)
    private PrismContext prismContext;
	
	@Autowired(required = true)
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
        checkExclusions(context, evaluatedAssignmentTriple.getZeroSet(), evaluatedAssignmentTriple.getPlusSet());
        checkExclusions(context, evaluatedAssignmentTriple.getPlusSet(), evaluatedAssignmentTriple.getPlusSet());
        checkAssigneeConstraints(context, evaluatedAssignmentTriple, result);
	}
	
	private <F extends FocusType> void checkAssignmentRules(LensContext<F> context,
			DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
			OperationResult result) throws PolicyViolationException, SchemaException {
		checkAssignmentRules(context, evaluatedAssignmentTriple.getPlusSet(), result);
		checkAssignmentRules(context, evaluatedAssignmentTriple.getMinusSet(), result);
	}
	
	private <F extends FocusType> void checkAssignmentRules(LensContext<F> context,
			Collection<EvaluatedAssignmentImpl<F>> evaluatedAssignmentSet,
			OperationResult result) throws PolicyViolationException, SchemaException {
		for(EvaluatedAssignmentImpl<F> evaluatedAssignment: evaluatedAssignmentSet) {
			Collection<EvaluatedPolicyRule> policyRules = evaluatedAssignment.getThisTargetPolicyRules();
			for (EvaluatedPolicyRule policyRule: policyRules) {
				PolicyConstraintsType policyConstraints = policyRule.getPolicyConstraints();
				if (policyConstraints == null) {
					continue;
				}
				for (AssignmentPolicyConstraintType assignmentConstraint: policyConstraints.getAssignment()) {
					if (assignmentConstraint.getRelation().isEmpty()) {
						if (MiscSchemaUtil.compareRelation(null, evaluatedAssignment.getRelation())) {
							EvaluatedPolicyRuleTrigger trigger = new EvaluatedPolicyRuleTrigger(PolicyConstraintKindType.ASSIGNMENT, 
									assignmentConstraint, "Assignment of "+evaluatedAssignment.getTarget());
							evaluatedAssignment.triggerConstraint(policyRule, trigger);
						}
					} else {
						for (QName constraintRelation: assignmentConstraint.getRelation()) {
							if (MiscSchemaUtil.compareRelation(constraintRelation, evaluatedAssignment.getRelation())) {
								EvaluatedPolicyRuleTrigger trigger = new EvaluatedPolicyRuleTrigger(PolicyConstraintKindType.ASSIGNMENT, 
										assignmentConstraint, "Assignment of "+evaluatedAssignment.getTarget());
								evaluatedAssignment.triggerConstraint(policyRule, trigger);
							}
						}
					}
				}
			}
		}
	}
	
	private <F extends FocusType> void checkExclusions(LensContext<F> context, Collection<EvaluatedAssignmentImpl<F>> assignmentsA,
			Collection<EvaluatedAssignmentImpl<F>> assignmentsB) throws PolicyViolationException {
		for (EvaluatedAssignmentImpl<F> assignmentA: assignmentsA) {
			checkExclusion(context, assignmentA, assignmentsB);
		}
	}

	private <F extends FocusType> void checkExclusion(LensContext<F> context, EvaluatedAssignmentImpl<F> assignmentA,
			Collection<EvaluatedAssignmentImpl<F>> assignmentsB) throws PolicyViolationException {
		for (EvaluatedAssignmentImpl<F> assignmentB: assignmentsB) {
			checkExclusion(context, assignmentA, assignmentB);
		}
	}

	private <F extends FocusType> void checkExclusion(LensContext<F> context, EvaluatedAssignmentImpl<F> assignmentA, EvaluatedAssignmentImpl<F> assignmentB) throws PolicyViolationException {
		if (assignmentA == assignmentB) {
			// Same thing, this cannot exclude itself
			return;
		}
		for (EvaluatedAssignmentTargetImpl eRoleA: assignmentA.getRoles().getAllValues()) {
			for (EvaluatedAssignmentTargetImpl eRoleB: assignmentB.getRoles().getAllValues()) {
				checkExclusion(assignmentA, assignmentB, eRoleA, eRoleB);
			}
		}
	}

	private <F extends FocusType> void checkExclusion(EvaluatedAssignmentImpl<F> assignmentA, EvaluatedAssignmentImpl<F> assignmentB, EvaluatedAssignmentTargetImpl roleA, EvaluatedAssignmentTargetImpl roleB) throws PolicyViolationException {
		checkExclusionOneWayLegacy(assignmentA, assignmentB, roleA, roleB);
		checkExclusionOneWayLegacy(assignmentB, assignmentA, roleB, roleA);
		checkExclusionOneWayRuleBased(assignmentA, assignmentB, roleA, roleB);
		checkExclusionOneWayRuleBased(assignmentB, assignmentA, roleB, roleA);
	}

	private <F extends FocusType> void checkExclusionOneWayLegacy(EvaluatedAssignmentImpl<F> assignmentA, EvaluatedAssignmentImpl<F> assignmentB, 
			EvaluatedAssignmentTargetImpl roleA, EvaluatedAssignmentTargetImpl roleB) throws PolicyViolationException {
		for (ExclusionPolicyConstraintType exclusionA : roleA.getExclusions()) {
			checkAndTriggerExclusionConstraintViolation(assignmentA, assignmentB, roleA, roleB, exclusionA, null);
		}
	}

	private <F extends FocusType> void checkExclusionOneWayRuleBased(EvaluatedAssignmentImpl<F> assignmentA, EvaluatedAssignmentImpl<F> assignmentB,
			EvaluatedAssignmentTargetImpl roleA, EvaluatedAssignmentTargetImpl roleB) throws PolicyViolationException {
		for (EvaluatedPolicyRule policyRule : assignmentA.getThisTargetPolicyRules()) {	// or getTargetPolicyRules?
			if (policyRule.getPolicyConstraints() != null) {
				for (ExclusionPolicyConstraintType exclusionConstraint : policyRule.getPolicyConstraints().getExclusion()) {
					checkAndTriggerExclusionConstraintViolation(assignmentA, assignmentB, roleA, roleB, exclusionConstraint, policyRule);
				}
			}
		}
	}

	private <F extends FocusType> void checkAndTriggerExclusionConstraintViolation(EvaluatedAssignmentImpl<F> assignmentA,
			EvaluatedAssignmentImpl<F> assignmentB, EvaluatedAssignmentTargetImpl roleA, EvaluatedAssignmentTargetImpl roleB,
			ExclusionPolicyConstraintType constraint, EvaluatedPolicyRule policyRule)
			throws PolicyViolationException {
		ObjectReferenceType targetRef = constraint.getTargetRef();
		if (roleB.getOid().equals(targetRef.getOid())) {
			EvaluatedPolicyRuleTrigger trigger = new EvaluatedPolicyRuleTrigger(PolicyConstraintKindType.EXCLUSION,
					constraint, "Violation of SoD policy: " + roleA.getTarget() + " excludes " + roleB.getTarget() +
					", they cannot be assigned at the same time", assignmentB);
			assignmentA.triggerConstraint(policyRule, trigger);
		}
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
					if (trigger.getConstraintKind() != PolicyConstraintKindType.EXCLUSION) {
						continue;
					}
					PolicyActionsType actions = targetPolicyRule.getActions();
					if (actions == null || actions.getPrune() == null) {
						continue;
					}
					EvaluatedAssignment<FocusType> conflictingAssignment = trigger.getConflictingAssignment();
					if (conflictingAssignment == null) {
						throw new SystemException("Added assignment "+plusAssignment
								+", the exclusion prune rule was triggered but there is no conflicting assignment in the trigger");
					}
					LOGGER.debug("Pruning assignment {} because it conflicts with added assignment {}", conflictingAssignment, plusAssignment);
					
					// TODO
					// TODO
					// TODO
					// TODO
					
					needToReevaluateAssignments = true;
				}
			}
		}
		
		return needToReevaluateAssignments;
	}

}
