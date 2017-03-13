/**
 * Copyright (c) 2015-2017 Evolveum
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
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import javax.xml.soap.SOAPMessage;

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
    
    @Autowired(required = true)
    private ModelObjectResolver objectResolver;

    public void auditLoginSuccess(@NotNull UserType user, @NotNull ConnectionEnvironment connEnv) {
        auditLogin(user.getName().getOrig(), user, connEnv, OperationResultStatus.SUCCESS, null);
    }

    public void auditLoginFailure(@Nullable String username, @Nullable UserType user, @NotNull ConnectionEnvironment connEnv, String message) {
        auditLogin(username, user, connEnv, OperationResultStatus.FATAL_ERROR, message);
    }

    private void auditLogin(@Nullable String username, @Nullable UserType user, @NotNull ConnectionEnvironment connEnv, @NotNull OperationResultStatus status,
                            @Nullable String message) {
        Task task = taskManager.createTaskInstance();
        task.setChannel(connEnv.getChannel());

        LOGGER.debug("Login {} username={}, channel={}: {}",
				status == OperationResultStatus.SUCCESS ? "success" : "failure", username,
				connEnv.getChannel(), message);

        AuditEventRecord record = new AuditEventRecord(AuditEventType.CREATE_SESSION, AuditEventStage.REQUEST);
        record.setParameter(username);
		if (user != null) {
			record.setInitiator(user.asPrismObject());
		}

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
    
    public <F extends FocusType> SecurityPolicyType locateSecurityPolicy(PrismObject<F> user, PrismObject<SystemConfigurationType> systemConfiguration, Task task, OperationResult result) throws SchemaException {
    	SecurityPolicyType orgSecurityPolicy = determineOrgSecurityPolicy(user, task, result);
    	if (orgSecurityPolicy != null) {
    		return orgSecurityPolicy;
    	}
    	if (systemConfiguration == null) {
			return null;
		}
		ObjectReferenceType globalSecurityPolicyRef = systemConfiguration.asObjectable().getGlobalSecurityPolicyRef();
		if (globalSecurityPolicyRef == null) {
			return null;
		}
		try {
			return objectResolver.resolve(globalSecurityPolicyRef, SecurityPolicyType.class, null, "global security policy reference in system configuration", task, result);
		} catch (ObjectNotFoundException | SchemaException e) {
			LOGGER.error(e.getMessage(), e);
			return null;
		}
	}

	private <O extends ObjectType> SecurityPolicyType determineOrgSecurityPolicy(PrismObject<O> object, Task task, OperationResult result)
			throws SchemaException {
		LOGGER.trace("Determining security policies from object: {}", object);
		PrismReference orgRef = object.findReference(ObjectType.F_PARENT_ORG_REF);
		if (orgRef == null) {
			return null;
		}
		List<PrismReferenceValue> orgRefValues = orgRef.getValues();
		SecurityPolicyType resultingSecurityPolicy = null;
		List<PrismObject<OrgType>> orgs = new ArrayList<PrismObject<OrgType>>();
		try {
			for (PrismReferenceValue orgRefValue : orgRefValues) {
				if (orgRefValue != null) {

					PrismObject<OrgType> org = objectResolver.resolve(orgRefValue, "resolving parent org ref", null, null, result);
					orgs.add(org);
					SecurityPolicyType securityPolicy = resolveOrgSecurityPolicy(org, task, result);

					if (securityPolicy != null) {
						if (resultingSecurityPolicy == null) {
							resultingSecurityPolicy = securityPolicy;
						} else if (!StringUtils.equals(securityPolicy.getOid(), resultingSecurityPolicy.getOid())) {
							throw new SchemaException(
									"Found more than one security policy for user. Please check your configuration");
						}
					}
				}
			}
		} catch (ObjectNotFoundException ex) {
			throw new IllegalStateException(ex);
		}
		// go deeper
		if (resultingSecurityPolicy == null) {
			for (PrismObject<OrgType> orgType : orgs) {
				resultingSecurityPolicy = determineOrgSecurityPolicy(orgType, task, result);
				if (resultingSecurityPolicy != null) {
					return resultingSecurityPolicy;
				}
			}
		}
		return resultingSecurityPolicy;
	}

	private SecurityPolicyType resolveOrgSecurityPolicy(PrismObject<OrgType> org, Task task, OperationResult result)
			throws SchemaException {
		try {
			SecurityPolicyType securityPolicy = null;
			OrgType orgType = org.asObjectable();
			ObjectReferenceType securityPolicyRef = orgType.getSecurityPolicyRef();
			if (securityPolicyRef != null) {
				LOGGER.trace("Org {} has specified security policy.", orgType);
				securityPolicy = objectResolver.resolve(securityPolicyRef, SecurityPolicyType.class, null,
						"resolving security policy for organization", task, result);
				LOGGER.trace("Resolved security policy {}", securityPolicy);				
			} else {
				ObjectReferenceType paswordPolicyRef = orgType.getPasswordPolicyRef();
				if (paswordPolicyRef != null) {
					LOGGER.trace("Org {} has specified password policy.", orgType);
					ValuePolicyType passordPolicy = objectResolver.resolve(paswordPolicyRef, ValuePolicyType.class, null,
							"resolving password policy for organization", task, result);
					LOGGER.trace("Resolved password policy {}", securityPolicy);
					securityPolicy = createSecurityPolicy(passordPolicy);
				}
			}
			
			return securityPolicy;

		} catch (ObjectNotFoundException e) {
			LOGGER.error("Cannot find policy: {}", e.getMessage(), e);
			return null;
		}
	}
	
	private SecurityPolicyType createSecurityPolicy(ValuePolicyType passordPolicy) {
		SecurityPolicyType securityPolicy = new SecurityPolicyType();
		CredentialsPolicyType creds = new CredentialsPolicyType();
		PasswordCredentialsPolicyType passd = new PasswordCredentialsPolicyType();
		ObjectReferenceType passwordPolicyRef = new ObjectReferenceType();
		passwordPolicyRef.asReferenceValue().setObject(passordPolicy.asPrismObject());
		passd.setPasswordPolicyRef(passwordPolicyRef);
		creds.setPassword(passd);
		securityPolicy.setCredentials(creds);
		return securityPolicy;
	}
}
