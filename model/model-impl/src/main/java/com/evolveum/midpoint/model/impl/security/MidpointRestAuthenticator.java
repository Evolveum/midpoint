/*
 * Copyright (c) 2010-2017 Evolveum
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

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.evolveum.midpoint.model.api.AuthenticationEvaluator;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.context.AbstractAuthenticationContext;
import com.evolveum.midpoint.model.impl.ModelRestService;
import com.evolveum.midpoint.model.impl.util.RestServiceUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;


public abstract class MidpointRestAuthenticator<T extends AbstractAuthenticationContext> {

	private static final Trace LOGGER = TraceManager.getTrace(MidpointRestAuthenticator.class);

	@Autowired private SecurityContextManager securityContextManager;
	@Autowired private SecurityEnforcer securityEnforcer;
	@Autowired private SecurityHelper securityHelper;
	@Autowired private TaskManager taskManager;
	@Autowired private ModelService model;

	protected abstract AuthenticationEvaluator<T> getAuthenticationEvaluator();
	protected abstract T createAuthenticationContext(AuthorizationPolicy policy, ContainerRequestContext requestCtx);

	 public void handleRequest(AuthorizationPolicy policy, Message m, ContainerRequestContext requestCtx) {

	    	if (policy == null){
	    		RestServiceUtil.createAbortMessage(requestCtx);
	        	return;
	        }


	        T authenticationContext = createAuthenticationContext(policy, requestCtx);

	        if (authenticationContext == null) {
	        	return;
	        }

	        String enteredUsername = authenticationContext.getUsername();

	        if (enteredUsername == null){
	        	RestServiceUtil.createAbortMessage(requestCtx);
	        	return;
	        }

	        LOGGER.trace("Authenticating username '{}' to REST service", enteredUsername);

	        // We need to create task before attempting authentication. Task ID is also a session ID.
	        Task task = taskManager.createTaskInstance(ModelRestService.OPERATION_REST_SERVICE);
	        task.setChannel(SchemaConstants.CHANNEL_REST_URI);

	        ConnectionEnvironment connEnv = ConnectionEnvironment.create(SchemaConstants.CHANNEL_REST_URI);
	        connEnv.setSessionIdOverride(task.getTaskIdentifier());
	        UsernamePasswordAuthenticationToken token;
	        try {
	        	token = getAuthenticationEvaluator().authenticate(connEnv, authenticationContext);
	        } catch (UsernameNotFoundException | BadCredentialsException e) {
	        	LOGGER.trace("Exception while authenticating username '{}' to REST service: {}", enteredUsername, e.getMessage(), e);
	        	requestCtx.abortWith(Response.status(Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic authentication failed. Cannot authenticate user.").build());
				return;
	        } catch (DisabledException | LockedException | CredentialsExpiredException | AccessDeniedException
	        		| AuthenticationCredentialsNotFoundException | AuthenticationServiceException e) {
	        	LOGGER.trace("Exception while authenticating username '{}' to REST service: {}", enteredUsername, e.getMessage(), e);
	        	requestCtx.abortWith(Response.status(Status.FORBIDDEN).build());
				return;
	        }

	        UserType user = ((MidPointPrincipal)token.getPrincipal()).getUser();
	        task.setOwner(user.asPrismObject());

	        //  m.put(RestServiceUtil.MESSAGE_PROPERTY_TASK_NAME, task);
	        if (!authorizeUser(user, null, enteredUsername, connEnv, requestCtx)){
	        	return;
	        }

	        String oid = requestCtx.getHeaderString("Switch-To-Principal");
	        OperationResult result = task.getResult();
	        if (StringUtils.isNotBlank(oid)){
	        	try {
					PrismObject<UserType> authorizedUser = model.getObject(UserType.class, oid, null, task, result);
					task.setOwner(authorizedUser);
					if (!authorizeUser(AuthorizationConstants.AUTZ_REST_PROXY_URL, user, authorizedUser, enteredUsername, connEnv, requestCtx)){
		        		return;
		        	}
					authenticateUser(authorizedUser, authorizedUser.getName().getOrig(), connEnv, requestCtx);
//					if (!authorizeUser(authorizedUser.asObjectable(), null, authorizedUser.getName().getOrig(), connEnv, requestCtx)){
//		        		return;
//		        	}
				} catch (ObjectNotFoundException | SchemaException | SecurityViolationException
						| CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
					LOGGER.trace("Exception while authenticating user identified with '{}' to REST service: {}", oid, e.getMessage(), e);
		        	requestCtx.abortWith(Response.status(Status.UNAUTHORIZED).header("WWW-Authenticate", "Proxy Authentication failed. Cannot authenticate user.").build());
					return;
				}
	
	
	        }

	        m.put(RestServiceUtil.MESSAGE_PROPERTY_TASK_NAME, task);

	        LOGGER.trace("Authorized to use REST service ({})", user);

	    }

	   private boolean authorizeUser(UserType user, PrismObject<UserType> proxyUser, String enteredUsername, ConnectionEnvironment connEnv, ContainerRequestContext requestCtx) {
	    	authenticateUser(user.asPrismObject(), enteredUsername, connEnv, requestCtx);
	        return authorizeUser(AuthorizationConstants.AUTZ_REST_ALL_URL, user, null, enteredUsername, connEnv, requestCtx);
	    }

	   private void authenticateUser(PrismObject<UserType> user, String enteredUsername, ConnectionEnvironment connEnv, ContainerRequestContext requestCtx) {
		   try {
	    		securityContextManager.setupPreAuthenticatedSecurityContext(user);
	        } catch (SchemaException e) {
				securityHelper.auditLoginFailure(enteredUsername, user.asObjectable(), connEnv, "Schema error: "+e.getMessage());
				requestCtx.abortWith(Response.status(Status.BAD_REQUEST).build());
//				return false;
			}

	        LOGGER.trace("Authenticated to REST service as {}", user);
	   }
	   
	    private boolean authorizeUser(String authorization, UserType user, PrismObject<UserType> proxyUser, String enteredUsername, ConnectionEnvironment connEnv, ContainerRequestContext requestCtx) {
	    	Task task = taskManager.createTaskInstance(MidpointRestAuthenticator.class.getName() + ".authorizeUser");
	    	try {
	    		// authorize for proxy
	    		securityEnforcer.authorize(authorization, null, AuthorizationParameters.Builder.buildObject(proxyUser), null, task, task.getResult());
			} catch (SecurityViolationException e){
				securityHelper.auditLoginFailure(enteredUsername, user, connEnv, "Not authorized");
				requestCtx.abortWith(Response.status(Status.FORBIDDEN).build());
				return false;
			} catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | ConfigurationException e) {
				securityHelper.auditLoginFailure(enteredUsername, user, connEnv, "Internal error: "+e.getMessage());
				requestCtx.abortWith(Response.status(Status.BAD_REQUEST).build());
				return false;
			}
	    	return true;
	    }

	    public SecurityContextManager getSecurityContextManager() {
			return securityContextManager;
		}

	    public SecurityEnforcer getSecurityEnforcer() {
			return securityEnforcer;
		}
		public ModelService getModel() {
			return model;
		}

	    public TaskManager getTaskManager() {
			return taskManager;
		}
}
