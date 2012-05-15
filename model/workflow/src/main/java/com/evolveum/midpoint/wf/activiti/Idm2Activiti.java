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

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.communication.workflow_1.*;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.*;
import org.activiti.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 *  Transports messages from midPoint to Activiti. (Originally via Camel, currently using direct java calls.)
 */

@Component
public class Idm2Activiti {

    @Autowired(required = true)
    ActivitiEngine activitiEngine;

    @Autowired(required = true)
    Activiti2Idm activiti2Idm;

    private static final Trace LOGGER = TraceManager.getTrace(Idm2Activiti.class);

    /**
     * Converts an XML StartProcessInstanceCommand message coming from midPoint
     * into a activiti process-starting message.
     *
     * Namely, creates these variables:
     *  - midpointTaskOid (from XML message)
     *  - midpointListener (by instantiating the appropriate class)
     *  - all variables contained within incoming XML message
     */

    public void idm2activiti(Object o) {

        LOGGER.trace(" *** A message from midPoint has arrived; class = " + o.getClass().getName() + " ***");

        if (o instanceof WfQueryProcessInstanceCommandType)
        {
            WfQueryProcessInstanceCommandType qpic = (WfQueryProcessInstanceCommandType) o;
            String id = qpic.getWfProcessInstanceId();
            LOGGER.trace("Querying process instance id = " + id);

            HistoryService hs = activitiEngine.getHistoryService();
            HistoricDetailQuery hdq = hs.createHistoricDetailQuery()
                    .variableUpdates()
                    .processInstanceId(id)
                    .orderByVariableRevision().desc();

            TreeMap<String,String> variables = new TreeMap<String,String>();		// sorted in order to provide deterministic answer
            for (HistoricDetail hd : hdq.list())
            {
                HistoricVariableUpdate hvu = (HistoricVariableUpdate) hd;
                String varname = hvu.getVariableName();
                Object value = hvu.getValue();
                String varvalue = value == null ? null : value.toString();
                LOGGER.trace("hvu: " + varname + " <- " + varvalue);
                System.out.println("Variable: " + varname + " <- " + varvalue);
                if (!variables.containsKey(varname))
                    variables.put(varname, varvalue);
            }

            HistoricDetailQuery hdq2 = hs.createHistoricDetailQuery()
                    .formProperties()
                    .processInstanceId(id)
                    .orderByVariableRevision().desc();
            for (HistoricDetail hd : hdq2.list())
            {
                HistoricFormProperty hfp = (HistoricFormProperty) hd;
                String varname = hfp.getPropertyId();
                Object value = hfp.getPropertyValue();
                String varvalue = value == null ? null : value.toString();
                LOGGER.trace("form-property: " + varname + " <- " + varvalue);
                System.out.println("form-property: " + varname + " <- " + varvalue);
                if (!variables.containsKey(varname))
                    variables.put(varname, varvalue);
            }

            // obsolete (wfAnswer)
//            String answerValue = variables.get("wfAnswer");
//            LOGGER.trace("wfAnswer value = " + answerValue);
//            System.out.println("wfAnswer value = " + answerValue);

            // is the process still running (needed if value == null)
//            if (answerValue == null)
//            {
//                HistoricProcessInstance hip = hs.createHistoricProcessInstanceQuery()
//                        .processInstanceId(id).singleResult();
//                if (hip != null)
//                {
//                    LOGGER.trace("Found historic process instance with id " + hip.getId() + ", end time = " + hip.getEndTime());
//                    if (hip.getEndTime() != null)
//                        answerValue = "no-result";
//                }
//                else
//                    LOGGER.trace("No historic process instance with id " + id + " was found.");
//            }

            WfProcessInstanceEventType event = new WfProcessInstanceEventType();
            event.setMidpointTaskOid(qpic.getMidpointTaskOid());
            event.setWfProcessInstanceId(id);
//            event.setWfAnswer(answerValue);
            event.setAnswerToQuery(true);

            for (String v : variables.keySet())
            {
                WfProcessVariable pv = new WfProcessVariable();
                pv.setName(v);
                pv.setValue(variables.get(v));
                event.getWfProcessVariable().add(pv);
            }

            LOGGER.trace("Event to be sent to IDM: " + event);
            activiti2Idm.onWorkflowMessage(event);
        }
        else if (o instanceof WfStartProcessInstanceCommandType)
        {
            WfStartProcessInstanceCommandType spic = (WfStartProcessInstanceCommandType) o;

            Map<String,Object> map = new HashMap<String,Object>();

            LOGGER.trace("midpointTaskOid = " + spic.getMidpointTaskOid());

            map.put("midpointTaskOid", spic.getMidpointTaskOid());
            map.put("midpointListener", new IdmExecutionListenerProxy());
            for (WfProcessVariable var : spic.getWfProcessVariable())
            {
                LOGGER.trace("process variable: " + var.getName() + " = " + var.getValue());
                map.put(var.getName(), var.getValue());
            }

            LOGGER.trace("process name = " + spic.getWfProcessName());

            RuntimeService rs = ProcessEngines.getDefaultProcessEngine().getRuntimeService();
            ProcessInstance pi = rs.startProcessInstanceByKey(spic.getWfProcessName(), map);

            // let us send a reply back (useful for listener-free processes)
            // TODO for processes with listeners this is not good, as (typically) this "process started"
            // event comes *after* first internal process event (so the resulting event list looks illogical)
            // We should add a "send confirmation" flag to StartCommand, by which midpoint will enable/disable this message

            if (spic.isSendStartConfirmation()) {
                WfProcessInstanceStartedEventType event = new WfProcessInstanceStartedEventType();
                event.setMidpointTaskOid(spic.getMidpointTaskOid());
                event.setWfProcessInstanceId(pi.getProcessInstanceId());
                event.getWfProcessVariable().addAll(spic.getWfProcessVariable());

                LOGGER.info("Event to be sent to IDM: " + event);
                activiti2Idm.onWorkflowMessage(event);
            }
        }
        else
        {
            String message = "Unknown incoming message type: " + o.getClass().getName();
            LOGGER.error(message);
        }
    }

}
