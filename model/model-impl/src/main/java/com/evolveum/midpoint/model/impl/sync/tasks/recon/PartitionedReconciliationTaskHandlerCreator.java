/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.task.api.StaticTaskPartitionsDefinition;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskPartitionsDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Creates the task handler for partitioned reconciliation. (A bit awkward idea but it stems from the fact that the handler
 * itself resides in task-quartz-impl. This class serves only as the point of instantiation and configuration of that handler.)
 *
 * ---
 * Task handler created here is responsible for creating subtasks that would deal with particular phases
 * of the reconciliation process:
 *
 * 1. finishing operations
 * 2. iterating through resource objects and synchronizing them
 * 3. iterating through shadows and checking their existence
 */
@Component
public class PartitionedReconciliationTaskHandlerCreator {

    public static final String HANDLER_URI = ModelPublicConstants.PARTITIONED_RECONCILIATION_TASK_HANDLER_URI;

    @Autowired private TaskManager taskManager;
    @Autowired private PrismContext prismContext;

    @PostConstruct
    private void initialize() {
        taskManager.createAndRegisterPartitioningTaskHandler(HANDLER_URI, this::createPartitionsDefinition);
    }

    private TaskPartitionsDefinition createPartitionsDefinition(Task masterTask) {
        TaskPartitionsDefinitionType definitionInTask = masterTask.getWorkManagement() != null ?
                masterTask.getWorkManagement().getPartitions() : null;
        TaskPartitionsDefinitionType partitionsDefinition = definitionInTask != null ?
                definitionInTask.clone() : new TaskPartitionsDefinitionType();
        partitionsDefinition.setCount(3);
        partitionsDefinition.setCopyMasterExtension(true);
        return new StaticTaskPartitionsDefinition(partitionsDefinition,
                prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class));
    }
}
