/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.task.TaskExecution;

class MockOpeningTaskPartExecution extends AbstractMockTaskPartExecution {

    MockOpeningTaskPartExecution(@NotNull TaskExecution taskExecution) {
        super(taskExecution);
    }

}
