/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.task.api.StaticTaskPartitionsDefinition;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskPartitionsDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author mederly
 *
 */
class PartitionedMockWorkBucketsTaskHandlerCreator {

    private TaskManager taskManager;
    private PrismContext prismContext;

    PartitionedMockWorkBucketsTaskHandlerCreator(TaskManager taskManager,
            PrismContext prismContext) {
        this.taskManager = taskManager;
        this.prismContext = prismContext;
    }

    @SuppressWarnings("Duplicates")
    void initializeAndRegister(String handlerUri) {
        taskManager.createAndRegisterPartitioningTaskHandler(handlerUri, this::createPartitioningDefinition);
    }

    // mimics PartitionedReconciliationTaskHandlerCreator
    private TaskPartitionsDefinition createPartitioningDefinition(Task masterTask) {
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
