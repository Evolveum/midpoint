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

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggeredPolicyRulesStorageStrategyType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.TriggeredPolicyRulesStorageStrategyType.FULL;

/**
 * @author mederly
 */
public class PolicyRuleExternalizationOptions implements Serializable {

	@NotNull private TriggeredPolicyRulesStorageStrategyType triggeredRulesStorageStrategy;
	private boolean includeAssignmentsContent;
	private boolean respectFinalFlag;

	public PolicyRuleExternalizationOptions() {
		this(FULL, false, true);
	}

	public PolicyRuleExternalizationOptions(TriggeredPolicyRulesStorageStrategyType triggeredRulesStorageStrategy,
			boolean includeAssignmentsContent, boolean respectFinalFlag) {
		this.triggeredRulesStorageStrategy = triggeredRulesStorageStrategy != null ? triggeredRulesStorageStrategy : FULL;
		this.includeAssignmentsContent = includeAssignmentsContent;
		this.respectFinalFlag = respectFinalFlag;
	}

	@NotNull
	public TriggeredPolicyRulesStorageStrategyType getTriggeredRulesStorageStrategy() {
		return triggeredRulesStorageStrategy;
	}

	public boolean isIncludeAssignmentsContent() {
		return includeAssignmentsContent;
	}

	public boolean isRespectFinalFlag() {
		return respectFinalFlag;
	}
}
