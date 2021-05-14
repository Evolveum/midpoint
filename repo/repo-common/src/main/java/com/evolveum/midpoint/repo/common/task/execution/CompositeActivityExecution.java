/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.execution;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Execution of pure- or semi-composite activity.
 */
public interface CompositeActivityExecution extends ActivityExecution {

    /** Executions of child activities. */
    @NotNull List<ActivityExecution> getChildren();

    /** Adds a child activity execution. */
    void addChild(@NotNull ActivityExecution child);
}
