/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import org.apache.cxf.jaxrs.client.WebClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.ws.rs.core.Response;
import java.util.function.BiConsumer;

/**
 *
 *  Generally, execute(..) methods prepare a configured WebClient (URL, authentication, ...) and execute
 *  specified client code on it. The client code is responsible for setting the correct path (e.g. "/scheduler/information"),
 *  executing specified operation (e.g. GET) and interpreting the result. See callers of these methods for sample usage.
 *
 *  Other helper methods will probably appear here as well.
 */
public interface ClusterExecutionHelper {

    @FunctionalInterface
    interface ClientCode {
        void execute(WebClient client, NodeType node, OperationResult result);
    }

    /**
     * Executes operation on a specified remote node (by OID).
     */
    void execute(@NotNull String nodeOid, @NotNull ClientCode code,
            ClusterExecutionOptions options, String context, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException;

    /**
     * Executes operation on a specified remote node (by OID). If the operation does not succeed it tries to execute
     * it on other nodes (NOT on the current node).
     *
     * @return The node that was ultimately successful in executing the code (or null if none).
     */
    PrismObject<NodeType> executeWithFallback(@Nullable String nodeOid, @NotNull ClientCode code,
            ClusterExecutionOptions options, String context, OperationResult parentResult);

    /**
     * Executes operation on a specified remote node (by node object).
     *
     * @return OperationResult of the execution itself
     */
    OperationResult execute(@NotNull NodeType node, @NotNull ClientCode code,
            ClusterExecutionOptions options, String context, OperationResult parentResult)
            throws SchemaException;

    /**
     * Executes operation on all cluster nodes except for the current one.
     */
    void execute(@NotNull ClientCode code, ClusterExecutionOptions options, String context, OperationResult parentResult);

    /**
     * Extracts the result from the REST response.
     */
    <T> T extractResult(Response response, Class<T> expectedClass) throws SchemaException;
}
