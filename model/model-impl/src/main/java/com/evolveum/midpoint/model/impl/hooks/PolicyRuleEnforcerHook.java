/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.model.impl.hooks;

import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.TriggeredPolicyRulesStorageStrategyType.FULL;

/**
 * Hook used to enforce the policy rules that have the enforce action.
 *
 * @author semancik
 *
 */
@Component
public class PolicyRuleEnforcerHook implements ChangeHook {

	private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleEnforcerHook.class);

	public static final String HOOK_URI = SchemaConstants.NS_MODEL + "/policy-rule-enforcer-hook-3";

	@Autowired private HookRegistry hookRegistry;
	@Autowired private PrismContext prismContext;

	@PostConstruct
    public void init() {
        hookRegistry.registerChangeHook(HOOK_URI, this);
		LOGGER.trace("PolicyRuleEnforcerHook registered.");
    }

    // TODO clean this up
    private class EvaluationContext {
		private final List<LocalizableMessage> messages = new ArrayList<>();
		private final List<EvaluatedPolicyRuleType> rules = new ArrayList<>();
    }

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.hooks.ChangeHook#invoke(com.evolveum.midpoint.model.api.context.ModelContext, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public <O extends ObjectType> HookOperationMode invoke(@NotNull ModelContext<O> context, @NotNull Task task,
			@NotNull OperationResult result) throws PolicyViolationException {

		if (context.getState() == ModelState.PRIMARY) {
			EvaluationContext evalCtx = invokeInternal(context, task, result);
			if (!evalCtx.messages.isEmpty()) {
				throw new PolicyViolationException(evalCtx.messages);
			}
        }
		return HookOperationMode.FOREGROUND;

	}

	@Override
	public void invokePreview(@NotNull ModelContext<? extends ObjectType> context, Task task, OperationResult result) {
		// TODO check partial processing option (after it will be implemented)
		PolicyRuleEnforcerHookPreviewOutputType output = new PolicyRuleEnforcerHookPreviewOutputType(prismContext);
		EvaluationContext evalCtx = invokeInternal(context, task, result);
		output.getRule().addAll(evalCtx.rules);
		((LensContext) context).addHookPreviewResults(HOOK_URI, Collections.singletonList(output));
	}

	@NotNull
	private <O extends ObjectType> EvaluationContext invokeInternal(@NotNull ModelContext<O> context, @NotNull Task task,
			@NotNull OperationResult result) {
		EvaluationContext evalCtx = new EvaluationContext();
		ModelElementContext<O> focusContext = context.getFocusContext();
		if (focusContext == null || !FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
			return evalCtx;
		}

		evaluateFocusRules(evalCtx, (ModelContext<FocusType>) context, task, result);
		evaluateAssignmentRules(evalCtx, (ModelContext<FocusType>) context, task, result);

		return evalCtx;
	}

	private <F extends FocusType> void evaluateFocusRules(EvaluationContext evalCtx, ModelContext<F> context, Task task,
			OperationResult result) {
		enforceTriggeredRules(evalCtx, context.getFocusContext().getPolicyRules());
	}

	private <F extends FocusType> void evaluateAssignmentRules(EvaluationContext evalCtx, ModelContext<F> context, Task task,
			OperationResult result) {
		DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
		if (evaluatedAssignmentTriple == null) {
			return;
		}
		evaluatedAssignmentTriple.simpleAccept(assignment -> enforceTriggeredRules(evalCtx, assignment.getAllTargetsPolicyRules()));
	}

	private <F extends FocusType> void enforceTriggeredRules(EvaluationContext evalCtx, Collection<EvaluatedPolicyRule> policyRules) {
		for (EvaluatedPolicyRule policyRule: policyRules) {

			Collection<EvaluatedPolicyRuleTrigger<?>> triggers = policyRule.getTriggers();
			if (triggers.isEmpty()) {
				continue;
			}

			if (!isEnforce(policyRule)) {
				continue;
			}

			// TODO really include assignments content?
			policyRule.addToEvaluatedPolicyRuleTypes(evalCtx.rules, new PolicyRuleExternalizationOptions(FULL, true, true));

			for (EvaluatedPolicyRuleTrigger trigger: triggers) {
				if (trigger.getMessage() != null) {
					evalCtx.messages.add(trigger.getMessage());
				}
			}
		}
	}

	private boolean isEnforce(EvaluatedPolicyRule policyRule) {
		PolicyActionsType actions = policyRule.getActions();

		if (actions == null) {
			// enforce is NO LONGER the default
			return false;
		}

		EnforcementPolicyActionType enforcement = actions.getEnforcement();
		if (enforcement == null) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.hooks.ChangeHook#invokeOnException(com.evolveum.midpoint.model.api.context.ModelContext, java.lang.Throwable, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public void invokeOnException(@NotNull ModelContext context, @NotNull Throwable throwable, @NotNull Task task,
			@NotNull OperationResult result) {
		// Nothing to do
	}

	// Must be executed first, see MID-3836
	@Override
	public int getPriority() {
		return 0;
	}
}
