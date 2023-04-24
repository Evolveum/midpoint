/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl;

import jakarta.annotation.PostConstruct;
import javax.ws.rs.core.Response;

import com.evolveum.midpoint.model.api.util.ClusterServiceConsts;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.authentication.api.config.NodeAuthenticationToken;
import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.repo.api.CacheListener;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ClusterExecutionHelper;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@Component
public class ClusterCacheListener implements CacheListener {

    private static final Trace LOGGER = TraceManager.getTrace(ClusterCacheListener.class);

    @Autowired private TaskManager taskManager;
    @Autowired private CacheDispatcher cacheDispatcher;
    @Autowired private ClusterExecutionHelper clusterExecutionHelper;

    @PostConstruct
    public void addListener() {
        cacheDispatcher.registerCacheListener(this);
    }

    @Override
    public <O extends ObjectType> void invalidate(Class<O> type, String oid, boolean clusterwide,
            CacheInvalidationContext context) {

        if (!canExecute(type, oid, clusterwide, context)) {
            return;
        }

        Task task = taskManager.createTaskInstance("invalidate");
        OperationResult result = task.getResult();

        LOGGER.trace("Cache invalidation context {}", context);

        // Regular cache invalidation can be skipped for nodes not checking in. Cache entries will expire on such nodes
        // eventually. (We can revisit this design decision if needed.)
        clusterExecutionHelper.execute((client, node, result1) -> {
            client.path(getInvalidationRestPath(type, oid));
            Response response = client.post(null);
            Response.StatusType statusInfo = response.getStatusInfo();
            if (statusInfo.getFamily() != Response.Status.Family.SUCCESSFUL) {
                LOGGER.warn("Cluster-wide cache clearance finished on {} with status {}, {}", node.getNodeIdentifier(),
                        statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
            } else {
                LOGGER.debug("Cluster-wide cache clearance finished on {} with status {}, {}", node.getNodeIdentifier(),
                        statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
            }
            response.close();
        }, null, "cache invalidation", result);
    }

    @NotNull
    private <O extends ObjectType> String getInvalidationRestPath(Class<O> type, String oid) {
        StringBuilder sb = new StringBuilder(ClusterServiceConsts.EVENT_INVALIDATION);
        if (type != null) {
            sb.append(ObjectTypes.getRestTypeFromClass(type));
            if (oid != null) {
                sb.append("/").append(oid);
            }
        } else {
            if (oid != null) {
                LOGGER.warn("Cannot invalidate object type null with specific OID. Converting to global invalidation (type=null, oid=null).");
            }
        }
        return sb.toString();
    }

    private <O extends ObjectType> boolean canExecute(Class<O> type, String oid, boolean clusterwide, CacheInvalidationContext context) {
        if (!clusterwide) {
            LOGGER.trace("Ignoring invalidate() call for type {} (oid={}) because clusterwide=false", type, oid);
            return false;
        }

        if (!taskManager.isClustered()) {
            LOGGER.trace("Node is not part of a cluster, skipping remote cache entry invalidation");
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof NodeAuthenticationToken || context != null && context.isFromRemoteNode()) {
            // This is actually a safety check only. The invalidation call coming from the other node
            // should be redistributed with clusterwide=false.
            LOGGER.warn("Skipping cluster-wide cache invalidation as this is already a remotely-invoked invalidate() call");
            return false;
        }

        return true;
    }
}
