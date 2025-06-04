/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state;

import com.evolveum.midpoint.repo.common.activity.ActivityTree;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.activity.run.task.ActivityBasedTaskRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class ActivityExecutionWriter {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityExecutionWriter.class);

    @NotNull private final ActivityBasedTaskRun taskRun;

    @NotNull private final CommonTaskBeans beans;

    public ActivityExecutionWriter(@NotNull ActivityBasedTaskRun taskRun, @NotNull CommonTaskBeans beans) {
        this.taskRun = taskRun;
        this.beans = beans;
    }

    public void writeActivityExecution(OperationResult result) throws ActivityRunException {
        ActivityTree tree = taskRun.getActivityTree();


    }
}
