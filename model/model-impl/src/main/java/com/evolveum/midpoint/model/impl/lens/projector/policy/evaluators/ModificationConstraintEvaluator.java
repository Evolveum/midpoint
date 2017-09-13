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
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModificationPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.List;

/**
 * @author semancik
 * @author mederly
 */
@Component
public class ModificationConstraintEvaluator implements PolicyConstraintEvaluator<ModificationPolicyConstraintType> {

	private static final Trace LOGGER = TraceManager.getTrace(ModificationConstraintEvaluator.class);

	@Override
	public <F extends FocusType> EvaluatedPolicyRuleTrigger<?> evaluate(JAXBElement<ModificationPolicyConstraintType> constraint,
			PolicyRuleEvaluationContext<F> rctx, OperationResult result) throws SchemaException {

		if (!(rctx instanceof ObjectPolicyRuleEvaluationContext)) {
			return null;
		}
		ObjectPolicyRuleEvaluationContext<F> ctx = (ObjectPolicyRuleEvaluationContext<F>) rctx;

		if (modificationConstraintMatches(constraint.getValue(), ctx)) {
			ModelState state = rctx.lensContext.getState();
			String verb;
			if (state == ModelState.INITIAL || state == ModelState.PRIMARY) {
				verb = "is about to be";
			} else if (state == ModelState.FINAL) {
				verb = "was";
			} else {
				verb = "is being (or was)";		// TODO derive more precise information from executed deltas, if needed
			}
			return new EvaluatedModificationTrigger(PolicyConstraintKindType.OBJECT_MODIFICATION,
					constraint.getValue(),
					LocalizableMessageBuilder.buildFallbackMessage("Object "+ ObjectTypeUtil.toShortString(ctx.focusContext.getObjectAny())+" " + verb + " " + ctx.focusContext.getOperation().getPastTense()));
		} else {
			return null;
		}
	}

	private <F extends FocusType> boolean modificationConstraintMatches(ModificationPolicyConstraintType constraint,
			ObjectPolicyRuleEvaluationContext<F> ctx) throws SchemaException {
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
		for (ItemPathType path : constraint.getItem()) {
			if (!pathMatches(summaryDelta, ctx.focusContext.getObjectOld(), path.getItemPath())) {
				return false;
			}
		}
		return true;
	}

	private <F extends FocusType> boolean pathMatches(ObjectDelta<?> delta, PrismObject<F> objectOld, ItemPath path)
			throws SchemaException {
		if (delta.isAdd()) {
			return delta.getObjectToAdd().containsItem(path, false);
		} else if (delta.isDelete()) {
			return objectOld != null && objectOld.containsItem(path, false);
		} else {
			return delta.findItemDelta(path) != null;
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
