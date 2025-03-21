/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.utils;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

import static com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisObjectState.SUSPENDED;
import static com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisObjectState.isStable;

/**
 * The `RoleAnalysisUtils` class contains utility methods used in the role analysis process.
 * It is used to update the role analysis operation status, submit the operation status, and remove redundant patterns.
 */
public class RoleAnalysisUtils {

    public static @Nullable RoleAnalysisOperationStatusType updateRoleAnalysisOperationStatus(
            @NotNull RepositoryService repositoryService,
            @NotNull RoleAnalysisOperationStatusType status,
            boolean isSession,
            @NotNull Trace logger,
            @NotNull OperationResult result) {
        OperationResultStatusType operationStatus = status.getStatus();
        if (operationStatus == null) {
            return null;
        }

        if (!operationStatus.equals(OperationResultStatusType.IN_PROGRESS) && !isSession) {
            return null;
        }

        if (status.getMessage() != null && isStable(status.getMessage())) {
            return null;
        }

        ObjectReferenceType taskRef = status.getTaskRef();
        if (taskRef == null || taskRef.getOid() == null) {
            return null;
        }

        PrismObject<TaskType> object;
        try {
            object = repositoryService.getObject(TaskType.class, taskRef.getOid(), null, result);
        } catch (ObjectNotFoundException | SchemaException e) {
            logger.warn("Error retrieving TaskType object for oid: {}", taskRef.getOid(), e);
            status.setStatus(OperationResultStatusType.FATAL_ERROR);
            status.setMessage(SUSPENDED.getDisplayString());
            return status;
        }

        TaskType taskType = object.asObjectable();

        if (isSession) {
            status.setMessage(updateSessionStateMessage(taskType, taskType.getExecutionState()));
        } else {
            status.setMessage(updateClusterStateMessage(taskType));
        }
        status.setModifyTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
        status.setStatus(taskType.getResultStatus());

        return status;
    }

    public static String updateClusterStateMessage(
            @NotNull TaskType taskObject) {
        String stateString = "";
        String expectedTotalString = "0";
        String actual = "0";
        TaskExecutionStateType executionState = taskObject.getExecutionState();

        TaskActivityStateType activityState = taskObject.getActivityState();
        if (activityState != null
                && activityState.getActivity() != null
                && activityState.getActivity().getProgress() != null) {
            Integer expectedTotal = activityState.getActivity().getProgress().getExpectedTotal();
            if (expectedTotal != null) {
                expectedTotalString = expectedTotal.toString();
            }
        }

        if (taskObject.getProgress() != null) {
            actual = taskObject.getProgress().toString();
            if (executionState != null) {
                stateString = "(" + actual + "/" + expectedTotalString + ") " + executionState.value();
            } else {
                stateString = "(" + actual + "/" + expectedTotalString + ")";
            }
        }

        return stateString;
    }

    public static String updateSessionStateMessage(
            @NotNull TaskType taskType,
            TaskExecutionStateType executionState) {
        String stateString = "";
        if (taskType.getProgress() != null) {
            String actual = taskType.getProgress().toString();
            if (executionState != null) {
                stateString = "(" + actual + "/" + 7 + ") " + executionState.value();
            } else {
                stateString = "(" + actual + "/" + 7 + ")";
            }
        }
        return stateString;
    }

    @NotNull
    public static RoleAnalysisOperationStatusType buildOpExecution(
            @NotNull String taskOid,
            OperationResultStatusType operationResultStatusType,
            String message,
            RoleAnalysisOperationType operationType,
            XMLGregorianCalendar createTimestamp,
            @Nullable FocusType owner) {
        RoleAnalysisOperationStatusType operationExecutionType = new RoleAnalysisOperationStatusType();
        XMLGregorianCalendar xmlGregorianCalendar = XmlTypeConverter.createXMLGregorianCalendar(new Date());

        if (createTimestamp == null) {
            createTimestamp = xmlGregorianCalendar;
        }

        if (owner != null) {
            operationExecutionType.setInitiatorRef(new ObjectReferenceType()
                    .oid(owner.getOid())
                    .targetName(owner.getName())
                    .type(UserType.COMPLEX_TYPE));
        }

        operationExecutionType.createTimestamp(createTimestamp);
        operationExecutionType.modifyTimestamp(xmlGregorianCalendar);
        operationExecutionType.setStatus(operationResultStatusType);
        operationExecutionType.setOperationChannel(operationType);
        operationExecutionType.setTaskRef(
                new ObjectReferenceType()
                        .oid(taskOid)
                        .type(TaskType.COMPLEX_TYPE));
        if (message != null) {
            operationExecutionType.setMessage(message);
        }
        return operationExecutionType;
    }

    public static void submitClusterOperationStatus(
            @NotNull ModelService modelService,
            @NotNull PrismObject<RoleAnalysisClusterType> cluster,
            @NotNull String taskOid,
            @NotNull RoleAnalysisOperationType operationChannel,
            @NotNull FocusType initiator,
            Trace logger,
            @NotNull Task task,
            @NotNull OperationResult result) {

        @NotNull RoleAnalysisOperationStatusType operationStatus = buildOpExecution(
                taskOid,
                OperationResultStatusType.IN_PROGRESS,
                null,
                operationChannel,
                null,
                initiator);

        try {
            ObjectDelta<Objectable> objectDelta = PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_OPERATION_STATUS)
                    .add(operationStatus.clone())
                    .asObjectDelta(cluster.getOid());

            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);

            modelService.executeChanges(deltas, null, task, result);
        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            logger.error("Couldn't add operation status {}", cluster.getOid(), e);
        }
    }

    public static void submitSessionOperationStatus(
            @NotNull ModelService modelService,
            @NotNull PrismObject<RoleAnalysisSessionType> cluster,
            @NotNull String taskOid,
            @NotNull FocusType initiator,
            @NotNull Trace logger,
            @NotNull Task task,
            @NotNull OperationResult result) {

        @NotNull RoleAnalysisOperationStatusType operationStatus = buildOpExecution(
                taskOid,
                OperationResultStatusType.IN_PROGRESS,
                null,
                RoleAnalysisOperationType.CLUSTERING,
                null,
                initiator);

        try {
            ObjectDelta<RoleAnalysisSessionType> objectDelta = PrismContext.get().deltaFor(RoleAnalysisSessionType.class)
                    .item(RoleAnalysisSessionType.F_OPERATION_STATUS)
                    .replace(operationStatus.clone())
                    .asObjectDelta(cluster.getOid());

            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);

            modelService.executeChanges(deltas, null, task, result);
        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            logger.error("Couldn't add operation status {}", cluster.getOid(), e);
        }
    }
}
