/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks.scanner;

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
 * TODO
 */
@Component
public class PartitionedFocusValidityScannerTaskHandlerCreator {

    @Autowired private TaskManager taskManager;
    @Autowired private PrismContext prismContext;

    @PostConstruct
    private void initialize() {
        taskManager.createAndRegisterPartitioningTaskHandler(ModelPublicConstants.PARTITIONED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI,
                this::createPartitionsDefinition);
    }

    private TaskPartitionsDefinition createPartitionsDefinition(Task masterTask) {
        TaskPartitionsDefinitionType definitionInTask = masterTask.getWorkManagement() != null ?
                masterTask.getWorkManagement().getPartitions() : null;
        TaskPartitionsDefinitionType partitionsDefinition = definitionInTask != null ?
                definitionInTask.clone() : new TaskPartitionsDefinitionType();
        partitionsDefinition.setCount(2);
        partitionsDefinition.setDurablePartitions(true);
        partitionsDefinition.setCopyMasterExtension(true);
        return new StaticTaskPartitionsDefinition(partitionsDefinition,
                prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class));
    }
}
