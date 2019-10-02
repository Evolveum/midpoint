/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
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

	/**
	 * The methods below should be in class like "TaskUtilForProvisioning". For simplicity let's keep them here for now.
 	 */
	public static boolean isDryRun(Task task) throws SchemaException {
		return findExtensionItemValueInThisOrParent(task, SchemaConstants.MODEL_EXTENSION_DRY_RUN, false);
	}

	public static boolean findExtensionItemValueInThisOrParent(Task task, QName path, boolean defaultValue) throws SchemaException {
		Boolean rawValue = findExtensionItemValueInThisOrParent(task, path);
		return rawValue != null ? rawValue : defaultValue;
	}

	private static Boolean findExtensionItemValueInThisOrParent(Task task, QName path) throws SchemaException {
		Boolean value = findExtensionItemValue(task, path);
		if (value != null) {
			return value;
		}
		if (task instanceof RunningTask) {
			RunningTask runningTask = (RunningTask) task;
			if (runningTask.isLightweightAsynchronousTask() && runningTask.getParentForLightweightAsynchronousTask() != null) {
				return findExtensionItemValue(runningTask.getParentForLightweightAsynchronousTask(), path);
			}
		}
		return null;
	}

	private static Boolean findExtensionItemValue(Task task, QName path) throws SchemaException {
		Validate.notNull(task, "Task must not be null.");
		if (!task.hasExtension()) {
			return null;
		}
		PrismProperty<Boolean> item = task.getExtensionPropertyOrClone(ItemName.fromQName(path));
		if (item == null || item.isEmpty()) {
			return null;
		}
		if (item.getValues().size() > 1) {
			throw new SchemaException("Unexpected number of values for option '" + path + "'.");
		}
		return item.getValues().iterator().next().getValue();
	}
}
