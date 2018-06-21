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
package com.evolveum.midpoint.task.api;

/**
 * Task persistence status.
 *
 * Persistence status tells whether the task is in-memory or persisted in the
 * repository.
 *
 * @author Radovan Semancik
 *
 */
public enum TaskPersistenceStatus {

	/**
	 * The task is in-memory only, it is not stored in the repository. Only
	 * synchronous foreground tasks may use this approach. As the task data only
	 * exists while the task is being executed, the user or the client
	 * application needs to (synchronously) wait for a task to complete.
	 */
	TRANSIENT,

	/**
	 * The task is stored in the repository. Both synchronous (foreground) and
	 * asynchronous (background, scheduled, etc.) tasks may use this approach,
	 * however it is used almost exclusively for asynchronous tasks.
	 */
	PERSISTENT
}
