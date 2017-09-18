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

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author mederly
 */
public class ObjectPolicyRuleEvaluationContext<F extends FocusType> extends PolicyRuleEvaluationContext<F> {

	public ObjectPolicyRuleEvaluationContext(@NotNull EvaluatedPolicyRule policyRule, LensContext<F> context, Task task) {
		this(policyRule, context, task, ObjectState.AFTER);
	}

	public ObjectPolicyRuleEvaluationContext(@NotNull EvaluatedPolicyRule policyRule, LensContext<F> context, Task task,
			ObjectState state) {
		super(policyRule, context, task, state);
	}

	@Override
	public PolicyRuleEvaluationContext<F> cloneWithStateConstraints(ObjectState state) {
		return new ObjectPolicyRuleEvaluationContext<>(policyRule, lensContext, task, state);
	}

	@Override
	public void triggerRule(Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
		focusContext.triggerRule(policyRule, triggers);
	}

	@Override
	public String getShortDescription() {
		return ObjectTypeUtil.toShortString(focusContext.getObjectAny()) + " / " + state;
	}
}
