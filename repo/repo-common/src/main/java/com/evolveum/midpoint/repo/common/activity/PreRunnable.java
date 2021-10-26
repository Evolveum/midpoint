/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;

import org.jetbrains.annotations.NotNull;

/**
 * A code that executes before local or distributing activity run takes place.
 */
@FunctionalInterface
public interface PreRunnable<WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>> {

    void run(@NotNull EmbeddedActivity<WD, AH> activity, @NotNull RunningTask runningTask, @NotNull OperationResult result)
            throws CommonException, ActivityRunException;
}
