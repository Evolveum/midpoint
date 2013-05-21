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

package com.evolveum.midpoint.wf.activiti;

import java.io.Serializable;

import org.activiti.engine.impl.pvm.delegate.ExecutionListenerExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evolveum.midpoint.xml.ns._public.communication.workflow_1.WfProcessInstanceEventType;

/**
 * Currently unused. Necessary for "smart" workflow tasks.
 */

public class IdmExecutionListenerProxy implements Serializable {

	private static Logger logger = LoggerFactory.getLogger(IdmExecutionListenerProxy.class);
	
	private static final long serialVersionUID = 1L;

	public void notify(ExecutionListenerExecution execution) throws Exception {
		notify(execution, "", null);
	}

	public void notify(ExecutionListenerExecution execution, String description) throws Exception {
		notify(execution, description, null);
	}

	public void notify(ExecutionListenerExecution execution, String description, String answer)
			throws Exception {

		IdmExecutionListener l = SpringApplicationContextHolder
				.getApplicationContext().
				getBean("idmExecutionListenerBean", IdmExecutionListener.class);
	
		if (l == null)
			logger.error("idmExecutionListenerBean could not be found.");
		else
		{
            if (logger.isTraceEnabled()) {
			    logger.trace("idmExecutionListenerBean found: " + l + ", calling notify(...)");
            }
			l.notify(execution, description, answer);
		}
	}

	public void notify(WfProcessInstanceEventType event) throws Exception
	{
		IdmExecutionListener l = SpringApplicationContextHolder
			.getApplicationContext().
			getBean("idmExecutionListenerBean", IdmExecutionListener.class);

		if (l == null)
			logger.error("idmExecutionListenerBean could not be found.");
		else
		{
            if (logger.isTraceEnabled()) {
			    logger.trace("idmExecutionListenerBean found: " + l + ", calling notify(...)");
            }
			l.notify(event);
		}
	}
}
