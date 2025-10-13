/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.model.api.util.EvaluatedPolicyRuleUtil;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class EvaluatedTriggerDto implements Serializable {

    public static final String F_MESSAGE = "message";
    public static final String F_CHILDREN = "children";

    @NotNull private final EvaluatedPolicyRuleTriggerType trigger;
    @NotNull private final EvaluatedTriggerGroupDto children;
    private boolean highlighted;

    public EvaluatedTriggerDto(TreeNode<EvaluatedPolicyRuleUtil.AugmentedTrigger<EvaluatedTriggerGroupDto.HighlightingInformation>> node) {
        this.trigger = node.getUserObject().trigger;
        this.children = new EvaluatedTriggerGroupDto(null, node.getChildren());
        this.highlighted = node.getUserObject().additionalData.value;
    }

    @NotNull
    public EvaluatedPolicyRuleTriggerType getTrigger() {
        return trigger;
    }

    public LocalizableMessageType getMessage() {
        return trigger.getMessage();
    }

    @NotNull
    public EvaluatedTriggerGroupDto getChildren() {
        return children;
    }

    public boolean isHighlighted() {
        return highlighted;
    }
}
