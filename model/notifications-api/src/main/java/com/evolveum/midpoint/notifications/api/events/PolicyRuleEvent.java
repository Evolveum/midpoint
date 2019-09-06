/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
