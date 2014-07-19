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
package com.evolveum.midpoint.model.impl.security;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.ws.commons.schema.utils.DOMUtil;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Responsible to inject Spring authentication object before we call WS method
 */
public class SpringAuthenticationInjectorInterceptor implements PhaseInterceptor<SoapMessage> {

	private static final Trace LOGGER = TraceManager.getTrace(SpringAuthenticationInjectorInterceptor.class);
	
    private String phase;
    private Set<String> before = new HashSet<String>();
    private Set<String> after = new HashSet<String>();
    private String id;

    private UserProfileService userDetailsService;
    private SecurityEnforcer securityEnforcer;
    private AuditService auditService;
    private TaskManager taskManager;

    public SpringAuthenticationInjectorInterceptor(UserProfileService userDetailsService,
    		SecurityEnforcer securityEnforcer, AuditService auditService, TaskManager taskManager) {
        super();
        this.userDetailsService = userDetailsService;
        this.securityEnforcer = securityEnforcer;
        this.auditService = auditService;
        this.taskManager = taskManager;
        id = getClass().getName();
        phase = Phase.PRE_PROTOCOL;
        getAfter().add(WSS4JInInterceptor.class.getName());
    }

    @Override
    public Set<String> getAfter() {
        return after;
    }

    @Override
    public Set<String> getBefore() {
        return before;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getPhase() {
        return phase;
    }

    @Override
    public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
        return null;
    }


    private SOAPMessage getSOAPMessage(SoapMessage msg) {
        SAAJInInterceptor.INSTANCE.handleMessage(msg);
        return msg.getContent(SOAPMessage.class);
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        //Note: in constructor we have specified that we will be called after we have been successfully authenticated the user through WS-Security
        //Now we will only set the Spring Authentication object based on the user found in the header
        SOAPMessage doc = getSOAPMessage(message);
        String username = null;
        try {
            Element securityHeader = WSSecurityUtil.getSecurityHeader(doc.getSOAPPart(), "");
            username = getUsernameFromSecurityHeader(securityHeader);

            if (username != null && username.length() > 0) {
            	MidPointPrincipal principal = userDetailsService.getPrincipal(username);
                Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                String operationName;
				try {
					operationName = DOMUtil.getFirstChildElement(doc.getSOAPBody()).getLocalName();
				} catch (SOAPException e) {
					LOGGER.debug("Access to web service denied for user '{}': SOAP error: {}", 
		        			new Object[]{username, e.getMessage(), e});
					auditLoginFailure(username);
					throw new Fault(e);
				}
                String action = QNameUtil.qNameToUri(new QName(AuthorizationConstants.NS_AUTHORIZATION_WS, operationName));
                LOGGER.trace("Determining authorization for web service operation {} (action: {})", operationName, action);
                boolean isAuthorized;
				try {
					isAuthorized = securityEnforcer.isAuthorized(action, AuthorizationPhaseType.REQUEST, null, null, null, null);
				} catch (SchemaException e) {
					LOGGER.debug("Access to web service denied for user '{}': schema error: {}", 
		        			new Object[]{username, e.getMessage(), e});
					auditLoginFailure(username);
					throw new Fault(e);
				}
                if (!isAuthorized) {
                	LOGGER.debug("Access to web service denied for user '{}': not authorized", 
                			new Object[]{username});
                	auditLoginFailure(username);
                	throw new Fault(new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "Not authorized"));
                }
            }
        } catch (WSSecurityException e) {
        	LOGGER.debug("Access to web service denied for user '{}': security exception: {}", 
        			new Object[]{username, e.getMessage(), e});
        	auditLoginFailure(username);
            throw new Fault(e);
        } catch (ObjectNotFoundException e) {
        	LOGGER.debug("Access to web service denied for user '{}': object not found: {}", 
        			new Object[]{username, e.getMessage(), e});
        	auditLoginFailure(username);
            throw new Fault(new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "Not authorized"));
		}

        LOGGER.debug("Access to web service allowed for user '{}'", username);
    }

    private String getUsernameFromSecurityHeader(Element securityHeader) {
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

    @Override
    public void handleFault(SoapMessage message) {
    }
    
    private void auditLoginFailure(String username) {
		Task task = taskManager.createTaskInstance();
        task.setChannel(SchemaConstants.CHANNEL_WEB_SERVICE_URI);

        AuditEventRecord record = new AuditEventRecord(AuditEventType.CREATE_SESSION, AuditEventStage.REQUEST);
        record.setParameter(username);

        record.setChannel(SchemaConstants.CHANNEL_WEB_SERVICE_URI);
        record.setTimestamp(System.currentTimeMillis());
        record.setOutcome(OperationResultStatus.FATAL_ERROR);

        auditService.audit(record, task);
	}
}