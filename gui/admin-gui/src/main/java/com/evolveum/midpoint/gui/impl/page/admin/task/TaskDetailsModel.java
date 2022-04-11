/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.task;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TaskDetailsModel extends AssignmentHolderDetailsModel<TaskType> {

    private static final Trace LOGGER = TraceManager.getTrace(TaskDetailsModel.class);

    private static final String OPERATION_LOAD_TASK_ROOT = TaskDetailsModel.class.getName() + ".loadTaskRoot";


    private LoadableModel<TaskType> rootTaskModel;

    public TaskDetailsModel(LoadableDetachableModel<PrismObject<TaskType>> prismObjectModel, ModelServiceLocator serviceLocator) {
        super(prismObjectModel, serviceLocator);

        rootTaskModel = createRootTaskModel(this::getObjectType, () -> (PageBase) serviceLocator);
    }

    public @NotNull LoadableModel<TaskType> createRootTaskModel(
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

    @Override
    public void reset() {
        super.reset();
        rootTaskModel.reset();
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

    public LoadableModel<TaskType> getRootTaskModel() {
        return rootTaskModel;
    }

    public TaskType getRootTaskModelObject() {
        return rootTaskModel.getObject();
    }

    @Override
    public void detach() {
        rootTaskModel.detach();

        super.detach();
    }
}
