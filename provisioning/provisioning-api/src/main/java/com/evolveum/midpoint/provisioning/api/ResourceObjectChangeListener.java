/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * @author Radovan Semancik
 *
 */
public interface ResourceObjectChangeListener extends ProvisioningListener {

	String CLASS_NAME_WITH_DOT = ResourceObjectChangeListener.class.getName() + ".";
	String NOTIFY_CHANGE = CLASS_NAME_WITH_DOT + "notifyChange";
	String CHECK_SITUATION = CLASS_NAME_WITH_DOT + "checkSituation";

	/**
	 * Submits notification about a specific change that happened on the
	 * resource.
	 *
	 * This describes the change that has already happened on the resource. The upper layers are
	 * notified to take that change into an account (synchronize it).
	 *
	 * The call should return without a major delay. It means that the
	 * implementation can do calls to repository, but it should not
	 * (synchronously) initiate a long-running process or provisioning request.
	 *
	 * This operation may be called multiple times with the same change, e.g. in
	 * case of failures in IDM or on the resource. The implementation must be
	 * able to handle such duplicates.
	 *
	 * @param change
	 *            change description
	 */
	public void notifyChange(ResourceObjectShadowChangeDescription change, Task task, OperationResult parentResult);

}
