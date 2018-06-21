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

/**
 * EXPERIMENTAL
 *
 * @author mederly
 */
public class TaskWorkBucketProcessingResult extends TaskRunResult {

	private boolean bucketComplete;         // "bucket not complete" implies "should not continue"
	private boolean shouldContinue;

	public boolean isBucketComplete() {
		return bucketComplete;
	}

	public void setBucketComplete(boolean bucketComplete) {
		this.bucketComplete = bucketComplete;
	}

	public boolean isShouldContinue() {
		return shouldContinue;
	}

	public void setShouldContinue(boolean shouldContinue) {
		this.shouldContinue = shouldContinue;
	}

	@Override
	public String toString() {
		return "TaskWorkBucketProcessingResult{" +
				"bucketComplete=" + bucketComplete +
				", shouldContinue=" + shouldContinue +
				", progress=" + progress +
				", runResultStatus=" + runResultStatus +
				", operationResult=" + operationResult +
				'}';
	}
}
