/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Ready-made implementation of {@link SearchBasedActivityExecutionSpecifics} interface: contains {@link #activityExecution}
 * object and some default functionality.
 */
public abstract class BaseSearchBasedExecutionSpecificsImpl<
        O extends ObjectType,
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>>
        extends BaseActivityExecutionSpecificsImpl<WD, AH, SearchBasedActivityExecution<?, WD, AH, ?>>
        implements SearchBasedActivityExecutionSpecifics<O> {

    protected BaseSearchBasedExecutionSpecificsImpl(@NotNull SearchBasedActivityExecution<O, WD, AH, ?> activityExecution) {
        super(activityExecution);
    }
}
