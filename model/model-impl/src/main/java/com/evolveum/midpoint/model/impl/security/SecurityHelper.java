/**
 * Copyright (c) 2015-2016 Evolveum
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
package com.evolveum.midpoint.model.impl.security;

import javax.xml.soap.SOAPMessage;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 *
 */
@Component
public class SecurityHelper {
	
	private static final Trace LOGGER = TraceManager.getTrace(SecurityHelper.class);

    public static final String CONTEXTUAL_PROPERTY_AUDITED_NAME = SecurityHelper.class.getName() + ".audited";

    @Autowired
    private TaskManager taskManager;
    @Autowired
    private AuditService auditService;

    public void auditLoginSuccess(String username, ConnectionEnvironment connEnv) {
        auditLogin(username, connEnv, OperationResultStatus.SUCCESS, null);
    }

    public void auditLoginFailure(String username, ConnectionEnvironment connEnv, String message) {
        auditLogin(username, connEnv, OperationResultStatus.FATAL_ERROR, message);
    }

    private void auditLogin(String username, ConnectionEnvironment connEnv, OperationResultStatus status,
                            String message) {
        Task task = taskManager.createTaskInstance();
        task.setChannel(connEnv.getChannel());

        LOGGER.debug("Login {} username={}, channel={}: {}",
                new Object[]{status == OperationResultStatus.SUCCESS ? "success" : "failure", username,
                        connEnv.getChannel(), message});

        AuditEventRecord record = new AuditEventRecord(AuditEventType.CREATE_SESSION, AuditEventStage.REQUEST);
        record.setParameter(username);

        record.setChannel(connEnv.getChannel());
        record.setTimestamp(System.currentTimeMillis());
        record.setOutcome(status);
        record.setMessage(message);
        record.setSessionIdentifier(connEnv.getSessionId());

        auditService.audit(record, task);
    }
    
    public void auditLogout(ConnectionEnvironment connEnv, Task task) {
    	AuditEventRecord record = new AuditEventRecord(AuditEventType.TERMINATE_SESSION, AuditEventStage.REQUEST);
		PrismObject<UserType> owner = task.getOwner();
		if (owner != null) {
			record.setInitiator(owner);
			PolyStringType name = owner.asObjectable().getName();
			if (name != null) {
				record.setParameter(name.getOrig());
			}
		}

		record.setChannel(connEnv.getChannel());
		record.setTimestamp(System.currentTimeMillis());
		record.setSessionIdentifier(connEnv.getSessionId());

		record.setOutcome(OperationResultStatus.SUCCESS);

		auditService.audit(record, task);
    }

	public String getUsernameFromMessage(SOAPMessage saajSoapMessage) throws WSSecurityException {
        if (saajSoapMessage == null) {
        	return null;
        }
        Element securityHeader = WSSecurityUtil.getSecurityHeader(saajSoapMessage.getSOAPPart(), "");
        return getUsernameFromSecurityHeader(securityHeader);
	}
	
    private String getUsernameFromSecurityHeader(Element securityHeader) {
    	if (securityHeader == null) {
    		return null;
    	}
    	
        String username = "";
        NodeList list = securityHeader.getChildNodes();
        int len = list.getLength();
        Node elem;
        for (int i = 0; i < len; i++) {
            elem = list.item(i);
            if (elem.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if ("UsernameToken".equals(elem.getLocalName())) {
                NodeList nodes = elem.getChildNodes();
                int len2 = nodes.getLength();
                for (int j = 0; j < len2; j++) {
                    Node elem2 = nodes.item(j);
                    if ("Username".equals(elem2.getLocalName())) {
                        username = elem2.getTextContent();
                    }
                }
            }
        }
        return username;
    }
	
    public SOAPMessage getSOAPMessage(SoapMessage msg) {
        SAAJInInterceptor.INSTANCE.handleMessage(msg);
        return msg.getContent(SOAPMessage.class);
    }
}
