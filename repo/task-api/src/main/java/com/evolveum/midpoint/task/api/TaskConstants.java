/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

	public static final String NOOP_TASK_HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/noop/handler-3";
	public static final String NOOP_TASK_HANDLER_URI_1 = NOOP_TASK_HANDLER_URI + "#1";
	public static final String NOOP_TASK_HANDLER_URI_2 = NOOP_TASK_HANDLER_URI + "#2";
	public static final String NOOP_TASK_HANDLER_URI_3 = NOOP_TASK_HANDLER_URI + "#3";
	public static final String NOOP_TASK_HANDLER_URI_4 = NOOP_TASK_HANDLER_URI + "#4";

	public static final String JDBC_PING_HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/jdbc-ping/handler-3";

	public static final String LIMIT_FOR_OTHER_GROUPS = "*";            // the same as Scheduler.LIMIT_FOR_OTHER_GROUPS
}
