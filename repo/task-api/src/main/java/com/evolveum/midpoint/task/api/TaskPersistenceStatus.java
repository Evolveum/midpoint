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
