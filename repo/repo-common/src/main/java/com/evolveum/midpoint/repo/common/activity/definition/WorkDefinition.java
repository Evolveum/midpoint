/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;

/**
 * Defines the work that is to be done within an activity.
 */
public interface WorkDefinition extends DebugDumpable, Cloneable {

    @NotNull ExecutionModeType getExecutionMode();

    // TODO decide on this
    @NotNull ActivityTailoring getActivityTailoring();

    WorkDefinition clone();
}
