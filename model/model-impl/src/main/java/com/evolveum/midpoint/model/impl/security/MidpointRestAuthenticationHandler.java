/*
 * Copyright (c) 2013-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.security;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.security.api.RestAuthenticationMethod;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.api.authentication.NodeAuthenticationEvaluator;
import com.evolveum.midpoint.model.impl.util.RestServiceUtil;
import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;

/**
 * @author Katka Valalikova
 * @author Radovan Semancik
 */
public class MidpointRestAuthenticationHandler implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointRestAuthenticationHandler.class);

    @Autowired private MidpointRestPasswordAuthenticator passwordAuthenticator;

    @Autowired private MidpointRestSecurityQuestionsAuthenticator securityQuestionAuthenticator;

    @Autowired private NodeAuthenticationEvaluator nodeAuthenticator;
    @Autowired private TaskManager taskManager;

    @Autowired
    private SystemObjectCache systemObjectCache;

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        // nothing to do
    }

    @Override
    public void filter(ContainerRequestContext requestCtx) {

        boolean isExperimentalEnabled = false;
        try {
            isExperimentalEnabled = SystemConfigurationTypeUtil.isExperimentalCodeEnabled(
                    systemObjectCache.getSystemConfiguration(new OperationResult("Load System Config")).asObjectable());
        } catch (SchemaException e) {
            LOGGER.error("Couldn't load system configuration", e);
        }
        if (isExperimentalEnabled) {
            //used flexible authentication
            return;
        }

        Message m = JAXRSUtils.getCurrentMessage();

        AuthorizationPolicy policy = m.get(AuthorizationPolicy.class);
        if (policy != null) {
            passwordAuthenticator.handleRequest(policy, m, requestCtx);
            return;
        }

        String authorization = requestCtx.getHeaderString("Authorization");

        if (StringUtils.isBlank(authorization)){
            RestServiceUtil.createAbortMessage(requestCtx);
            return;
        }

        String[] parts = authorization.split(" ");
        String authenticationType = parts[0];

        if (parts.length == 1 && RestAuthenticationMethod.SECURITY_QUESTIONS.getMethod().equals(authenticationType)) {
            RestServiceUtil.createSecurityQuestionAbortMessage(requestCtx, "{\"user\" : \"username\"}");
            return;
        }

        if (parts.length != 2) {
            RestServiceUtil.createAbortMessage(requestCtx);
            return;
        }

        String base64Credentials = parts[1];

        if (RestAuthenticationMethod.SECURITY_QUESTIONS.getMethod().equals(authenticationType)) {
            try {
                String decodedCredentials = new String(Base64Utility.decode(base64Credentials));
                policy = new AuthorizationPolicy();
                policy.setAuthorizationType(RestAuthenticationMethod.SECURITY_QUESTIONS.getMethod());
                policy.setAuthorization(decodedCredentials);
                securityQuestionAuthenticator.handleRequest(policy, m, requestCtx);
            } catch (Base64Exception e) {
                RestServiceUtil.createSecurityQuestionAbortMessage(requestCtx, "{\"user\" : \"username\"}");
            }
        } else if (RestAuthenticationMethod.CLUSTER.getMethod().equals(authenticationType)) {
            HttpConnectionInformation connectionInfo = SecurityUtil.getCurrentConnectionInformation();
            String remoteAddress = connectionInfo != null ? connectionInfo.getRemoteHostAddress() : null;
            String decodedCredentials;
            try {
                decodedCredentials = new String(Base64Utility.decode(base64Credentials));
            } catch (Base64Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't decode base64-encoded credentials", e);
                RestServiceUtil.createAbortMessage(requestCtx);
                return;
            }
            if (!nodeAuthenticator.authenticate(null, remoteAddress, decodedCredentials, "?")) {
                RestServiceUtil.createAbortMessage(requestCtx);
                return;
            }
            Task task = taskManager.createTaskInstance();
            m.put(RestServiceUtil.MESSAGE_PROPERTY_TASK_NAME, task);
        }
    }
}
