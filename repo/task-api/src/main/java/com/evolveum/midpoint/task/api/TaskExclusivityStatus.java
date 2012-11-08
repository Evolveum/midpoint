/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskExclusivityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskExecutionStatusType;

/**
 * Exclusivity status tells about task "locking" to a particular node.
 * 
 * @author Radovan Semancik
 * 
 */
public enum TaskExclusivityStatus {

	/**
	 * The tasks is being held by one of the IDM nodes. The node is either
	 * executing the task or making some kind of "exclusive" operation on the
	 * task. Only one node may "claim" a task. Claimed tasks has an allocated
	 * *thread* that is used for task execution.
	 */
	CLAIMED,

	/**
	 * The task is free for all nodes to participate. Any node may try to
	 * "claim" the task to execute it or make another step in the task
	 * lifecycle. Released task is not being executed by an IDM node and
	 * therefore does not have an allocated thread.
	 */
	RELEASED;

	public static TaskExclusivityStatus fromTaskType(TaskExclusivityStatusType exclusivityStatus) {
		if (exclusivityStatus == null) {
			return null;
		}
		if (exclusivityStatus == TaskExclusivityStatusType.CLAIMED) {
			return CLAIMED;
		}
		if (exclusivityStatus == TaskExclusivityStatusType.RELEASED) {
			return RELEASED;
		}
		throw new IllegalArgumentException("Unknown exclusivity status type "+exclusivityStatus);
	}

	public TaskExclusivityStatusType toTaskType() {
		if (this==CLAIMED) {
			return TaskExclusivityStatusType.CLAIMED;
		}
		if (this==RELEASED) {
			return TaskExclusivityStatusType.RELEASED;
		}
		throw new IllegalArgumentException("Unknown exclusivity status type "+this);
	}
}
