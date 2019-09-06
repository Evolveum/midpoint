/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import org.apache.cxf.jaxrs.client.WebClient;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 *  Helps with the intra-cluster remote code execution.
 *
 *  Generally, execute(..) methods prepare a configured WebClient (URL, authentication, ...) and execute
 *  specified client code on it. The client code is responsible for setting the correct path (e.g. "/scheduler/information"),
 *  executing specified operation (e.g. GET) and interpreting the result. See callers of these methods for sample usage.
 *
 *  Other helper methods will probably appear here as well.
 */
public interface ClusterExecutionHelper {

	/**
	 * Executes operation on a specified remote node (by OID).
	 */
	void execute(@NotNull String nodeOid, @NotNull BiConsumer<WebClient, OperationResult> code, String context, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException;

	/**
	 * Executes operation on a specified remote node (by node object).
	 */
	void execute(@NotNull NodeType node, @NotNull BiConsumer<WebClient, OperationResult> code, String context, OperationResult parentResult)
			throws SchemaException;

	/**
	 * Executes operation on all cluster nodes except for the current one.
	 * TODO what to do with dead nodes?
	 */
	void execute(@NotNull BiConsumer<WebClient, OperationResult> code, String context, OperationResult parentResult);


	/**
	 * Extracts the result from the REST response.
	 */
	<T> T extractResult(Response response, Class<T> expectedClass) throws SchemaException;

}
