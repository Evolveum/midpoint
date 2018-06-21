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
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author mederly
 */
public abstract class PolicyRuleEvaluationContext<F extends FocusType> implements Cloneable {

	@NotNull public final EvaluatedPolicyRule policyRule;
	@NotNull public final LensContext<F> lensContext;
	@NotNull public final LensFocusContext<F> focusContext;
	@NotNull public final Task task;
	@NotNull public final ObjectState state;
	@NotNull public final RulesEvaluationContext globalCtx;

	public PolicyRuleEvaluationContext(@NotNull EvaluatedPolicyRule policyRule, @NotNull LensContext<F> context,
			@NotNull Task task, @NotNull RulesEvaluationContext globalCtx, @NotNull ObjectState state) {
		this.policyRule = policyRule;
		this.lensContext = context;
		this.focusContext = context.getFocusContext();
		this.task = task;
		this.globalCtx = globalCtx;
		if (focusContext == null) {
			throw new IllegalStateException("No focus context");
		}
		this.state = state;
	}

	public abstract PolicyRuleEvaluationContext<F> cloneWithStateConstraints(ObjectState state);

	public abstract void triggerRule(Collection<EvaluatedPolicyRuleTrigger<?>> triggers);

	public PrismObject<F> getObject() {
		if (state == ObjectState.BEFORE) {
			return focusContext.getObjectOld();
		} else {
			return focusContext.getObjectNew();
		}
	}

	public boolean isApplicableToState() {
		return getObject() != null;
	}

	public abstract String getShortDescription();

	public void record() {
		globalCtx.rulesToRecord.add(policyRule);
	}
}
