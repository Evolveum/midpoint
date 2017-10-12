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

import com.evolveum.midpoint.model.api.context.EvaluatedModificationTrigger;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentModificationPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

/**
 * @author semancik
 * @author mederly
 */
@Component
public class AssignmentModificationConstraintEvaluator extends ModificationConstraintEvaluator<AssignmentModificationPolicyConstraintType> {

	private static final String CONSTRAINT_KEY_PREFIX = "assignmentModification.";

	@Override
	public <F extends FocusType> EvaluatedPolicyRuleTrigger evaluate(JAXBElement<AssignmentModificationPolicyConstraintType> constraintElement,
			PolicyRuleEvaluationContext<F> rctx, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		AssignmentModificationPolicyConstraintType constraint = constraintElement.getValue();
		if (!(rctx instanceof AssignmentPolicyRuleEvaluationContext)) {
			return null;
		}
		AssignmentPolicyRuleEvaluationContext<F> ctx = (AssignmentPolicyRuleEvaluationContext<F>) rctx;
		if (!ctx.isDirect ||
				!operationMatches(constraint, ctx.inPlus, ctx.inZero, ctx.inMinus) ||
				!relationMatches(constraint, ctx) ||
				!pathsMatch(constraint, ctx)) {
			return null;
		}
		// TODO check modifications
		return new EvaluatedModificationTrigger(PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, constraint,
				createMessage(constraintElement, ctx, result),
				createShortMessage(constraintElement, ctx, result));
	}

	private <F extends FocusType> LocalizableMessage createMessage(JAXBElement<AssignmentModificationPolicyConstraintType> constraint,
			AssignmentPolicyRuleEvaluationContext<F> ctx, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		String keyPostfix = createStateKey(ctx) + createOperationKey(ctx);
		LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
				.key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + keyPostfix)
				.arg(evaluatorHelper.createTechnicalObjectSpecification(ctx.evaluatedAssignment.getTarget()))
				.arg(ctx.evaluatedAssignment.getRelation() != null ? ctx.evaluatedAssignment.getRelation().getLocalPart() : null)
				.build();
		return evaluatorHelper.createLocalizableMessage(constraint.getValue(), ctx, builtInMessage, result);
	}

	@NotNull
	private <F extends FocusType> String createOperationKey(AssignmentPolicyRuleEvaluationContext<F> ctx) {
		if (ctx.inPlus) {
			return "Added";
		} else if (ctx.inMinus) {
			return "Deleted";
		} else {
			return "Modified";
		}
	}

	private <F extends FocusType> LocalizableMessage createShortMessage(JAXBElement<AssignmentModificationPolicyConstraintType> constraint,
			AssignmentPolicyRuleEvaluationContext<F> ctx, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		String keyPostfix = createStateKey(ctx) + createOperationKey(ctx);
		LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
				.key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + keyPostfix)
				.arg(evaluatorHelper.createObjectSpecification(ctx.evaluatedAssignment.getTarget()))
				.arg(evaluatorHelper.createObjectSpecification(ctx.getObject()))
				.build();
		return evaluatorHelper.createLocalizableShortMessage(constraint.getValue(), ctx, builtInMessage, result);
	}

	private <F extends FocusType> boolean relationMatches(AssignmentModificationPolicyConstraintType constraint,
			AssignmentPolicyRuleEvaluationContext<F> ctx) {
		List<QName> relationsToCheck = constraint.getRelation().isEmpty() ?
				singletonList(null) : constraint.getRelation();
		for (QName constraintRelation : relationsToCheck) {
			if (MiscSchemaUtil.compareRelation(constraintRelation, ctx.evaluatedAssignment.getRelation())) {
				return true;
			}
		}
		return false;
	}

	private boolean operationMatches(AssignmentModificationPolicyConstraintType constraint, boolean inPlus, boolean inZero, boolean inMinus) {
		List<ChangeTypeType> operations = constraint.getOperation();
		return operations.isEmpty() ||
				inPlus && operations.contains(ChangeTypeType.ADD) ||
				inZero && operations.contains(ChangeTypeType.MODIFY) ||
				inMinus && operations.contains(ChangeTypeType.DELETE);
	}

	// TODO discriminate between primary and secondary changes (perhaps make it configurable)
	// Primary changes are "approvable", secondary ones are not.
	private <F extends FocusType> boolean pathsMatch(AssignmentModificationPolicyConstraintType constraint,
			AssignmentPolicyRuleEvaluationContext<F> ctx) throws SchemaException {

		// hope this is correctly filled in
		if (constraint.getItem().isEmpty()) {
			if (ctx.inPlus || ctx.inMinus) {
				return true;
			} else {
				Collection<? extends ItemDelta<?, ?>> subItemDeltas = ctx.evaluatedAssignment.getAssignmentIdi().getSubItemDeltas();
				return subItemDeltas != null && !subItemDeltas.isEmpty();
			}
		}
		for (ItemPathType path : constraint.getItem()) {
			ItemPath itemPath = path.getItemPath();
			if (ctx.inPlus && !pathMatches(ctx.evaluatedAssignment.getAssignmentType(false), itemPath) ||
					ctx.inMinus && !pathMatches(ctx.evaluatedAssignment.getAssignmentType(true), itemPath) ||
					ctx.inZero && !pathMatches(ctx.evaluatedAssignment.getAssignmentIdi().getSubItemDeltas(), itemPath)) {
				return false;
			}
		}
		return true;
	}

	private boolean pathMatches(AssignmentType assignment, ItemPath path) throws SchemaException {
		return assignment.asPrismContainerValue().containsItem(path, false);
	}

	private boolean pathMatches(Collection<? extends ItemDelta<?, ?>> deltas, ItemPath path) {
		for (ItemDelta<?, ?> delta : emptyIfNull(deltas)) {
			// TODO what about changes like extension/cities[2]/name (in delta) vs. extension/cities/name (in spec)
			if (path.isSubPathOrEquivalent(delta.getPath().tail(2))) {
				return true;
			}
		}
		return false;
	}
}
