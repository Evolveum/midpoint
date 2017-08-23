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

package com.evolveum.midpoint.model.impl.util;

import java.net.URI;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.ext.MessageContext;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.security.RestAuthenticationMethod;
import com.evolveum.midpoint.model.impl.security.SecurityHelper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.AuthorizationException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConcurrencyException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.NoFocusNameSchemaException;
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

	public static final String MESSAGE_PROPERTY_TASK_NAME = "task";
	private static final String QUERY_PARAMETER_OPTIONS = "options";
	public static final String OPERATION_RESULT_STATUS = "OperationResultStatus";
	public static final String OPERATION_RESULT_MESSAGE = "OperationResultMessage";

	public static Response handleException(OperationResult result, Exception ex) {
		return createErrorResponseBuilder(result, ex).build();
	}
	
	public static <T> Response createResponse(Response.Status statusCode, OperationResult result) {
		
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
	
	public static <T> Response createResponse(Response.Status statusCode, URI location, OperationResult result) {
		result.computeStatusIfUnknown();
		
		if (result.isPartialError()) {
			return createBody(Response.status(250), false, null, result).location(location).build();
		} else if (result.isHandledError()) {
			return createBody(Response.status(240), false, null, result).location(location).build();
		}		
		
		
		return location == null ? Response.status(statusCode).build() : Response.status(statusCode).location(location).build();
	}
	
	

	public static Response.ResponseBuilder createErrorResponseBuilder(OperationResult result, Exception ex) {
		if (ex instanceof ObjectNotFoundException) {
			return createErrorResponseBuilder(Response.Status.NOT_FOUND, result);
		}

		if (ex instanceof CommunicationException || ex instanceof TunnelException) {
			return createErrorResponseBuilder(Response.Status.GATEWAY_TIMEOUT, result);
		}

		if (ex instanceof SecurityViolationException || ex instanceof AuthorizationException) {
			return createErrorResponseBuilder(Response.Status.FORBIDDEN, result);
		}
		
		if (ex instanceof ConfigurationException) {
			return createErrorResponseBuilder(Response.Status.BAD_GATEWAY, result);
		}
		
		if (ex instanceof SchemaException 
				|| ex instanceof NoFocusNameSchemaException 
				|| ex instanceof ExpressionEvaluationException) {
			return createErrorResponseBuilder(Response.Status.BAD_REQUEST, result);
		}

		if (ex instanceof PolicyViolationException
				|| ex instanceof ObjectAlreadyExistsException
				|| ex instanceof ConcurrencyException) {
			return createErrorResponseBuilder(Response.Status.CONFLICT, result);
		}

		return createErrorResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR, result);
	}

	public static Response.ResponseBuilder createErrorResponseBuilder(Response.Status status, OperationResult result) {
		result.computeStatusIfUnknown();
		return createErrorResponseBuilder(status, result.createOperationResultType());
	}

	public static Response.ResponseBuilder createErrorResponseBuilder(Response.Status status, OperationResultType message) {
		return Response.status(status).entity(message);
	}

	public static ModelExecuteOptions getOptions(UriInfo uriInfo){
    	List<String> options = uriInfo.getQueryParameters().get(QUERY_PARAMETER_OPTIONS);
		return ModelExecuteOptions.fromRestOptions(options);
    }

	public static Task initRequest(MessageContext mc) {
		// No need to audit login. it was already audited during authentication
		return (Task) mc.get(MESSAGE_PROPERTY_TASK_NAME);
	}

	public static void finishRequest(Task task, SecurityHelper securityHelper) {
		task.getResult().computeStatus();
		ConnectionEnvironment connEnv = ConnectionEnvironment.create(SchemaConstants.CHANNEL_REST_URI);
		connEnv.setSessionIdOverride(task.getTaskIdentifier());
		securityHelper.auditLogout(connEnv, task);
	}

	// slightly experimental
	public static Response.ResponseBuilder createResultHeaders(Response.ResponseBuilder builder, OperationResult result) {
		return builder.entity(result);
//				.header(OPERATION_RESULT_STATUS, OperationResultStatus.createStatusType(result.getStatus()).value())
//				.header(OPERATION_RESULT_MESSAGE, result.getMessage());
	}
	
	public static void createAbortMessage(ContainerRequestContext requestCtx){
		requestCtx.abortWith(Response.status(Status.UNAUTHORIZED)
				.header("WWW-Authenticate", RestAuthenticationMethod.BASIC.getMethod() + " realm=\"midpoint\", " + RestAuthenticationMethod.SECURITY_QUESTIONS.getMethod()).build());
	}
	
	public static void createSecurityQuestionAbortMessage(ContainerRequestContext requestCtx, String secQChallenge){
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
