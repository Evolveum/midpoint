/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.WorkBucketStatisticsCollector;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class ReleaseBucketOperation extends BucketOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ReleaseBucketOperation.class);

    private final int sequentialNumber;

    ReleaseBucketOperation(WorkStateManager workStateManager, @NotNull String workerTaskOid,
            @NotNull ActivityPath activityPath, WorkBucketStatisticsCollector collector, int sequentialNumber) {
        super(workerTaskOid, activityPath, collector, workStateManager);
        this.sequentialNumber = sequentialNumber;
    }

    public void execute(OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {

        loadTasks(result);
        LOGGER.trace("Releasing bucket {} in {} (coordinator {})", sequentialNumber, workerTask, coordinatorTask);

        if (isStandalone()) {
            throw new UnsupportedOperationException("Cannot release work bucket from standalone task " + workerTask);
        } else {
            releaseWorkBucketMultiNode(sequentialNumber, result);
        }
    }

    private void releaseWorkBucketMultiNode(int sequentialNumber, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        ActivityWorkStateType workState = getCoordinatorTaskPartWorkState();
        WorkBucketType bucket = TaskWorkStateUtil.findBucketByNumber(workState.getBucket(), sequentialNumber);
        if (bucket == null) {
            throw new IllegalStateException("No work bucket with sequential number of " + sequentialNumber + " in " + coordinatorTask);
        }
        if (bucket.getState() != WorkBucketStateType.DELEGATED) {
            throw new IllegalStateException("Work bucket " + sequentialNumber + " in " + coordinatorTask
                    + " cannot be released, as it is not delegated; its state = " + bucket.getState());
        }
        checkWorkerRefOnDelegatedBucket(bucket);
        try {
            repositoryService.modifyObject(TaskType.class, coordinatorTask.getOid(),
                    bucketStateChangeDeltas(coordinatorStatePath, bucket, WorkBucketStateType.READY, null),
                    bucketUnchangedPrecondition(bucket), null, result);
        } catch (PreconditionViolationException e) {
            // just for sure
            throw new IllegalStateException("Unexpected concurrent modification of work bucket " + bucket + " in " + coordinatorTask, e);
        }
        deleteBucketFromWorker(sequentialNumber, result);
        statisticsKeeper.register(RELEASE_WORK_BUCKET);
    }

    @Override
    protected void extendDebugDump(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "sequentialNumber", sequentialNumber, indent + 1);
    }
}
