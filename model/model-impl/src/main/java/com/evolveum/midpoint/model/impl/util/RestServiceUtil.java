/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.security.SecurityHelper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import org.apache.cxf.jaxrs.ext.MessageContext;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * @author mederly (only copied existing code)
 */
public class RestServiceUtil {

	public static final String MESSAGE_PROPERTY_TASK_NAME = "task";
	private static final String QUERY_PARAMETER_OPTIONS = "options";

	public static Response handleException(Exception ex) {
		if (ex instanceof ObjectNotFoundException) {
			return buildErrorResponse(Response.Status.NOT_FOUND, ex);
		}

		if (ex instanceof CommunicationException) {
			return buildErrorResponse(Response.Status.GATEWAY_TIMEOUT, ex);
		}

		if (ex instanceof SecurityViolationException) {
			return buildErrorResponse(Response.Status.FORBIDDEN, ex);
		}

		if (ex instanceof ConfigurationException) {
			return buildErrorResponse(Response.Status.BAD_GATEWAY, ex);
		}

		if (ex instanceof SchemaException
				|| ex instanceof PolicyViolationException
				|| ex instanceof ConsistencyViolationException
				|| ex instanceof ObjectAlreadyExistsException) {
			return buildErrorResponse(Response.Status.CONFLICT, ex);
		}

		return buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, ex);
	}

	public static Response buildErrorResponse(Response.Status status, Exception ex) {
		return buildErrorResponse(status, ex.getMessage());
	}

	public static Response buildErrorResponse(Response.Status status, String message) {
		return Response.status(status).entity(message).type(MediaType.TEXT_PLAIN).build();
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
		ConnectionEnvironment connEnv = new ConnectionEnvironment();
		connEnv.setChannel(SchemaConstants.CHANNEL_REST_URI);
		connEnv.setSessionId(task.getTaskIdentifier());
		securityHelper.auditLogout(connEnv, task);
	}
}
