/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.traces.operations;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.traces.OpNode;
import com.evolveum.midpoint.schema.traces.OpResultInfo;
import com.evolveum.midpoint.schema.traces.TraceInfo;
import com.evolveum.midpoint.schema.traces.TraceUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

public class PolicyRuleEvaluationOpNode extends OpNode {

    public PolicyRuleEvaluationOpNode(PrismContext prismContext,
            OperationResultType result,
            OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
    }

    public boolean isTriggered() {
        return Boolean.parseBoolean(getReturn("triggered"));
    }

    public String getRuleInfo() {
        String rule = getParameter(OperationResult.PARAM_POLICY_RULE);
        String triggeredSuffix = isTriggered() ? getEnabledActionsCount() + " enabled action(s)" : "";

        return rule + " ⇒ " + triggeredSuffix;
    }

    private int getEnabledActionsCount() {
        return TraceUtil.getReturnsAsStringList(result, "enabledActions").size();
    }

    @Override
    protected void postProcess() {
        setDisabled(!isTriggered());
    }
}
