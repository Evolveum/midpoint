/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.execution;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerConfiguration;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.execution.remote.JmxConnector;
import com.evolveum.midpoint.task.quartzimpl.execution.remote.RestConnector;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Represents remote schedulers. Concerned mainly with:
 *
 * - stopping threads and querying their state,
 * - starting/stopping scheduler and querying its state.
 *
 * TODO finish review of this class
 */
@Component
public class RemoteSchedulers {

    private static final Trace LOGGER = TraceManager.getTrace(RemoteSchedulers.class);

    public static final JobKey STARTER_JOB_KEY = JobKey.jobKey("STARTER JOB");
    public static final String CLASS_DOT = RemoteSchedulers.class.getName();

    @Autowired private ClusterManager clusterManager;
    @Autowired private JmxConnector jmxConnector;
    @Autowired private RestConnector restConnector;
    @Autowired private TaskManagerConfiguration configuration;

    public void stopRemoteScheduler(String nodeIdentifier, OperationResult result) throws SchemaException {
        NodeType node = getNode(nodeIdentifier, result);
        if (node == null) {
            return; // result is already updated
        }

        if (configuration.isUseJmx()) {
            jmxConnector.stopRemoteScheduler(node, result);
        } else {
            restConnector.stopRemoteScheduler(node, result);
        }
    }

    private NodeType getNode(String nodeIdentifier, OperationResult result) {
        try {
            return clusterManager.getNodeById(nodeIdentifier, result).asObjectable();
        } catch (ObjectNotFoundException e) {
            result.recordFatalError("A node with identifier " + nodeIdentifier + " does not exist.");
            return null;
        }
    }

    public void startRemoteScheduler(String nodeIdentifier, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(CLASS_DOT + ".startRemoteScheduler");
        result.addParam("node", nodeIdentifier);
        try {
            NodeType node = getNode(nodeIdentifier, result);
            if (node == null) {
                return; // result is already updated
            }

            if (configuration.isUseJmx()) {
                jmxConnector.startRemoteScheduler(node, result);
            } else {
                restConnector.startRemoteScheduler(node, result);
            }
        } catch (Throwable t) {
            result.recordFatalError("Couldn't start scheduler on remote node", t);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't start scheduler on remote node: {}", t, nodeIdentifier);
            // TODO throw the exception?
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    // the task should be really running
    void stopRemoteTaskRun(String oid, NodeType node, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(CLASS_DOT + ".stopRemoteTaskRun");
        result.addParam("oid", oid);
        result.addParam("node", node.toString());
        try {
            LOGGER.debug("Interrupting task {} running at {}", oid, clusterManager.dumpNodeInfo(node));
            if (configuration.isUseJmx()) {
                jmxConnector.stopRemoteTaskRun(oid, node, result);
            } else {
                restConnector.stopRemoteTaskRun(oid, node, result);
            }
        } catch (Throwable t) {
            result.recordFatalError("Couldn't stop task running on remote node", t);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't stop task running on remote node: {}", t, oid);
        } finally {
            result.computeStatusIfUnknown();
        }
    }
}
