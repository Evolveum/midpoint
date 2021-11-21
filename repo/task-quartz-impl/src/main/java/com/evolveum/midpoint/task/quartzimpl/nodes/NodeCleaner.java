/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.nodes;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.schema.statistics.Operation;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.schema.statistics.IterativeOperationStartInfo;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.util.TimeBoundary;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DeadNodeCleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

import java.util.function.Predicate;

/** TODO merge with some relevant class */
@Component
public class NodeCleaner {
    private static final Trace LOGGER = TraceManager.getTrace(NodeCleaner.class);

    @Autowired private ClusterManager clusterManager;
    @Autowired private RepositoryService repositoryService;

    /**
     * Cleans up dead nodes older than specified age.
     *
     * @param selector If returns false, the respective node will not be removed.
     */
    public void cleanupNodes(@NotNull DeadNodeCleanupPolicyType policy, @NotNull Predicate<NodeType> selector,
            @NotNull RunningTask task, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {
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
                IterativeOperationStartInfo iterativeOperationStartInfo = new IterativeOperationStartInfo(new IterationItemInformation(node));
                iterativeOperationStartInfo.setSimpleCaller(true);
                Operation op = task.recordIterativeOperationStart(iterativeOperationStartInfo);
                if (ObjectTypeUtil.isIndestructible(node)) {
                    LOGGER.debug("Not deleting dead but indestructible node {}", node);
                    op.skipped();
                    continue;
                }

                try {
                    // Selector testing is in try-catch because of possible exceptions during autz evaluation
                    if (!selector.test(node.asObjectable())) {
                        LOGGER.debug("Not deleting node {} because it was rejected by the selector", node);
                        op.skipped();
                        continue;
                    }
                    repositoryService.deleteObject(NodeType.class, node.getOid(), result);
                    op.succeeded();
                } catch (Throwable t) {
                    op.failed(t);
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete dead node {}", t, node);
                }
                task.incrementLegacyProgressAndStoreStatisticsIfTimePassed(result);
            }
        }
    }
}
