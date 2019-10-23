/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
