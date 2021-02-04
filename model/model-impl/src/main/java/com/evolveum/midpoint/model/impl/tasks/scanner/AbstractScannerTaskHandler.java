/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks.scanner;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.tasks.AbstractSearchIterativeModelTaskHandler;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;

/**
 * Task handler covering all scanner tasks - i.e. ones that select their objects based on timestamps.
 * Currently these are: validity, trigger, and shadow refresh tasks.
 *
 * @author Radovan Semancik
 */
@Component
public abstract class AbstractScannerTaskHandler
        <TH extends AbstractScannerTaskHandler<TH, TE>,
                TE extends AbstractScannerTaskExecution<TH, TE>>
        extends AbstractSearchIterativeModelTaskHandler<TH, TE> {

    public AbstractScannerTaskHandler(String taskName, String taskOperationPrefix) {
        super(taskName, taskOperationPrefix);
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.SYSTEM;
    }
}
