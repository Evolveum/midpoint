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
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentModificationPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.List;

/**
 * @author semancik
 * @author mederly
 */
@Component
public class AssignmentConstraintEvaluator implements PolicyConstraintEvaluator<AssignmentModificationPolicyConstraintType> {

	private static final String CONSTRAINT_KEY_PREFIX = "assignmentModification.";

	@Autowired private ConstraintEvaluatorHelper evaluatorHelper;

	@Override
	public <F extends FocusType> EvaluatedPolicyRuleTrigger evaluate(JAXBElement<AssignmentModificationPolicyConstraintType> constraintElement,
			PolicyRuleEvaluationContext<F> rctx, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		AssignmentModificationPolicyConstraintType constraint = constraintElement.getValue();
		if (!(rctx instanceof AssignmentPolicyRuleEvaluationContext)) {
			return null;
		}
		AssignmentPolicyRuleEvaluationContext<F> ctx = (AssignmentPolicyRuleEvaluationContext<F>) rctx;
		if (ctx.isDirect && (ctx.inPlus || ctx.inMinus) && matchesOperation(constraint, ctx.inPlus, ctx.inMinus)) {
			List<QName> relationsToCheck = constraint.getRelation().isEmpty() ?
					Collections.singletonList(null) : constraint.getRelation();
			for (QName constraintRelation : relationsToCheck) {
				if (MiscSchemaUtil.compareRelation(constraintRelation, ctx.evaluatedAssignment.getRelation())) {
					return new EvaluatedModificationTrigger(
							PolicyConstraintKindType.ASSIGNMENT_MODIFICATION,
							constraint, createMessage(constraintElement, ctx, result));
				}
			}
		}
		return null;
	}

	private <F extends FocusType> LocalizableMessage createMessage(JAXBElement<AssignmentModificationPolicyConstraintType> constraint,
			AssignmentPolicyRuleEvaluationContext<F> ctx, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		ModelState state = ctx.lensContext.getState();
		String stateKey;
		if (state == ModelState.INITIAL || state == ModelState.PRIMARY) {
			stateKey = "toBe";
		} else {
			stateKey = "was";
			// TODO derive more precise information from executed deltas, if needed
		}
		if (ctx.inPlus) {
			stateKey += "Added";
		} else if (ctx.inMinus) {
			stateKey += "Deleted";
		} else{
			stateKey += "Modified";
		}
		LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
				.key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + stateKey)
				.args(evaluatorHelper.createObjectSpecification(ctx.evaluatedAssignment.getTarget()),
						ctx.evaluatedAssignment.getRelation() != null ? ctx.evaluatedAssignment.getRelation().getLocalPart() : null)
				.build();
		return evaluatorHelper.createLocalizableMessage(constraint.getValue(), ctx, builtInMessage, result);
	}

	private boolean matchesOperation(AssignmentModificationPolicyConstraintType constraint, boolean inPlus, boolean inMinus) {
		List<ChangeTypeType> operations = constraint.getOperation();
		return operations.isEmpty() ||
				inPlus && operations.contains(ChangeTypeType.ADD) ||
				inMinus && operations.contains(ChangeTypeType.DELETE);
	}

}
