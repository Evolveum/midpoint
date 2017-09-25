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

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyTriggerStorageStrategyType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyTriggerStorageStrategyType.FULL;

/**
 * @author mederly
 */
public class PolicyRuleExternalizationOptions implements Serializable {

	@NotNull private PolicyTriggerStorageStrategyType triggerStorageStrategy;
	private boolean includeAssignmentsContent;
	private boolean respectFinalFlag;

	public PolicyRuleExternalizationOptions() {
		this(FULL, false, true);
	}

	public PolicyRuleExternalizationOptions(PolicyTriggerStorageStrategyType triggerStorageStrategy,
			boolean includeAssignmentsContent, boolean respectFinalFlag) {
		this.triggerStorageStrategy = triggerStorageStrategy != null ? triggerStorageStrategy : FULL;
		this.includeAssignmentsContent = includeAssignmentsContent;
		this.respectFinalFlag = respectFinalFlag;
	}

	@NotNull
	public PolicyTriggerStorageStrategyType getTriggerStorageStrategy() {
		return triggerStorageStrategy;
	}

	public boolean isIncludeAssignmentsContent() {
		return includeAssignmentsContent;
	}

	public boolean isRespectFinalFlag() {
		return respectFinalFlag;
	}
}
