/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl;

import com.evolveum.midpoint.notifications.impl.events.ActivityPolicyRuleEventImpl;

import com.evolveum.midpoint.repo.common.activity.policy.EvaluatedActivityPolicyRule;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.ActivityEvent;
import com.evolveum.midpoint.notifications.impl.events.ActivityEventImpl;
import com.evolveum.midpoint.notifications.impl.events.ActivityRealizationCompleteEventImpl;
import com.evolveum.midpoint.repo.common.activity.ActivityListener;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Creates and submits {@link ActivityEvent} instances.
 */
@Component
public class NotificationActivityListener implements ActivityListener {

    @Autowired private NotificationManager notificationManager;

    @Override
    public void onActivityRealizationComplete(
            @NotNull AbstractActivityRun<?, ?, ?> activityRun, @NotNull Task task, @NotNull OperationResult result) {
        ActivityEventImpl event = new ActivityRealizationCompleteEventImpl(activityRun);
        event.setRequesterAndRequesteeAsTaskOwner(task, result);
        notificationManager.processEvent(event, task, result);
    }

    @Override
    public void onActivityPolicyRuleTrigger(
            @NotNull AbstractActivityRun<?, ?, ?> activityRun,
            @NotNull EvaluatedActivityPolicyRule policyRule,
            @NotNull Task task,
            @NotNull OperationResult result) {

        ActivityPolicyRuleEventImpl event = new ActivityPolicyRuleEventImpl(activityRun, policyRule);
        notificationManager.processEvent(event, task, result);
    }
}
