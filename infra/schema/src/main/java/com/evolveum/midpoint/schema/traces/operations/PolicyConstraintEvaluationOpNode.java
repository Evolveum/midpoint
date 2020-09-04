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
import com.evolveum.midpoint.schema.traces.TraceUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PolicyConstraintEvaluationOpNode extends OpNode {

    private static final Pattern NAME_PATTERN = Pattern.compile("com\\.evolveum\\.midpoint\\.model\\.impl\\.lens\\.projector\\.policy\\.evaluators\\.(.*)ConstraintEvaluator\\.evaluate");

    private final String constraintName;
    private final String trigger;

    public PolicyConstraintEvaluationOpNode(PrismContext prismContext,
            OperationResultType result,
            OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
        constraintName = determineConstraintName();
        trigger = getReturn("trigger");
    }

    private String determineConstraintName() {
        String operation = result.getOperation();

        Matcher matcher = NAME_PATTERN.matcher(operation);
        if (matcher.matches()) {
            String constraintRaw = matcher.group(1);
            if (StringUtils.isNotEmpty(constraintRaw)) {
                return Character.toLowerCase(constraintRaw.charAt(0)) + constraintRaw.substring(1);
            }
        }
        return operation;
    }

    public String getConstraintInfo() {
        return constraintName + " ⇒ " + trigger;
    }

    public String getConstraintName() {
        return constraintName;
    }

    public String getTrigger() {
        return trigger;
    }

    public boolean isTriggered() {
        return StringUtils.isNotEmpty(trigger);
    }

    public String getRuleInfo() {
        String rule = getParameter("policyRule");
        String triggeredSuffix = isTriggered() ? " # (" + getEnabledActionsCount() + ")" : "";

        return rule + triggeredSuffix;
    }

    private int getEnabledActionsCount() {
        return TraceUtil.getReturnsAsStringList(result, "enabledActions").size();
    }

    @Override
    protected void postProcess() {
        setDisabled(!isTriggered());
    }
}
