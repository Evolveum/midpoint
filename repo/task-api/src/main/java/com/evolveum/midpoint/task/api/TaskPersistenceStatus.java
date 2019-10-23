/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
