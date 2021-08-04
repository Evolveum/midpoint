/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;

import org.jetbrains.annotations.NotNull;

/**
 * Ready-made implementation class for {@link PlainIterativeActivityExecutionSpecifics}.
 *
 * @param <I> Items being iterated on
 * @param <WD> Work definition type
 * @param <AH> Activity handler type
 */
public abstract class BasePlainIterativeExecutionSpecificsImpl<
        I,
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>>
        extends BaseActivityExecutionSpecificsImpl<WD, AH, PlainIterativeActivityExecution<I, WD, AH, ?>>
        implements PlainIterativeActivityExecutionSpecifics<I> {

    protected BasePlainIterativeExecutionSpecificsImpl(@NotNull PlainIterativeActivityExecution<I, WD, AH, ?> activityExecution) {
        super(activityExecution);
    }

    public @NotNull ProcessingCoordinator<I> getProcessingCoordinator() {
        return activityExecution.getCoordinator();
    }
}
