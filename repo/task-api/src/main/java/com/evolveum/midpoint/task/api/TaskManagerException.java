/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.api;

public class TaskManagerException extends Exception {
    public TaskManagerException() {
        super();
    }

    public TaskManagerException(Throwable cause) {
        super(cause);
    }

    public TaskManagerException(String message) {
        super(message);
    }

    public TaskManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
