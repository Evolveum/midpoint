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

import javax.xml.bind.JAXBElement;

import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.pvm.delegate.ExecutionListenerExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evolveum.midpoint.xml.ns._public.communication.workflow_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.communication.workflow_1.WfProcessInstanceEventType;
import com.evolveum.midpoint.xml.ns._public.communication.workflow_1.WfProcessInstanceFinishedEventType;
import com.evolveum.midpoint.xml.ns._public.communication.workflow_1.WfProcessInstanceStartedEventType;
import com.evolveum.midpoint.xml.ns._public.communication.workflow_1.WfProcessVariable;

/**
 * Currently unused. Necessary for "smart" workflow tasks.
 */

public class IdmExecutionListener {
	
	private static Logger logger = LoggerFactory.getLogger(IdmExecutionListener.class);

	public void notify(ExecutionListenerExecution execution) throws Exception {
		notify(execution, "", null);
	}

	public void notify(ExecutionListenerExecution execution, String description) throws Exception {
		notify(execution, "", null);
	}
	
	public void notify(ExecutionListenerExecution execution, String description, String wfAnswer) throws Exception {

		if (execution == null)
		{
			logger.error("Variable 'execution' (ExecutionListenerExecution) cannot be null, please check your listener configuration.");
			return;
		}
		
		String eventName = execution.getEventName();
		String pid = execution.getProcessInstanceId();
		Object midpointTaskOid = execution.getVariable("midpointTaskOid");
		if (wfAnswer == null)
			wfAnswer = (String) execution.getVariable("wfAnswer");
		
		logger.info("** PROCESS EXECUTION EVENT: " + eventName + " **");
		logger.info("Process instance id = " + pid);
		logger.info("Description = " + description);
		logger.info("Answer = " + wfAnswer);
		
		WfProcessInstanceEventType event;
				
		if (ExecutionListener.EVENTNAME_START.equals(eventName))
			event = new WfProcessInstanceStartedEventType();
		else if (ExecutionListener.EVENTNAME_END.equals(eventName))
			event = new WfProcessInstanceFinishedEventType();
		else 
			event = new WfProcessInstanceEventType();
			
		event.setWfProcessInstanceId(pid);
		event.setWfAnswer(wfAnswer);
		
		if (midpointTaskOid != null)
			event.setMidpointTaskOid(midpointTaskOid.toString());
		else
			logger.error("Process variable midpointTaskOid is not set!");
		
		event.setWfStateDescription(description);
		
		for (String v : execution.getVariableNames())
		{
			logger.info("Variable " + v + " = " + execution.getVariable(v));
			WfProcessVariable pv = new WfProcessVariable();
			pv.setName(v);
			Object o = execution.getVariable(v);
			pv.setValue(o != null ? o.toString() : null);
			event.getWfProcessVariable().add(pv);
		}
		logger.info("(end of event data, sending camel message)");
		
		//producerToIdm.sendBody(marshal(event));
		logger.info("(camel message sent)");
	}
	
	public void notify(WfProcessInstanceEventType event) throws Exception
	{
		//producerToIdm.sendBody(marshal(event));
		logger.info("(custom camel message sent)");
	}

	private String marshal(WfProcessInstanceEventType event) throws Exception
	{
		ObjectFactory of = new ObjectFactory();
		JAXBElement<?> jaxbElement;
		if (event instanceof WfProcessInstanceStartedEventType)
			jaxbElement = of.createWfProcessInstanceStartedEvent((WfProcessInstanceStartedEventType) event);
		else if (event instanceof WfProcessInstanceFinishedEventType)
			jaxbElement = of.createWfProcessInstanceFinishedEvent((WfProcessInstanceFinishedEventType) event);
		else
			jaxbElement = of.createWfProcessInstanceEvent(event);
		
		return //JAXBUtil.marshal(jaxbElement);
        null;
	}
}
