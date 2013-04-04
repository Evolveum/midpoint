/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
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
