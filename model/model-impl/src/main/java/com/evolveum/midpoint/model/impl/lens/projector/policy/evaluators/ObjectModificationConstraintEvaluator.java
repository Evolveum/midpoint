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
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModificationPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author semancik
 * @author mederly
 */
@Component
public class ObjectModificationConstraintEvaluator extends ModificationConstraintEvaluator<ModificationPolicyConstraintType> {

	private static final Trace LOGGER = TraceManager.getTrace(ObjectModificationConstraintEvaluator.class);

	private static final String CONSTRAINT_KEY_PREFIX = "objectModification.";

	@Override
	public <F extends FocusType> EvaluatedPolicyRuleTrigger<?> evaluate(JAXBElement<ModificationPolicyConstraintType> constraint,
			PolicyRuleEvaluationContext<F> rctx, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

		if (!(rctx instanceof ObjectPolicyRuleEvaluationContext)) {
			return null;
		}
		ObjectPolicyRuleEvaluationContext<F> ctx = (ObjectPolicyRuleEvaluationContext<F>) rctx;

		if (modificationConstraintMatches(constraint.getValue(), ctx, result)) {
			LocalizableMessage message = createMessage(constraint, rctx, result);
			LocalizableMessage shortMessage = createShortMessage(constraint, rctx, result);
			return new EvaluatedModificationTrigger(PolicyConstraintKindType.OBJECT_MODIFICATION, constraint.getValue(), 
					message, shortMessage);
		} else {
			return null;
		}
	}

	private <F extends FocusType> LocalizableMessage createMessage(JAXBElement<ModificationPolicyConstraintType> constraint,
			PolicyRuleEvaluationContext<F> rctx, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		String keyPostfix = createStateKey(rctx) + createOperationKey(rctx);
		LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
				.key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + keyPostfix)
				.args(ObjectTypeUtil.createDisplayInformation(rctx.focusContext.getObjectAny(), true))
				.build();
		return evaluatorHelper.createLocalizableMessage(constraint.getValue(), rctx, builtInMessage, result);
	}

	private <F extends FocusType> LocalizableMessage createShortMessage(JAXBElement<ModificationPolicyConstraintType> constraint,
			PolicyRuleEvaluationContext<F> rctx, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		String keyPostfix = createStateKey(rctx) + createOperationKey(rctx);
		LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
				.key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY_PREFIX + keyPostfix)
				.args(ObjectTypeUtil.createDisplayInformation(rctx.focusContext.getObjectAny(), false))
				.build();
		return evaluatorHelper.createLocalizableShortMessage(constraint.getValue(), rctx, builtInMessage, result);
	}

	@NotNull
	private <F extends FocusType> String createOperationKey(PolicyRuleEvaluationContext<F> rctx) {
		if (rctx.focusContext.isAdd()) {
			return "Added";
		} else if (rctx.focusContext.isDelete()) {
			return "Deleted";
		} else {
			return "Modified";
		}
	}

	// TODO discriminate between primary and secondary changes (perhaps make it configurable)
	// Primary changes are "approvable", secondary ones are not.
	private <F extends FocusType> boolean modificationConstraintMatches(ModificationPolicyConstraintType constraint,
			ObjectPolicyRuleEvaluationContext<F> ctx, OperationResult result)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
			SecurityViolationException, ExpressionEvaluationException {
		if (!operationMatches(ctx.focusContext, constraint.getOperation())) {
			LOGGER.trace("Rule {} operation not applicable", ctx.policyRule.getName());
			return false;
		}
		if (constraint.getItem().isEmpty()) {
			return ctx.focusContext.hasAnyDelta();
		}
		ObjectDelta<?> summaryDelta = ObjectDelta.union(ctx.focusContext.getPrimaryDelta(), ctx.focusContext.getSecondaryDelta());
		if (summaryDelta == null) {
			return false;
		}
		boolean exactPathMatch = isTrue(constraint.isExactPathMatch());
		for (ItemPathType path : constraint.getItem()) {
			if (!pathMatches(summaryDelta, ctx.focusContext.getObjectOld(), path.getItemPath(), exactPathMatch)) {
				return false;
			}
		}
		if (!expressionPasses(constraint, ctx, result)) {
			return false;
		}
		return true;
	}

	private <F extends FocusType> boolean pathMatches(ObjectDelta<?> delta, PrismObject<F> objectOld, ItemPath path,
			boolean exactPathMatch) throws SchemaException {
		if (delta.isAdd()) {
			return delta.getObjectToAdd().containsItem(path, false);
		} else if (delta.isDelete()) {
			return objectOld != null && objectOld.containsItem(path, false);
		} else {
			return ItemDelta.pathMatches(emptyIfNull(delta.getModifications()), path, 0, exactPathMatch);
		}
	}

	private <F extends FocusType> boolean operationMatches(LensFocusContext<F> focusContext, List<ChangeTypeType> operations) {
		if (operations.isEmpty()) {
			return true;
		}
		for (ChangeTypeType operation: operations) {
			if (focusContext.operationMatches(operation)) {
				return true;
			}
		}
		return false;
	}
}
