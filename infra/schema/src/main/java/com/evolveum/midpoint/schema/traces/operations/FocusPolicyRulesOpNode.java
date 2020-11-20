/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.operations;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.traces.OpNode;
import com.evolveum.midpoint.schema.traces.OpResultInfo;
import com.evolveum.midpoint.schema.traces.TraceInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class FocusPolicyRulesOpNode extends ProjectorComponentOpNode {

    private List<PolicyRuleEvaluationOpNode> evaluations = new ArrayList<>();

    public FocusPolicyRulesOpNode(PrismContext prismContext, OperationResultType result, OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
    }

    @Override
    protected void postProcess() {
        evaluations = getNodesDownwards(PolicyRuleEvaluationOpNode.class, Integer.MAX_VALUE); // todo really that deep?
        setDisabled(getTriggeredRulesCount() == 0);
    }

    public int getEvaluatedRulesCount() {
        return evaluations.size();
    }

    public int getTriggeredRulesCount() {
        return (int) evaluations.stream()
                .filter(PolicyRuleEvaluationOpNode::isTriggered)
                .count();
    }


    public String getFocusPolicyRulesInfo() {
        return "(" + getTriggeredRulesCount() + "/" + getEvaluatedRulesCount() + ")";
    }
}
