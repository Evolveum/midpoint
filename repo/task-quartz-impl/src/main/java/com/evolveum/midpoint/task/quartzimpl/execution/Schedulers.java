/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.quartzimpl.execution;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.quartz.LocalScheduler;

/**
 * Manages the schedulers clusterwide.
 *
 * TODO
 */
@Component
public class Schedulers {

    private static final String CLASS_DOT = Schedulers.class.getName() + ".";
    private static final String OP_STOP_SCHEDULER = CLASS_DOT + "stopScheduler";

    @Autowired private ClusterManager clusterManager;
    @Autowired private LocalScheduler localScheduler;
    @Autowired private RemoteSchedulers remoteSchedulers;

    public void stopScheduler(String nodeIdentifier, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_STOP_SCHEDULER);
        result.addParam("nodeIdentifier", nodeIdentifier);
        try {
            if (clusterManager.isCurrentNode(nodeIdentifier)) {
                localScheduler.stopScheduler(result);
            } else {
                remoteSchedulers.stopRemoteScheduler(nodeIdentifier, result);
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public void startScheduler(String nodeIdentifier, OperationResult result) {
        if (clusterManager.isCurrentNode(nodeIdentifier)) {
            localScheduler.startScheduler();
        } else {
            remoteSchedulers.startRemoteScheduler(nodeIdentifier, result);
        }
    }
}
