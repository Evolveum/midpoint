/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 */
public class TaskDto extends Selectable {

    private String oid;
    private String name;
    private String category;
    private ObjectReferenceType objectRef;
    private TaskExecutionStatus execution;
    private OperationResultStatus status;

    //helpers, won't be probably shown
    private Long lastRunStartTimestampLong;
    private Long lastRunFinishTimestampLong;
    private Long nextRunStartTimeLong;
    private String executesAt;
    private TaskBinding binding;
    private TaskRecurrence recurrence;

    public TaskDto(Task task, ClusterStatusInformation clusterStatusInfo, TaskManager taskManager) {
        Validate.notNull(task, "Task must not be null.");
        oid = task.getOid();
        name = task.getName();
        category = task.getCategory();
//        objectRef = ;
        execution = task.getExecutionStatus();       //todo is this alright?
//        scheduledToStartAgain=task.;
        lastRunFinishTimestampLong = task.getLastRunFinishTimestamp();
        lastRunStartTimestampLong = task.getLastRunStartTimestamp();
        nextRunStartTimeLong = task.getNextRunStartTime();

        if (clusterStatusInfo == null) {         // when listing nodes currently executing at this node
            this.executesAt = taskManager.getNodeId();
//            this.execution = TaskItemExecutionStatus.RUNNING;
        } else {
            Node node = clusterStatusInfo.findNodeInfoForTask(this.getOid());
            if (node != null) {
                this.executesAt = node.getNodeType().asObjectable().getNodeIdentifier();
//                this.executionStatus = TaskItemExecutionStatus.RUNNING;
            } else {
                this.executesAt = null;
//                this.executionStatus = TaskItemExecutionStatus.fromTask(task.getExecutionStatus());
            }
        }

        this.binding = task.getBinding();
        this.recurrence = task.getRecurrenceStatus();

        OperationResult result = task.getResult();
        if (result != null) {
            status = result.getStatus();
        }
    }

    public String getCategory() {
        return category;
    }

    public Long getCurrentRuntime() {
        if (isRunNotFinished()) {
            if (isAliveClusterwide()) {
                return System.currentTimeMillis() - lastRunStartTimestampLong;
            }
        }

        return null;
    }

    public TaskExecutionStatus getExecution() {
        return execution;
    }

    public String getName() {
        return name;
    }

    public ObjectReferenceType getObjectRef() {
        return objectRef;
    }

    public String getOid() {
        return oid;
    }

    public Long getScheduledToStartAgain() {
        long current = System.currentTimeMillis();
        if (!TaskRecurrence.RECURRING.equals(recurrence)) {
            return null;
        } else if (getExecution() != TaskExecutionStatus.RUNNABLE) {
            return null;
        } else if (TaskBinding.TIGHT.equals(binding)) {
            return -1L;
        }
//    	else if (taskRunNotFinished()) {
//    		return "to be computed";
        else if (nextRunStartTimeLong != null && nextRunStartTimeLong > 0) {
            if (nextRunStartTimeLong > current + 1000) {
                return nextRunStartTimeLong - System.currentTimeMillis();
            } else {
                return 0L;
            }
        } else {
            return null;
            // either a task is not recurring, or the next run start time has not been determined yet
            // TODO: if necessary, this could be made more clear in the future
        }
    }

    public OperationResultStatus getStatus() {
        return status;
    }

    private boolean isRunNotFinished() {
        return lastRunStartTimestampLong != null &&
                (lastRunFinishTimestampLong == null || lastRunStartTimestampLong > lastRunFinishTimestampLong);
    }

    private boolean isAliveClusterwide() {
        return executesAt != null;
    }
}
