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
package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CertificationPolicyActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Starts ad-hoc certifications as prescribed by "certificate" policy action.
 *
 * @author mederly
 */
@Component
public class CertificationHook implements ChangeHook {

	private static final Trace LOGGER = TraceManager.getTrace(CertificationHook.class);

	private static final String HOOK_URI = SchemaConstants.NS_MODEL + "/certification-hook-3";

	@Autowired private HookRegistry hookRegistry;
	@Autowired private CertificationManagerImpl certificationManager;

	@PostConstruct
    public void init() {
        hookRegistry.registerChangeHook(HOOK_URI, this);
        LOGGER.trace("CertificationHook registered.");
    }

	@Override
	public <O extends ObjectType> HookOperationMode invoke(@NotNull ModelContext<O> context, @NotNull Task task,
			@NotNull OperationResult result) {
		if (context.getState() != ModelState.FINAL) {
            return HookOperationMode.FOREGROUND;
        }
		LensElementContext<O> focusContext = (LensElementContext<O>) context.getFocusContext();
		if (focusContext == null || !FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
			return HookOperationMode.FOREGROUND;
		}
		List<CertificationPolicyActionType> actions = new ArrayList<>();
		actions.addAll(getFocusCertificationActions(context));
		actions.addAll(getAssignmentCertificationActions(context));
		try {
			certificationManager.startAdHocCertifications(focusContext.getObjectAny(), actions, task, result);
		} catch (CommonException e) {
			throw new SystemException("Couldn't start ad-hoc campaign(s): " + e.getMessage(), e);
		}
		return HookOperationMode.FOREGROUND;
	}

	private Collection<CertificationPolicyActionType> getFocusCertificationActions(ModelContext<?> context) {
		return getCertificationActions(context.getFocusContext().getPolicyRules());
	}

	private Collection<CertificationPolicyActionType> getAssignmentCertificationActions(ModelContext<?> context) {
		DeltaSetTriple<? extends EvaluatedAssignment<?>> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
		if (evaluatedAssignmentTriple == null) {
			return Collections.emptyList();
		} else {
			return evaluatedAssignmentTriple.stream()
					.flatMap(ea -> getCertificationActions(ea.getAllTargetsPolicyRules()).stream())
					.collect(Collectors.toList());
		}
	}

	private Collection<CertificationPolicyActionType> getCertificationActions(Collection<EvaluatedPolicyRule> policyRules) {
		return policyRules.stream()
				.filter(r -> r.isTriggered() && r.containsEnabledAction(CertificationPolicyActionType.class))
				.map(r -> r.getEnabledAction(CertificationPolicyActionType.class))
				.collect(Collectors.toList());
	}

	@Override
	public void invokeOnException(@NotNull ModelContext context, @NotNull Throwable throwable, @NotNull Task task,
			@NotNull OperationResult result) {
		// Nothing to do
	}

}
