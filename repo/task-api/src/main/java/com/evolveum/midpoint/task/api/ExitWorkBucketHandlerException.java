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

package com.evolveum.midpoint.task.api;

import org.jetbrains.annotations.NotNull;

/**
 * Used to signal that we have to exit handler processing with a given run result.
 *
 * Necessary for creation of separate methods for parts of task handler that need to return something
 * but also exit handler immediately if there's any issue.
 *
 * Experimental.
 *
 * @author mederly
 */
public class ExitWorkBucketHandlerException extends Exception {

	@NotNull private final TaskWorkBucketProcessingResult runResult;

	public ExitWorkBucketHandlerException(@NotNull TaskWorkBucketProcessingResult runResult) {
		this.runResult = runResult;
	}

	@NotNull
	public TaskWorkBucketProcessingResult getRunResult() {
		return runResult;
	}
}
