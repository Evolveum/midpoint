/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

public class TaskConstants {

    public static final String JDBC_PING_HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/jdbc-ping/handler-3";

    public static final String LIMIT_FOR_OTHER_GROUPS = "*";            // the same as Scheduler.LIMIT_FOR_OTHER_GROUPS
    public static final String LIMIT_FOR_CURRENT_NODE = "#";            // valid only for statically-configured limits!
    public static final String LIMIT_FOR_NULL_GROUP = "_";              // valid only for statically-configured limits!

    public static final String GET_LOCAL_SCHEDULER_INFORMATION_REST_PATH = "/scheduler/information";
    public static final String STOP_LOCAL_SCHEDULER_REST_PATH = "/scheduler/stop";
    public static final String START_LOCAL_SCHEDULER_REST_PATH = "/scheduler/start";
    public static final String STOP_LOCAL_TASK_REST_PATH_PREFIX = "/tasks/";
    public static final String STOP_LOCAL_TASK_REST_PATH_SUFFIX = "/stop";

    public static final String GET_TASK_REST_PATH = "/tasks/";
}
