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

import java.util.Collection;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EnforcementPolicyActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionsType;

/**
 * Hook used to enfore the policy rules that have the enforce action.
 * 
 * @author semancik
 *
 */
@Component
public class PolicyRuleEnforcerHook implements ChangeHook {
	
	private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleEnforcerHook.class);
	
	public static final String HOOK_URI = SchemaConstants.NS_MODEL + "/policy-rule-enforcer-hook-3";
	
	@Autowired(required = true)
    private HookRegistry hookRegistry;
	
	@PostConstruct
    public void init() {
        hookRegistry.registerChangeHook(HOOK_URI, this);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("PolicyRuleEnforcerHook registered.");
        }
    }

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.hooks.ChangeHook#invoke(com.evolveum.midpoint.model.api.context.ModelContext, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public <O extends ObjectType> HookOperationMode invoke(ModelContext<O> context, Task task,
			OperationResult result) throws PolicyViolationException {
		
		if (context.getState() != ModelState.PRIMARY) {
            return HookOperationMode.FOREGROUND;
        }
	
		ModelElementContext<O> focusContext = context.getFocusContext();
		if (focusContext == null) {
			return HookOperationMode.FOREGROUND;
		}
		
		if (!FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
			return HookOperationMode.FOREGROUND;
    	}
		
		evaluateFocusRules((ModelContext<FocusType>) context, task, result);
		evaluateAssignmentRules((ModelContext<FocusType>) context, task, result);
		
		return HookOperationMode.FOREGROUND;
	}
	
	private <F extends FocusType> void evaluateFocusRules(ModelContext<F> context, Task task,
			OperationResult result) throws PolicyViolationException {
		ModelElementContext<F> focusContext = context.getFocusContext();
		
		StringBuilder compositeMessageSb = new StringBuilder();
		enforceTriggeredRules(compositeMessageSb, focusContext.getPolicyRules());
		if (compositeMessageSb.length() != 0) {
			throw new PolicyViolationException(compositeMessageSb.toString());
		}
	}
	
	private <F extends FocusType> void evaluateAssignmentRules(ModelContext<F> context, Task task,
			OperationResult result) throws PolicyViolationException {
	
		DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
		if (evaluatedAssignmentTriple == null) {
			return;
		}
		
		StringBuilder compositeMessageSb = new StringBuilder();
		evaluatedAssignmentTriple.accept(assignment -> {
			enforceTriggeredRules(compositeMessageSb, assignment.getFocusPolicyRules());
			enforceTriggeredRules(compositeMessageSb, assignment.getAllTargetsPolicyRules());
		});

		if (compositeMessageSb.length() != 0) {
			throw new PolicyViolationException(compositeMessageSb.toString());
		}
	}

	private <F extends FocusType> void enforceTriggeredRules(StringBuilder compositeMessageSb, Collection<EvaluatedPolicyRule> policyRules) {
		for (EvaluatedPolicyRule policyRule: policyRules) {

			Collection<EvaluatedPolicyRuleTrigger<?>> triggers = policyRule.getTriggers();
			if (triggers.isEmpty()) {
				continue;
			}
			
			if (!isEnforce(policyRule)) {
				continue;
			}
			
			for (EvaluatedPolicyRuleTrigger trigger: triggers) {
				if (trigger.getMessage() != null) {
					if (compositeMessageSb.length() != 0) {
						compositeMessageSb.append("; ");
					}
					compositeMessageSb.append(trigger.getMessage());
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
	public void invokeOnException(ModelContext context, Throwable throwable, Task task,
			OperationResult result) {
		// Nothing to do
	}

}
