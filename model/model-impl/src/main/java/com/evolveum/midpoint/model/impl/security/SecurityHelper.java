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

import javax.xml.datatype.Duration;
import javax.xml.soap.SOAPMessage;

import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
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

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonceCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordLifeTimeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

/**
 * @author semancik
 *
 */
@Component
public class SecurityHelper {
	
	private static final Trace LOGGER = TraceManager.getTrace(SecurityHelper.class);

    public static final String CONTEXTUAL_PROPERTY_AUDITED_NAME = SecurityHelper.class.getName() + ".audited";

    @Autowired private TaskManager taskManager;
	@Autowired private AuditService auditService;
	@Autowired private ModelObjectResolver objectResolver;
	@Autowired private SecurityEnforcer securityEnforcer;

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
        record.setTimestamp(System.currentTimeMillis());
        record.setOutcome(status);
        record.setMessage(message);
		storeConnectionEnvironment(record, connEnv);

        auditService.audit(record, task);
    }
    
    public void auditLogout(ConnectionEnvironment connEnv, Task task) {
    	AuditEventRecord record = new AuditEventRecord(AuditEventType.TERMINATE_SESSION, AuditEventStage.REQUEST);
		record.setInitiatorAndLoginParameter(task.getOwner());
		record.setTimestamp(System.currentTimeMillis());
		record.setOutcome(OperationResultStatus.SUCCESS);
		storeConnectionEnvironment(record, connEnv);
		auditService.audit(record, task);
    }

	private void storeConnectionEnvironment(AuditEventRecord record, ConnectionEnvironment connEnv) {
		record.setChannel(connEnv.getChannel());
		record.setSessionIdentifier(connEnv.getSessionId());
		HttpConnectionInformation connInfo = connEnv.getConnectionInformation();
		if (connInfo != null) {
			record.setRemoteHostAddress(connInfo.getRemoteHostAddress());
			record.setHostIdentifier(connInfo.getLocalHostName());
		}
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
    
    /**
     * Returns security policy applicable for the specified user. It looks for organization and global policies and takes into account
     * deprecated properties and password policy references. The resulting security policy has all the (non-deprecated) properties set.
     * If there is also referenced value policy, it is will be stored as "object" in the value policy reference inside the
     * returned security policy.
     */
    public <F extends FocusType> SecurityPolicyType locateSecurityPolicy(PrismObject<F> user, PrismObject<SystemConfigurationType> systemConfiguration, 
    		Task task, OperationResult result) throws SchemaException {
    	
    	PrismObject<SecurityPolicyType> orgSecurityPolicy = objectResolver.searchOrgTreeWidthFirstReference(user, o -> o.asObjectable().getSecurityPolicyRef(), "security policy", task, result);
    	LOGGER.trace("Found organization security policy: {}", orgSecurityPolicy);
    	if (orgSecurityPolicy != null) {
    		SecurityPolicyType orgSecurityPolicyType = orgSecurityPolicy.asObjectable();
    		postProcessSecurityPolicy(orgSecurityPolicyType, task, result);
    		traceSecurityPolicy(orgSecurityPolicyType, user);
    		return orgSecurityPolicyType;
    	}
    	
    	if (systemConfiguration != null) {
			SecurityPolicyType globalSecurityPolicy = resolveGlobalSecurityPolicy(user, systemConfiguration.asObjectable(), task, result);
			if (globalSecurityPolicy != null) {
				return globalSecurityPolicy;
			}
    	}
    	
    	
    	// DEPRECATED, legacy
    	PrismObject<ValuePolicyType> orgPasswordPolicy = objectResolver.searchOrgTreeWidthFirstReference(user, o -> o.asObjectable().getPasswordPolicyRef(), "security policy", task, result);
    	LOGGER.trace("Found organization password policy: {}", orgPasswordPolicy);
    	if (orgPasswordPolicy != null) {
    		SecurityPolicyType policy = postProcessPasswordPolicy(orgPasswordPolicy.asObjectable());
    		traceSecurityPolicy(policy, user);
    		return policy;
    	}
    	
    	if (systemConfiguration != null) {
			SecurityPolicyType globalPasswordPolicy = resolveGlobalPasswordPolicy(user, systemConfiguration.asObjectable(), task, result);
			if (globalPasswordPolicy != null) {
				return globalPasswordPolicy;
			}
    	}
    	
    	return null;
	}
    
    public SecurityPolicyType locateGlobalSecurityPolicy(SystemConfigurationType systemConfiguration, Task task, OperationResult result) {
    	if (systemConfiguration != null) {
			SecurityPolicyType globalSecurityPolicy = resolveGlobalSecurityPolicy(null, systemConfiguration, task, result);
			if (globalSecurityPolicy != null) {
				return globalSecurityPolicy;
			}
    	}
    	
    	return null;
    }
    
    public SecurityPolicyType locateGlobalPasswordPolicy(SystemConfigurationType systemConfiguration, Task task, OperationResult result) {
    	if (systemConfiguration != null) {
			SecurityPolicyType globalPasswordPolicy = resolveGlobalPasswordPolicy(null, systemConfiguration, task, result);
			if (globalPasswordPolicy != null) {
				return globalPasswordPolicy;
			}
    	}
    	
    	return null;
    }
    
    private <F extends FocusType> SecurityPolicyType resolveGlobalSecurityPolicy(PrismObject<F> user, SystemConfigurationType systemConfiguration, Task task, OperationResult result) {
    	ObjectReferenceType globalSecurityPolicyRef = systemConfiguration.getGlobalSecurityPolicyRef();
		if (globalSecurityPolicyRef != null) {
			try {
				SecurityPolicyType globalSecurityPolicyType = objectResolver.resolve(globalSecurityPolicyRef, SecurityPolicyType.class, null, "global security policy reference in system configuration", task, result);
				LOGGER.trace("Using global security policy: {}", globalSecurityPolicyType);
				postProcessSecurityPolicy(globalSecurityPolicyType, task, result);
				traceSecurityPolicy(globalSecurityPolicyType, user);
				return globalSecurityPolicyType;
			} catch (ObjectNotFoundException | SchemaException e) {
				LOGGER.error(e.getMessage(), e);
				traceSecurityPolicy(null, user);
				return null;
			}
		}
		
		return null;
    }
    
    private <F extends FocusType> SecurityPolicyType resolveGlobalPasswordPolicy(PrismObject<F> user, SystemConfigurationType systemConfiguration, Task task, OperationResult result) {
    	ObjectReferenceType globalPasswordPolicyRef = systemConfiguration.getGlobalPasswordPolicyRef();
		if (globalPasswordPolicyRef != null) {
			try {
				ValuePolicyType globalPasswordPolicyType = objectResolver.resolve(globalPasswordPolicyRef, ValuePolicyType.class, null, "global security policy reference in system configuration", task, result);
				LOGGER.trace("Using global password policy: {}", globalPasswordPolicyType);
				SecurityPolicyType policy = postProcessPasswordPolicy(globalPasswordPolicyType);
				traceSecurityPolicy(policy, user);
	    		return policy;
			} catch (ObjectNotFoundException | SchemaException e) {
				LOGGER.error(e.getMessage(), e);
				traceSecurityPolicy(null, user);
				return null;
			}
		}
		return null;
    }
    
 
	private <F extends FocusType> void traceSecurityPolicy(SecurityPolicyType securityPolicyType, PrismObject<F> user) {
		if (LOGGER.isTraceEnabled()) {
			if (user != null) {
				if (securityPolicyType == null) {
					LOGGER.trace("Located security policy for {}: null", user);
				} else {
					LOGGER.trace("Located security policy for {}:\n{}", user, securityPolicyType.asPrismObject().debugDump(1));
				}
			} else {
				if (securityPolicyType == null) {
					LOGGER.trace("Located global security policy null");
				} else {
					LOGGER.trace("Located global security policy :\n{}", securityPolicyType.asPrismObject().debugDump(1));
				}
			}
		}
		
	}

	private void postProcessSecurityPolicy(SecurityPolicyType securityPolicyType, Task task, OperationResult result) {
		CredentialsPolicyType creds = securityPolicyType.getCredentials();
		if (creds != null) {
			PasswordCredentialsPolicyType passwd = creds.getPassword();
			if (passwd != null) {
				postProcessPasswordCredentialPolicy(securityPolicyType, passwd, task, result);
			}
			for (NonceCredentialsPolicyType nonce: creds.getNonce()) {
				postProcessCredentialPolicy(securityPolicyType, nonce, "nonce credential policy", task, result);
			}
			SecurityQuestionsCredentialsPolicyType securityQuestions = creds.getSecurityQuestions();
			if (securityQuestions != null) {
				postProcessCredentialPolicy(securityPolicyType, securityQuestions, "security questions credential policy", task, result);
			}
		}
	}
	
	private void postProcessPasswordCredentialPolicy(SecurityPolicyType securityPolicyType, PasswordCredentialsPolicyType passwd, Task task, OperationResult result) {
		// Deprecated settings
		Integer passwordHistoryLength = passwd.getPasswordHistoryLength();
		if (passwordHistoryLength != null && passwd.getHistoryLength() == null) {
			passwd.setHistoryLength(passwordHistoryLength);
		}
		
		ObjectReferenceType passwordPolicyRef = passwd.getPasswordPolicyRef();
		if (passwordPolicyRef != null && passwd.getValuePolicyRef() == null) {
			passwd.setValuePolicyRef(passwordPolicyRef.clone());
		}
		
		ValuePolicyType valuePolicyType = postProcessCredentialPolicy(securityPolicyType, passwd, "password credential policy", task, result);
		
		if (valuePolicyType != null) {
			setDeprecatedPasswordPolicyProperties(valuePolicyType, passwd);
		}
	}
	
	private ValuePolicyType postProcessCredentialPolicy(SecurityPolicyType securityPolicyType, CredentialPolicyType credPolicy, String credShortDesc, Task task, OperationResult result) {
		ObjectReferenceType valuePolicyRef = credPolicy.getValuePolicyRef();
		if (valuePolicyRef == null) {
			return null;
		}
		ValuePolicyType valuePolicyType;
		try {
			valuePolicyType = objectResolver.resolve(valuePolicyRef, ValuePolicyType.class, null, credShortDesc + " in " + securityPolicyType, task, result);
		} catch (ObjectNotFoundException | SchemaException e) {
			LOGGER.warn("{} {} referenced from {} was not found", credShortDesc, valuePolicyRef.getOid(), securityPolicyType);
			return null;
		}
		valuePolicyRef.asReferenceValue().setObject(valuePolicyType.asPrismObject());
		return valuePolicyType;
	}

	private SecurityPolicyType postProcessPasswordPolicy(ValuePolicyType passwordPolicyType) {
		SecurityPolicyType securityPolicyType = new SecurityPolicyType();
		CredentialsPolicyType creds = new CredentialsPolicyType();
		PasswordCredentialsPolicyType passwd = new PasswordCredentialsPolicyType();
		ObjectReferenceType passwordPolicyRef = new ObjectReferenceType();
		passwordPolicyRef.asReferenceValue().setObject(passwordPolicyType.asPrismObject());
		passwd.setValuePolicyRef(passwordPolicyRef);
		creds.setPassword(passwd);
		securityPolicyType.setCredentials(creds);
		setDeprecatedPasswordPolicyProperties(passwordPolicyType, passwd);
		return securityPolicyType;
	}

	private void setDeprecatedPasswordPolicyProperties(ValuePolicyType passwordPolicyType,
			PasswordCredentialsPolicyType passwd) {
		PasswordLifeTimeType lifetime = passwordPolicyType.getLifetime();
		if (lifetime != null) {
			Integer expiration = lifetime.getExpiration();
			if (expiration != null && expiration != 0 && passwd.getMaxAge() == null) {
				passwd.setMaxAge(daysToDuration(expiration));
			}
			Integer minPasswordAge = lifetime.getMinPasswordAge();
			if (minPasswordAge != null && minPasswordAge != 0 && passwd.getMinAge() == null) {
				passwd.setMinAge(daysToDuration(minPasswordAge));
			}
			Integer passwordHistoryLength = lifetime.getPasswordHistoryLength();
			if (passwordHistoryLength != null && passwd.getHistoryLength() == null) {
				passwd.setHistoryLength(passwordHistoryLength);
			}
		}
		String minOccurs = passwordPolicyType.getMinOccurs();
		if (minOccurs != null && passwd.getMinOccurs() == null) {
			passwd.setMinOccurs(minOccurs);
		}
	}
	
	private Duration daysToDuration(int days) {
		return XmlTypeConverter.createDuration((long) days * 1000 * 60 * 60 * 24);
	}

	public SecurityEnforcer getSecurityEnforcer() {
		return securityEnforcer;
	}
}
