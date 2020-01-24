/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.util;

import java.net.URI;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import com.evolveum.midpoint.model.impl.ModelRestService;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.ext.MessageContext;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.security.api.RestAuthenticationMethod;
import com.evolveum.midpoint.model.impl.security.SecurityHelper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConcurrencyException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

/**
 * @author mederly (only copied existing code)
 */
public class RestServiceUtil {

    private static final Trace LOGGER = TraceManager.getTrace(RestServiceUtil.class);

    public static final String MESSAGE_PROPERTY_TASK_NAME = "task";
    private static final String QUERY_PARAMETER_OPTIONS = "options";
    public static final String OPERATION_RESULT_STATUS = "OperationResultStatus";
    public static final String OPERATION_RESULT_MESSAGE = "OperationResultMessage";

    public static final String APPLICATION_YAML = "application/yaml";

    public static Response handleException(OperationResult result, Throwable t) {
        LoggingUtils.logUnexpectedException(LOGGER, "Got exception while servicing REST request: {}", t,
                result != null ? result.getOperation() : "(null)");
        return handleExceptionNoLog(result, t);
    }

    public static Response handleExceptionNoLog(OperationResult result, Throwable t) {
        return createErrorResponseBuilder(result, t).build();
    }

    public static Response createResponse(Response.Status statusCode, OperationResult result) {

        return createResponse(statusCode, null, result, false);

    }

    public static <T> Response createResponse(Response.Status statusCode, T body, OperationResult result) {

        return createResponse(statusCode, body, result, false);

    }

    public static <T> Response createResponse(Response.Status statusCode, T body, OperationResult result, boolean sendOriginObjectIfNotSuccess) {
        result.computeStatusIfUnknown();

        if (result.isPartialError()) {
            return createBody(Response.status(250), sendOriginObjectIfNotSuccess, body, result).build();
        } else if (result.isHandledError()) {
            return createBody(Response.status(240), sendOriginObjectIfNotSuccess, body, result).build();
        }

        return body == null ? Response.status(statusCode).build() : Response.status(statusCode).entity(body).build();
    }

    private static <T> ResponseBuilder createBody(ResponseBuilder builder, boolean sendOriginObjectIfNotSuccess, T body, OperationResult result) {
        if (sendOriginObjectIfNotSuccess) {
            return builder.entity(body);
        }
        return builder.entity(result);

    }

    public static Response createResponse(Response.Status statusCode, URI location, OperationResult result) {
        result.computeStatusIfUnknown();

        if (result.isPartialError()) {
            return createBody(Response.status(250), false, null, result).location(location).build();
        } else if (result.isHandledError()) {
            return createBody(Response.status(240), false, null, result).location(location).build();
        }

        return location == null ? Response.status(statusCode).build() : Response.status(statusCode).location(location).build();
    }

    public static Response.ResponseBuilder createErrorResponseBuilder(OperationResult result, Throwable t) {
        if (t instanceof ObjectNotFoundException) {
            return createErrorResponseBuilder(Response.Status.NOT_FOUND, result);
        }

        if (t instanceof CommunicationException || t instanceof TunnelException) {
            return createErrorResponseBuilder(Response.Status.GATEWAY_TIMEOUT, result);
        }

        if (t instanceof SecurityViolationException) {
            return createErrorResponseBuilder(Response.Status.FORBIDDEN, result);
        }

        if (t instanceof ConfigurationException) {
            return createErrorResponseBuilder(Response.Status.BAD_GATEWAY, result);
        }

        if (t instanceof SchemaException || t instanceof ExpressionEvaluationException) {
            return createErrorResponseBuilder(Response.Status.BAD_REQUEST, result);
        }

        if (t instanceof PolicyViolationException
                || t instanceof ObjectAlreadyExistsException
                || t instanceof ConcurrencyException) {
            return createErrorResponseBuilder(Response.Status.CONFLICT, result);
        }

        return createErrorResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR, result);
    }

    public static Response.ResponseBuilder createErrorResponseBuilder(Response.Status status, OperationResult result) {
        OperationResultType resultBean;
        if (result != null) {
            result.computeStatusIfUnknown();
            resultBean = result.createOperationResultType();
        } else {
            resultBean = null;
        }
        return createErrorResponseBuilder(status, resultBean);
    }

    public static Response.ResponseBuilder createErrorResponseBuilder(Response.Status status, OperationResultType message) {
        return Response.status(status).entity(message);
    }

    public static ModelExecuteOptions getOptions(UriInfo uriInfo) {
        List<String> options = uriInfo.getQueryParameters().get(QUERY_PARAMETER_OPTIONS);
        return ModelExecuteOptions.fromRestOptions(options);
    }

    public static Task initRequest(MessageContext mc) {
        // No need to audit login. it was already audited during authentication
        return (Task) mc.get(MESSAGE_PROPERTY_TASK_NAME);
    }

    public static Task initRequest(TaskManager taskManager) {
        // No need to audit login. it was already audited during authentication
        Task task = taskManager.createTaskInstance(ModelRestService.OPERATION_REST_SERVICE);
        task.setChannel(SchemaConstants.CHANNEL_REST_URI);
        return task;
    }

    public static void finishRequest(Task task, SecurityHelper securityHelper) {
        task.getResult().computeStatus();
        ConnectionEnvironment connEnv = ConnectionEnvironment.create(SchemaConstants.CHANNEL_REST_URI);
        connEnv.setSessionIdOverride(task.getTaskIdentifier());
        securityHelper.auditLogout(connEnv, task);
    }

    public static void createAbortMessage(ContainerRequestContext requestCtx) {
        requestCtx.abortWith(Response.status(Status.UNAUTHORIZED)
                .header("WWW-Authenticate",
                        RestAuthenticationMethod.BASIC.getMethod() + " realm=\"midpoint\", " +
                                RestAuthenticationMethod.SECURITY_QUESTIONS.getMethod())
                .build());
    }

    public static void createSecurityQuestionAbortMessage(ContainerRequestContext requestCtx, String secQChallenge) {
        String challenge = "";
        if (StringUtils.isNotBlank(secQChallenge)) {
            challenge = " " + Base64Utility.encode(secQChallenge.getBytes());
        }

        requestCtx.abortWith(Response.status(Status.UNAUTHORIZED)
                .header("WWW-Authenticate",
                        RestAuthenticationMethod.SECURITY_QUESTIONS.getMethod() + challenge)
                .build());
    }
}
