/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers;

import com.evolveum.midpoint.repo.common.task.TaskExecution;

import org.jetbrains.annotations.NotNull;

class MockClosingTaskPartExecution extends AbstractMockTaskPartExecution {

    MockClosingTaskPartExecution(@NotNull TaskExecution taskExecution) {
        super(taskExecution);
    }

}
