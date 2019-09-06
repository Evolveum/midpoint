/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.work;

import com.evolveum.midpoint.schema.util.TaskWorkStateTypeUtil;
import com.evolveum.midpoint.task.api.Task;

import java.util.List;

/**
 * Companion to TaskWorkStateTypeUtil. However, here are methods that cannot be implemented in schema layer
 * e.g. because they require Task objects.
 *
 * @author mederly
 */
public class TaskWorkStateUtil {

	public static Task findWorkerByBucketNumber(List<Task> workers, int sequentialNumber) {
		for (Task worker : workers) {
			if (worker.getWorkState() != null && TaskWorkStateTypeUtil
					.findBucketByNumber(worker.getWorkState().getBucket(), sequentialNumber) != null) {
				return worker;
			}
		}
		return null;
	}

}
