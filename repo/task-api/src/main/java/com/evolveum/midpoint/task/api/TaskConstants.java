/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

/**
 * @author mederly
 */
public class TaskConstants {

    // TODO reconsider when definitely placing these task handlers
    public static final String WORKERS_RESTART_TASK_HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/workers-restart/handler-3";
    public static final String WORKERS_CREATION_TASK_HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/workers-creation/handler-3";
    public static final String GENERIC_PARTITIONING_TASK_HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/generic-partitioning/handler-3";
    public static final String LIGHTWEIGHT_PARTITIONING_TASK_HANDLER_URI_DEPRECATED = "http://midpoint.evolveum.com/xml/ns/public/task/lightweigth-partitioning/handler-3";
    public static final String LIGHTWEIGHT_PARTITIONING_TASK_HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/lightweight-partitioning/handler-3";

    public static final String NOOP_TASK_HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/noop/handler-3";
    public static final String NOOP_TASK_HANDLER_URI_1 = NOOP_TASK_HANDLER_URI + "#1";
    public static final String NOOP_TASK_HANDLER_URI_2 = NOOP_TASK_HANDLER_URI + "#2";
    public static final String NOOP_TASK_HANDLER_URI_3 = NOOP_TASK_HANDLER_URI + "#3";
    public static final String NOOP_TASK_HANDLER_URI_4 = NOOP_TASK_HANDLER_URI + "#4";

    public static final String JDBC_PING_HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/jdbc-ping/handler-3";

    public static final String LIMIT_FOR_OTHER_GROUPS = "*";            // the same as Scheduler.LIMIT_FOR_OTHER_GROUPS
    public static final String LIMIT_FOR_CURRENT_NODE = "#";            // valid only for statically-configured limits!
    public static final String LIMIT_FOR_NULL_GROUP = "_";              // valid only for statically-configured limits!

    public static final String GET_LOCAL_SCHEDULER_INFORMATION_REST_PATH = "/scheduler/information";
    public static final String STOP_LOCAL_SCHEDULER_REST_PATH = "/scheduler/stop";
    public static final String START_LOCAL_SCHEDULER_REST_PATH = "/scheduler/start";
    public static final String STOP_LOCAL_TASK_REST_PATH_PREFIX = "/tasks/";
    public static final String STOP_LOCAL_TASK_REST_PATH_SUFFIX = "/stop";
}
