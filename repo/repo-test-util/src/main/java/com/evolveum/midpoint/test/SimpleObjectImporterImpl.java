/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Imports objects into repository via repository or task manager API. References in the objects are NOT resolved in this mode.
 *
 * Intentionally not a Spring bean. TODO implement this seriously
 */
class SimpleObjectImporterImpl implements ObjectImporter {

    private final RepositoryService repositoryService;
    private final TaskManager taskManager;

    SimpleObjectImporterImpl(RepositoryService repositoryService, TaskManager taskManager) {
        this.repositoryService = repositoryService;
        this.taskManager = taskManager;
    }

    @Override
    public <O extends ObjectType> void importObject(PrismObject<O> object, Task task, OperationResult result)
            throws CommonException {
        // Incomplete; we should handle other object types as well (e.g., nodes)
        // We assume task manager is present; this may or may not be true for very low-level tests
        if (TaskType.class.equals(object.getCompileTimeClass())) {
            //noinspection unchecked
            taskManager.addTask((PrismObject<TaskType>) object, result);
        } else {
            repositoryService.addObject(object, null, result);
        }
    }
}
