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

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;
import org.jetbrains.annotations.NotNull;

/**
 * Any event that is triggered by the 'notify' policy rule action.
 *
 * @author mederly
 */
public class PolicyRuleEvent extends BaseEvent {

    @NotNull private final EvaluatedPolicyRule policyRule;

    public PolicyRuleEvent(@NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator, @NotNull EvaluatedPolicyRule policyRule) {
        super(lightweightIdentifierGenerator);
        this.policyRule = policyRule;
    }

    @Override
    public boolean isRelatedToItem(ItemPath itemPath) {
        return false;           // not supported for this kind of events
    }

    @Override
    public boolean isStatusType(EventStatusType eventStatusType) {
        return eventStatusType == EventStatusType.SUCCESS || eventStatusType == EventStatusType.ALSO_SUCCESS;
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperationType) {
        return eventOperationType == EventOperationType.ADD;
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategoryType) {
        return eventCategoryType == EventCategoryType.POLICY_RULE_EVENT;
    }

    @NotNull
    public EvaluatedPolicyRule getPolicyRule() {
        return policyRule;
    }

	public String getRuleName() {
		return policyRule.getName();
	}
	
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
		debugDumpCommon(sb, indent);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "policyRule", policyRule, indent + 1);
		return sb.toString();
	}
}
