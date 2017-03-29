package com.evolveum.midpoint.model.impl.security;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;


public abstract class MidpointRestAuthenticator<T extends AbstractAuthenticationContext> {
	
	
	private static final Trace LOGGER = TraceManager.getTrace(MidpointRestAuthenticator.class);
	
	@Autowired(required = true)
	private SecurityEnforcer securityEnforcer;
			
	@Autowired(required = true)
	private SecurityHelper securityHelper;
	
	@Autowired(required=true)
	private TaskManager taskManager;
	
	@Autowired(required=true)
	private ModelService model;
	
	protected abstract AuthenticationEvaluator<T> getAuthenticationEvaluator();
	protected abstract T createAuthenticationContext(AuthorizationPolicy policy) throws IOException;
	
	 public void handleRequest(AuthorizationPolicy policy, Message m, ContainerRequestContext requestCtx) {
	        
	    	if (policy == null){
	        	requestCtx.abortWith(Response.status(Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic, SecQ").build());
	        	return;
	        }
	        
	        
	        T authenticationContext;
			try {
				authenticationContext = createAuthenticationContext(policy);
			} catch (IOException e1) {
				requestCtx.abortWith(Response.status(Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic, SecQ").build());
				return;
			}	
	        
	        String enteredUsername = authenticationContext.getUsername();
	        
	        if (enteredUsername == null){
	        	requestCtx.abortWith(Response.status(Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic, SecQ").build());
	        	return;
	        }
	        
	        LOGGER.trace("Authenticating username '{}' to REST service", enteredUsername);
	        
	        // We need to create task before attempting authentication. Task ID is also a session ID.
	        Task task = taskManager.createTaskInstance(ModelRestService.OPERATION_REST_SERVICE);
	        task.setChannel(SchemaConstants.CHANNEL_REST_URI);
	        
	        ConnectionEnvironment connEnv = createConnectionEnvironment();
	        connEnv.setSessionId(task.getTaskIdentifier());
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
	        if (!authorizeUser(user, enteredUsername, false, connEnv, requestCtx)){
	        	return;
	        }
	        
	        String oid = requestCtx.getHeaderString("Switch-To-Principal");
	        OperationResult result = task.getResult();
	        if (StringUtils.isNotBlank(oid)){
	        	try {
					PrismObject<UserType> authorizedUser = model.getObject(UserType.class, oid, null, task, result);
					task.setOwner(authorizedUser);
					user = authorizedUser.asObjectable();
				} catch (ObjectNotFoundException | SchemaException | SecurityViolationException
						| CommunicationException | ConfigurationException e) {
					LOGGER.trace("Exception while authenticating user identified with '{}' to REST service: {}", oid, e.getMessage(), e);
		        	requestCtx.abortWith(Response.status(Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic authentication failed. Cannot authenticate user.").build());
					return;
				}
	        	
	        	if (!authorizeUser(user, user.getName().getOrig(), true, connEnv, requestCtx)){
	        		return;
	        	}
	        }
	        
	        m.put(RestServiceUtil.MESSAGE_PROPERTY_TASK_NAME, task);
	        
	        LOGGER.trace("Authorized to use REST service ({})", user);
	        
	    }
	 
	   private boolean authorizeUser(UserType user, String enteredUsername, boolean isProxyUser, ConnectionEnvironment connEnv, ContainerRequestContext requestCtx) {
	    	try {
	        	securityEnforcer.setupPreAuthenticatedSecurityContext(user.asPrismObject());
	        } catch (SchemaException e) {
				securityHelper.auditLoginFailure(enteredUsername, user, connEnv, "Schema error: "+e.getMessage());
				requestCtx.abortWith(Response.status(Status.BAD_REQUEST).build());
				return false;
			}
	        
	        LOGGER.trace("Authenticated to REST service as {}", user);
	        
	        if (!authorizeUser(AuthorizationConstants.AUTZ_REST_ALL_URL, user, enteredUsername, connEnv, requestCtx)){
	        	return false;
	        }
	        
	        if (isProxyUser) {
	        	return authorizeUser(AuthorizationConstants.AUTZ_REST_PROXY_URL, user, enteredUsername, connEnv, requestCtx);
	        }
	        
	        return true;
	        
	    }
	    
	    private boolean authorizeUser(String authorization, UserType user, String enteredUsername, ConnectionEnvironment connEnv, ContainerRequestContext requestCtx){
	    	OperationResult authorizeResult = new OperationResult("Rest authentication/authorization operation.");
	    	try {
				securityEnforcer.authorize(AuthorizationConstants.AUTZ_REST_ALL_URL, null, null, null, null, null, authorizeResult);
			} catch (SecurityViolationException e){
				securityHelper.auditLoginFailure(enteredUsername, user, connEnv, "Not authorized");
				requestCtx.abortWith(Response.status(Status.FORBIDDEN).build());
				return false;
			} catch (SchemaException e) {
				securityHelper.auditLoginFailure(enteredUsername, user, connEnv, "Schema error: "+e.getMessage());
				requestCtx.abortWith(Response.status(Status.BAD_REQUEST).build());
				return false;
			}
	    	return true;
	    }
	    
	    private ConnectionEnvironment createConnectionEnvironment() {
			ConnectionEnvironment connEnv = new ConnectionEnvironment();
			connEnv.setChannel(SchemaConstants.CHANNEL_REST_URI);
			// TODO: remote host
			return connEnv;
		}

}
