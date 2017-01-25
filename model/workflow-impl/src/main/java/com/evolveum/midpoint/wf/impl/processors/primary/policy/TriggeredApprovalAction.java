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

package com.evolveum.midpoint.wf.impl.processors.primary.policy;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalPolicyActionType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO consolidate (think if constraints are needed)
 * @author mederly
 */
class TriggeredApprovalAction {
	final @NotNull ApprovalPolicyActionType approvalAction;
	final @NotNull Collection<EvaluatedPolicyRuleTrigger> triggers;			// may be empty for default situations TODO decide what of the these to keep
	final @NotNull Collection<AbstractPolicyConstraintType> constraints;	// may be empty for default situations

	TriggeredApprovalAction(
			@NotNull ApprovalPolicyActionType approvalAction,
			@NotNull Collection<EvaluatedPolicyRuleTrigger> triggers,
			@NotNull Collection<AbstractPolicyConstraintType> constraints) {
		this.approvalAction = approvalAction;
		this.triggers = triggers;
		this.constraints = constraints;
	}

	TriggeredApprovalAction(
			@NotNull ApprovalPolicyActionType approvalAction) {
		this.approvalAction = approvalAction;
		this.triggers = Collections.emptyList();
		this.constraints = Collections.emptyList();
	}
}
