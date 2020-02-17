/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.security;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

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
    protected abstract T createAuthenticationContext(AuthorizationPolicy policy, ContainerRequestContext requestCtx, Class<? extends FocusType> clazz);

    public void handleRequest(AuthorizationPolicy policy, Message m, ContainerRequestContext requestCtx) {

        if (policy == null){
            RestServiceUtil.createAbortMessage(requestCtx);
            return;
        }

        T authenticationContext = createAuthenticationContext(policy, requestCtx, UserType.class);

        if (authenticationContext == null) {
            return;
        }

        String enteredUsername = authenticationContext.getUsername();

        if (enteredUsername == null) {
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
        } catch (UsernameNotFoundException | BadCredentialsException | DisabledException | LockedException |
                CredentialsExpiredException | AccessDeniedException | AuthenticationCredentialsNotFoundException |
                AuthenticationServiceException e) {
            LOGGER.trace("Exception while authenticating username '{}' to REST service: {}", enteredUsername, e.getMessage(), e);
            RestServiceUtil.createAbortMessage(requestCtx);
            return;
        }

        FocusType focus = ((MidPointPrincipal)token.getPrincipal()).getFocus();
        task.setOwner(focus.asPrismObject());

        //  m.put(RestServiceUtil.MESSAGE_PROPERTY_TASK_NAME, task);
        if (!authorizeUser(focus, null, enteredUsername, connEnv, requestCtx)){
            return;
        }

        String oid = requestCtx.getHeaderString("Switch-To-Principal");
        OperationResult result = task.getResult();
        if (StringUtils.isNotBlank(oid)){
            try {
                PrismObject<? extends FocusType> authorizedUser = model.getObject(UserType.class, oid, null, task, result);
                task.setOwner(authorizedUser);
                if (!authorizeUser(AuthorizationConstants.AUTZ_REST_PROXY_URL, focus, authorizedUser, enteredUsername, connEnv, requestCtx)){
                    return;
                }
                authenticateUser(authorizedUser, authorizedUser.getName().getOrig(), connEnv, requestCtx);
                //                    if (!authorizeUser(authorizedUser.asObjectable(), null, authorizedUser.getName().getOrig(), connEnv, requestCtx)){
                //                        return;
                //                    }
            } catch (ObjectNotFoundException | SchemaException | SecurityViolationException
                    | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
                LOGGER.trace("Exception while authenticating user identified with '{}' to REST service: {}", oid, e.getMessage(), e);
                RestServiceUtil.createAbortMessage(requestCtx);
                return;
            }
        }

        m.put(RestServiceUtil.MESSAGE_PROPERTY_TASK_NAME, task);

        LOGGER.trace("Authorized to use REST service ({})", focus);

    }

    private boolean authorizeUser(FocusType focus, PrismObject<? extends FocusType> proxyFocus, String enteredUsername, ConnectionEnvironment connEnv, ContainerRequestContext requestCtx) {
        authenticateUser(focus.asPrismObject(), enteredUsername, connEnv, requestCtx);
        return authorizeUser(AuthorizationConstants.AUTZ_REST_ALL_URL, focus, null, enteredUsername, connEnv, requestCtx);
    }

    private void authenticateUser(PrismObject<? extends FocusType> focus, String enteredUsername, ConnectionEnvironment connEnv, ContainerRequestContext requestCtx) {
        try {
            securityContextManager.setupPreAuthenticatedSecurityContext(focus);
        } catch (SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
            securityHelper.auditLoginFailure(enteredUsername, focus.asObjectable(), connEnv, "Schema error: "+e.getMessage());
            requestCtx.abortWith(Response.status(Status.BAD_REQUEST).build());
            //                return false;
        }

        LOGGER.trace("Authenticated to REST service as {}", focus);
    }

    private boolean authorizeUser(String authorization, FocusType focus, PrismObject<? extends FocusType> proxyFocus, String enteredUsername, ConnectionEnvironment connEnv, ContainerRequestContext requestCtx) {
        Task task = taskManager.createTaskInstance(MidpointRestAuthenticator.class.getName() + ".authorizeUser");
        try {
            // authorize for proxy
            securityEnforcer.authorize(authorization, null, AuthorizationParameters.Builder.buildObject(proxyFocus), null, task, task.getResult());
        } catch (SecurityViolationException e){
            securityHelper.auditLoginFailure(enteredUsername, focus, connEnv, "Not authorized");
            requestCtx.abortWith(Response.status(Status.FORBIDDEN).build());
            return false;
        } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | ConfigurationException e) {
            securityHelper.auditLoginFailure(enteredUsername, focus, connEnv, "Internal error: "+e.getMessage());
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
