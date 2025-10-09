/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.api;

public class TaskManagerInitializationException extends TaskManagerException {
    public TaskManagerInitializationException() {
        super();
    }

    public TaskManagerInitializationException(Throwable cause) {
        super(cause);
    }

    public TaskManagerInitializationException(String message) {
        super(message);
    }

    public TaskManagerInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
