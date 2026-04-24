/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.repo.common.policy.TriggerBeanPresentationUtil;
import com.evolveum.midpoint.repo.common.policy.TriggerBeanPresentationUtil.TriggerWithData;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.web.page.admin.workflow.dto.EvaluatedTriggerGroupDto.HighlightingInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/** Provides information about trigger(s) that started the approval process which is being displayed. */
public class EvaluatedTriggerDto implements Serializable {

    public static final String F_MESSAGE = "message";
    public static final String F_CHILDREN = "children";

    /** The trigger bean itself. */
    @NotNull private final EvaluatedPolicyRuleTriggerType trigger;

    /** Triggers that caused the {@link #trigger} be fired. See {@link TriggerBeanPresentationUtil} for details. */
    @NotNull private final EvaluatedTriggerGroupDto children;

    /** Whether this trigger should be highlighted, e.g. because it corresponds to the current stage of the process. */
    private final boolean highlighted;

    EvaluatedTriggerDto(TreeNode<TriggerWithData<HighlightingInformation>> node) {
        this.trigger = node.getUserObject().trigger();
        this.children = new EvaluatedTriggerGroupDto(null, node.getChildren());
        this.highlighted = node.getUserObject().additionalData().value;
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
