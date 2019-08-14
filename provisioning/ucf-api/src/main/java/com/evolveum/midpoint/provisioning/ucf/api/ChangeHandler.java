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

import com.evolveum.midpoint.schema.result.OperationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Processes changes detected on a resource.
 *
 * Similar to ChangeListener.
 *
 * Differences:
 *  - semantics of handleChange return value
 *  - the presence of handleChange OperationResult parameter
 *  - handleError method
 *
 * Is this class eligible to be merged with ChangeListener? Probably not.
 */
public interface ChangeHandler {

	/**
	 * Called when given change has to be processed
	 * @param change The change.
	 * @return false if the processing of changes has to be stopped
	 */
	boolean handleChange(Change change, OperationResult result);

	/**
	 * Called when given change cannot be prepared for processing (or created altogether).
	 * @param change The change, if determinable.
	 * @param exception Exception encountered.
	 * @param result Context of the operation.
	 * @return false if the processing of changes has to be stopped (this is the usual case)
	 */
	boolean handleError(@Nullable Change change, @NotNull Throwable exception, @NotNull OperationResult result);
}
