/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

/**
 * Signals a
 * @author Pavol Mederly
 */
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
