/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.task;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Responsible for creating a model for a root task (given any task in the task tree).
 *
 * TODO clean up as needed
 * TODO optimize # of loads of the root task when navigating through the tree
 */
@Experimental
public class RootTaskLoader {

    private static final Trace LOGGER = TraceManager.getTrace(RootTaskLoader.class);

    private static final String OPERATION_LOAD_TASK_ROOT = RootTaskLoader.class.getName() + ".loadTaskRoot";

    public static @NotNull LoadableModel<TaskType> createRootTaskModel(
            SerializableSupplier<TaskType> taskSupplier,
            SerializableSupplier<PageBase> pageBaseSupplier) {
        return LoadableModel.create(() -> {
            TaskType task = taskSupplier.get();
            if (task == null || task.getOid() == null) {
                return null;
            } else if (task.getParent() == null) {
                return task;
            } else {
                return doLoadRoot(task, pageBaseSupplier);
            }
        }, false);
    }

    /**
     * Loads root task from the repository. Returns null if task cannot be loaded.
     *
     * Precondition: task is persistent.
     */
    private static @Nullable TaskType doLoadRoot(@NotNull TaskType taskBean, SerializableSupplier<PageBase> pageBaseSupplier) {
        PageBase pageBase = pageBaseSupplier.get();
        Task opTask = pageBase.createSimpleTask(OPERATION_LOAD_TASK_ROOT);
        OperationResult result = opTask.getResult();
        try {
            return pageBase.getTaskManager()
                    .createTaskInstance(taskBean.asPrismObject(), result)
                    .getRoot(result)
                    .getUpdatedTaskObject().asObjectable();
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Couldn't determine root for {}", e, taskBean);
            return null;
        }
    }
}
