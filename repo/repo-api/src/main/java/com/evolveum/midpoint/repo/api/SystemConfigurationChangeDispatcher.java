/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Central point of dispatching notifications about changes to the system configuration object.
 *
 * @author mederly
 */
public interface SystemConfigurationChangeDispatcher {

	/**
	 * Dispatches information on system configuration object change.
	 *
	 * Basically this directly pushes information to lower layers (prism, schema, repo, etc), and calls registered
	 * listeners that originate in upper layers.
	 *
	 * @param ignoreVersion If false, the information is dispatched unconditionally. If true, we dispatch the notification only
	 *                      if the system configuration version was really changed. This is to easily support sources that
	 *                      "ping" sysconfig object in regular intervals, e.g. the cluster manager thread.
	 * @param allowNotFound If true, we take non-existence of sysconfig object more easily. To be used e.g. on system init or
	 *                      during tests execution.
	 */
	void dispatch(boolean ignoreVersion, boolean allowNotFound, OperationResult result) throws SchemaException;

	/**
	 * Registers a listener that will be updated on system configuration object changes.
	 */
	void registerListener(SystemConfigurationChangeListener listener);

	/**
	 * Unregisters a listener.
	 */
	void unregisterListener(SystemConfigurationChangeListener listener);

}
