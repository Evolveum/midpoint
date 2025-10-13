/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.api;

public class TaskManagerConfigurationException extends TaskManagerInitializationException {

    public TaskManagerConfigurationException() {
        super();
    }

    public TaskManagerConfigurationException(Throwable cause) {
        super(cause);
    }

    public TaskManagerConfigurationException(String message) {
        super(message);
    }

    public TaskManagerConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
