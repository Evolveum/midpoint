/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 27.4.2012
 * Time: 20:59
 * To change this template use File | Settings | File Templates.
 */
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
