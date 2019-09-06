/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mederly
 */
public class TaskUtil {

	public static List<String> tasksToOids(List<Task> tasks) {
		return tasks.stream().map(Task::getOid).collect(Collectors.toList());
	}

//	public static TaskWorkManagementType adaptHandlerUri(PrismProperty<TaskWorkManagementType> cfg,
//			String handlerUri) {
//		if (cfg == null || cfg.getRealValue() == null) {
//			return null;
//		} else if (cfg.getRealValue().getWorkers() != null && cfg.getRealValue().getWorkers().getHandlerUri() != null) {
//			return cfg.getRealValue();
//		} else {
//			TaskWorkManagementType clone = cfg.getRealValue().clone();
//			if (clone.getWorkers() == null) {
//				clone.setWorkers(new WorkersManagementType());
//			}
//			clone.getWorkers().setHandlerUri(handlerUri);
//			return clone;
//		}
//	}

	public static Task findByIdentifier(@NotNull String identifier, @NotNull Collection<Task> tasks) {
		return tasks.stream().filter(t -> identifier.equals(t.getTaskIdentifier())).findFirst().orElse(null);
	}
}
