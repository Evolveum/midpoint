/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.nodes;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.util.TimeBoundary;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DeadNodeCleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

/** TODO merge with some relevant class */
@Component
public class NodeCleaner {
    private static final Trace LOGGER = TraceManager.getTrace(NodeCleaner.class);

    @Autowired private ClusterManager clusterManager;
    @Autowired private RepositoryService repositoryService;

    public void cleanupNodes(DeadNodeCleanupPolicyType policy, RunningTask task, OperationResult result) {
        if (policy.getMaxAge() == null) {
            return;
        }

        TimeBoundary timeBoundary = TimeBoundary.compute(policy.getMaxAge());
        XMLGregorianCalendar deleteNodesNotCheckedInAfter = timeBoundary.getBoundary();

        LOGGER.info("Starting cleanup for stopped nodes not checked in after {} (duration '{}').",
                deleteNodesNotCheckedInAfter, timeBoundary.getPositiveDuration());
        for (PrismObject<NodeType> node : clusterManager.getAllNodes(result)) {
            if (!task.canRun()) {
                result.recordWarning("Interrupted");
                LOGGER.warn("Node cleanup was interrupted.");
                break;
            }
            if (!clusterManager.isCurrentNode(node) &&
                    !clusterManager.isCheckingIn(node.asObjectable()) &&
                    XmlTypeConverter.compareMillis(node.asObjectable().getLastCheckInTime(), deleteNodesNotCheckedInAfter) <= 0) {
                // This includes last check in time == null
                LOGGER.info("Deleting dead node {}; last check in time = {}", node, node.asObjectable().getLastCheckInTime());
                String nodeName = PolyString.getOrig(node.getName());
                long started = System.currentTimeMillis();
                try {
                    task.recordIterativeOperationStart(nodeName, null, NodeType.COMPLEX_TYPE, node.getOid());
                    repositoryService.deleteObject(NodeType.class, node.getOid(), result);
                    task.recordIterativeOperationEnd(nodeName, null, NodeType.COMPLEX_TYPE, node.getOid(), started, null);
                } catch (Throwable t) {
                    task.recordIterativeOperationEnd(nodeName, null, NodeType.COMPLEX_TYPE, node.getOid(), started, t);
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete dead node {}", t, node);
                }
            }
        }
    }
}
