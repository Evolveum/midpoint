/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentTargetImpl;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class HasAssignmentConstraintEvaluator implements PolicyConstraintEvaluator<HasAssignmentPolicyConstraintType> {

	@Autowired private PrismContext prismContext;
	@Autowired private MatchingRuleRegistry matchingRuleRegistry;

	@Override
	public <F extends FocusType> EvaluatedPolicyRuleTrigger evaluate(JAXBElement<HasAssignmentPolicyConstraintType> constraintElement,
			PolicyRuleEvaluationContext<F> ctx, OperationResult result) throws SchemaException {

		boolean shouldExist = QNameUtil.match(constraintElement.getName(), PolicyConstraintsType.F_HAS_ASSIGNMENT);
		HasAssignmentPolicyConstraintType constraint = constraintElement.getValue();
		if (constraint.getTargetRef() == null) {
			throw new SchemaException("No targetRef in hasAssignment constraint");
		}

		DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple = ctx.lensContext.getEvaluatedAssignmentTriple();
		if (evaluatedAssignmentTriple == null) {
			return createTriggerIfShouldNotExist(shouldExist, constraint);
		}
		boolean allowZero = constraint.getInState().isEmpty() || constraint.getInState().contains(AssignmentStateType.ZERO);
		boolean allowPlus = constraint.getInState().isEmpty() || constraint.getInState().contains(AssignmentStateType.PLUS);
		if (!allowPlus && !allowZero) {
			throw new AssertionError("Assignments in neither zero nor plus set are to be checked");
		}
		boolean allowDirect = !Boolean.FALSE.equals(constraint.isDirect());
		boolean allowIndirect = !Boolean.TRUE.equals(constraint.isDirect());
		boolean allowEnabled = !Boolean.FALSE.equals(constraint.isEnabled());
		boolean allowDisabled = !Boolean.TRUE.equals(constraint.isEnabled());

		for (EvaluatedAssignmentImpl<?> evaluatedAssignment : evaluatedAssignmentTriple.getNonNegativeValues()) {
			boolean assignmentIsInPlusSet = evaluatedAssignmentTriple.presentInPlusSet(evaluatedAssignment);
			boolean assignmentIsInZeroSet = evaluatedAssignmentTriple.presentInZeroSet(evaluatedAssignment);
			DeltaSetTriple<EvaluatedAssignmentTargetImpl> targetsTriple = evaluatedAssignment.getRoles();
			for (EvaluatedAssignmentTargetImpl target : targetsTriple.getNonNegativeValues()) {
				if (!target.appliesToFocus()) {
					continue;
				}
				if (!(allowDirect && target.isDirectlyAssigned() || allowIndirect && !target.isDirectlyAssigned())) {
					continue;
				}
				if (!(allowEnabled && target.isValid() || allowDisabled && !target.isValid())) {
					continue;
				}
				if (!relationMatches(constraint.getTargetRef().getRelation(), constraint.getRelation(), target.getAssignment())) {
					continue;
				}
				boolean targetIsInPlusSet = targetsTriple.presentInPlusSet(target);
				boolean targetIsInZeroSet = targetsTriple.presentInZeroSet(target);
				boolean isPlus = assignmentIsInPlusSet || targetIsInPlusSet;
				boolean isZero = assignmentIsInZeroSet && targetIsInZeroSet;
				if (!(allowPlus && isPlus || allowZero && isZero)) {
					continue;
				}
				if (ExclusionConstraintEvaluator.matches(constraint.getTargetRef(), target, prismContext, matchingRuleRegistry, "hasAssignment constraint")) {
					if (shouldExist) {
						// TODO more specific trigger, containing information on matching assignment; see ExclusionConstraintEvaluator
						return new EvaluatedPolicyRuleTrigger<>(PolicyConstraintKindType.HAS_ASSIGNMENT, constraint,
								"Assignment exists for " + ObjectTypeUtil.toShortString(target.getTarget()));
					}
				}
			}
		}
		return createTriggerIfShouldNotExist(shouldExist, constraint);
	}

	private EvaluatedPolicyRuleTrigger createTriggerIfShouldNotExist(boolean shouldExist, HasAssignmentPolicyConstraintType constraint) {
		if (shouldExist) {
			return null;
		} else if (constraint.getTargetRef().getOid() != null) {
			return new EvaluatedPolicyRuleTrigger<>(PolicyConstraintKindType.HAS_NO_ASSIGNMENT, constraint,
					"No relevant assignment exists for " +
							(constraint.getTargetRef().getType() != null ? constraint.getTargetRef().getType().getLocalPart() + " " : "") +
							(constraint.getTargetRef().getTargetName() != null ? constraint.getTargetRef().getTargetName() : constraint.getTargetRef().getOid()));
			// (actually, targetName seems to be always null, even if specified in the policy rule)
		} else {
			return new EvaluatedPolicyRuleTrigger<>(PolicyConstraintKindType.HAS_NO_ASSIGNMENT, constraint,
					"No relevant assignment exists" + (constraint.getName() != null ? " (" + constraint.getName() + ")" : ""));
		}
	}

	private boolean relationMatches(QName primaryRelationToMatch, List<QName> secondaryRelationsToMatch, AssignmentType assignment) {
		if (assignment == null || assignment.getTargetRef() == null) {
			return false;           // shouldn't occur
		}
		List<QName> relationsToMatch = primaryRelationToMatch != null
				? Collections.singletonList(primaryRelationToMatch)
				: new ArrayList<>(secondaryRelationsToMatch);
		if (relationsToMatch.isEmpty()) {
			relationsToMatch.add(SchemaConstants.ORG_DEFAULT);
		}
		return ObjectTypeUtil.relationMatches(relationsToMatch, assignment.getTargetRef().getRelation());
	}
}
