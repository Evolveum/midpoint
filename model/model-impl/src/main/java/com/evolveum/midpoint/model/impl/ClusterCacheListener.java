/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.TerminateSessionEvent;
import com.evolveum.midpoint.model.impl.security.NodeAuthenticationToken;
import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.repo.api.CacheInvalidationDetails;
import com.evolveum.midpoint.repo.api.CacheListener;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.ClusterExecutionHelper;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.TerminateSessionEventType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        if (context != null && context.isTerminateSession()) {
            CacheInvalidationDetails details = context.getDetails();
            if (!(details instanceof TerminateSessionEvent)) {
                LOGGER.warn("Cannot perform cluster-wide session termination, no details provided.");
                return;
            }
            clusterExecutionHelper.execute((client, result1) -> {
                client.path(ClusterRestService.EVENT_TERMINATE_SESSION);

                Response response = client.post(((TerminateSessionEvent)details).toEventType());
                LOGGER.info("Cluster-wide cache clearance finished with status {}, {}", response.getStatusInfo().getStatusCode(),
                        response.getStatusInfo().getReasonPhrase());
                response.close();
            }, "session termination", result);
            return;
        }



        clusterExecutionHelper.execute((client, result1) -> {
            client.path(ClusterRestService.EVENT_INVALIDATION +
                    ObjectTypes.getRestTypeFromClass(type) + (oid != null ? "/" + oid : ""));
            Response response = client.post(null);
            LOGGER.info("Cluster-wide cache clearance finished with status {}, {}", response.getStatusInfo().getStatusCode(),
                    response.getStatusInfo().getReasonPhrase());
            response.close();
        }, "cache invalidation", result);
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
