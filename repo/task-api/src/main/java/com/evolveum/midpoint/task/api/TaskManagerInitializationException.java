/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
