/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.provisioning.ucf.api;

/**
 * Processes changes encountered on a resource
 */
public interface ChangeListener {

	/**
	 * Called when the connector learns about a resource change.
	 * @param change The change.
	 * @return true if the change was successfully processed and can be acknowledged on the resource;
	 * false (or a runtime exception) should be returned otherwise
	 *
	 * TODO add operation result here? Beware of simultaneous firing of changes. OperationResult is not thread-safe yet.
	 */
	boolean onChange(Change change);
}
