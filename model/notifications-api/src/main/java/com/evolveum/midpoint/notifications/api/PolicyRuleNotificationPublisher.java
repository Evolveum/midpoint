/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.api;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Allows explicit publishing of triggered policy rules events.
 *
 * Example use case:
 *
 * This interface can be used as a shortcut to emit policy rules related event notifications
 * before suspending task or skipping/restarting activity policy action. Enforcement of such
 * actions will prevent notification hook to execute and thus notifications in this scenario
 * would not be sent.
 */
public interface PolicyRuleNotificationPublisher {

    /**
     * Emits events related to triggered policy rules. This method should be called before
     * enforcing any policy action that would suspend task or skip/restart activity.
     *
     * By default, such events are processed (emmited) by notification hook.
     */
    void emitPolicyRulesEvents(ModelContext<?> context, Task task, OperationResult result);
}
