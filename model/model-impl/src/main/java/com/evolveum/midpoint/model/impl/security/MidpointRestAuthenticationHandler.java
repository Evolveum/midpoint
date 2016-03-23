/*
 * Copyright (c) 2013-2016 Evolveum
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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.security.Principal;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.SecurityContext;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class MidpointRestAuthenticationHandler implements ContainerRequestFilter, ContainerResponseFilter {
	 
	@Autowired(required =true)
	private UserProfileService userProfileService;
	
	@Autowired(required = true)
	private SecurityEnforcer securityEnforcer;
	
	@Autowired(required = true)
	private Protector protector;
		
	@Autowired(required = true)
	private SecurityHelper securityHelper;
	
	@Autowired(required = true)
	private ActivationComputer activationComputer;
	
    public void handleRequest(Message m, ContainerRequestContext requestCtx) {
        AuthorizationPolicy policy = (AuthorizationPolicy)m.get(AuthorizationPolicy.class);
        
        if (policy == null){
        	requestCtx.abortWith(Response.status(401).header("WWW-Authenticate", "Basic").build());
        	return;
        }
        
        String enteredUsername = policy.getUserName();
        
        if (enteredUsername == null){
        	requestCtx.abortWith(Response.status(401).header("WWW-Authenticate", "Basic").build());
        	return;
        }
        
        
        MidPointPrincipal principal;
		try {
			principal = userProfileService.getPrincipal(enteredUsername);
		} catch (ObjectNotFoundException e) {
			securityHelper.auditLoginFailure(enteredUsername, "No user", SchemaConstants.CHANNEL_REST_URI);
			requestCtx.abortWith(Response.status(401).header("WWW-Authenticate", "Basic authentication failed. Cannot authenticate user.").build());
			return;
		}
        
        if (principal == null ){
        	securityHelper.auditLoginFailure(enteredUsername, "No user", SchemaConstants.CHANNEL_REST_URI);
        	requestCtx.abortWith(Response.status(401).header("WWW-Authenticate", "Basic").build());
        	return;
        }
        
        UserType user = principal.getUser();
        
        if (!activationComputer.isActive(user.getActivation())) {
        	securityHelper.auditLoginFailure(enteredUsername, "User not active", SchemaConstants.CHANNEL_REST_URI);
			requestCtx.abortWith(Response.status(403).build());
			return;
        }
        
        String enteredPassword = policy.getPassword();
        
        if (enteredPassword == null) {
        	securityHelper.auditLoginFailure(enteredUsername, "No password entered", SchemaConstants.CHANNEL_REST_URI);
        	requestCtx.abortWith(Response.status(401).header("WWW-Authenticate", "Basic authentication failed. Cannot authenticate user without password").build());
        	return;
        }
        
        if (user.getCredentials() == null) {
        	securityHelper.auditLoginFailure(enteredUsername, "No user credentials", SchemaConstants.CHANNEL_REST_URI);
        	requestCtx.abortWith(Response.status(403).build());
        	return;
        }
        
        PasswordType userPassword = user.getCredentials().getPassword();
        
        if (userPassword == null) {
        	securityHelper.auditLoginFailure(enteredUsername, "No password in user credentials", SchemaConstants.CHANNEL_REST_URI);
        	requestCtx.abortWith(Response.status(403).build());
        	return;
        }
        
        ProtectedStringType protectedPass = userPassword.getValue();
        if (protectedPass.getClearValue() != null) {
        	if (!enteredPassword.equals(protectedPass.getClearValue())) {
        		securityHelper.auditLoginFailure(enteredUsername, "Wrong password", SchemaConstants.CHANNEL_REST_URI);
        		requestCtx.abortWith(Response.status(401).header("WWW-Authenticate", "Basic").build());
        		return;
            }
        } else if (protectedPass.getEncryptedDataType() != null) {
        	try{
        		String decrypted = protector.decryptString(protectedPass);
        		if (!enteredPassword.equals(decrypted)) {
        			securityHelper.auditLoginFailure(enteredUsername, "Wrong password", SchemaConstants.CHANNEL_REST_URI);
        			requestCtx.abortWith(Response.status(401).header("WWW-Authenticate", "Basic").build());
        			return;
        		}
        	} catch (EncryptionException ex) {
        		securityHelper.auditLoginFailure(enteredUsername, "Password cryptographic error: "+ex.getMessage(), SchemaConstants.CHANNEL_REST_URI);
        		requestCtx.abortWith(Response.status(403).build());
        		return;
        	}
        	
        } else {
        	securityHelper.auditLoginFailure(enteredUsername, "Unsupported password format or no password value", SchemaConstants.CHANNEL_REST_URI);
        	requestCtx.abortWith(Response.status(403).build());
        	return;
        }
        
        m.put("authenticatedUser", user);
        securityEnforcer.setupPreAuthenticatedSecurityContext(user.asPrismObject());
           
        OperationResult authorizeResult = new OperationResult("Rest authentication/authorization operation.");
        
        
        try {
			securityEnforcer.authorize(AuthorizationConstants.AUTZ_REST_ALL_URL, null, null, null, null, null, authorizeResult);
		} catch (SecurityViolationException e){
			securityHelper.auditLoginFailure(enteredUsername, "Not authorized", SchemaConstants.CHANNEL_REST_URI);
			requestCtx.abortWith(Response.status(403).build());
			return;
		} catch (SchemaException e) {
			securityHelper.auditLoginFailure(enteredUsername, "Schema error: "+e.getMessage(), SchemaConstants.CHANNEL_REST_URI);
			requestCtx.abortWith(Response.status(Status.BAD_REQUEST).build());
			return;
		}
        
    }

	@Override
	public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
		// nothing to do
	}

	@Override
	public void filter(ContainerRequestContext requestCtx) throws IOException {
		Message m = JAXRSUtils.getCurrentMessage();
		handleRequest(m, requestCtx);
	}

}
