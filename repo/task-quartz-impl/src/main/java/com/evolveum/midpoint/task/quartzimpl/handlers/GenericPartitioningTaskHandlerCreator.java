/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.handlers;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Creates generic partitioning task handler.
 *
 * @author mederly
 *
 */
@Component
public class GenericPartitioningTaskHandlerCreator {

	public static final String HANDLER_URI = TaskConstants.GENERIC_PARTITIONING_TASK_HANDLER_URI;

	@Autowired private TaskManager taskManager;
	@Autowired private PrismContext prismContext;

	@PostConstruct
	private void initialize() {
		taskManager.createAndRegisterPartitioningTaskHandler(HANDLER_URI, this::createPartitionsDefinition);
	}

	private TaskPartitionsDefinition createPartitionsDefinition(Task masterTask) {
		if (masterTask.getWorkManagement() == null || masterTask.getWorkManagement().getPartitions() == null) {
			throw new IllegalStateException("No partitions definition in task " + masterTask);
		}
		return new StaticTaskPartitionsDefinition(masterTask.getWorkManagement().getPartitions(),
				prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class));
	}
}
