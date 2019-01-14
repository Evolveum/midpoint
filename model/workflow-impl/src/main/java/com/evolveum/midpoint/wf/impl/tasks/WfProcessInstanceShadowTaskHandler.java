/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.wf.impl.tasks;

import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
@DependsOn({ "taskManager" })
public class WfProcessInstanceShadowTaskHandler implements TaskHandler {

	public static final String HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/workflow/process-instance-shadow/handler-3";
    private static final String DOT_CLASS = WfProcessInstanceShadowTaskHandler.class.getName() + ".";

    @Autowired
	private TaskManager taskManager;

    private static final Trace LOGGER = TraceManager.getTrace(WfProcessInstanceShadowTaskHandler.class);

    @PostConstruct
    public void init() {
        LOGGER.trace("Registering with taskManager as a handler for " + HANDLER_URI);
        taskManager.registerHandler(HANDLER_URI, this);
    }

	@Override
	public TaskRunResult run(Task task) {
		// this task handler serves only as a marker -- to be resolved later
		throw new UnsupportedOperationException("Operation not supported.");
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.WORKFLOW;
    }
}
