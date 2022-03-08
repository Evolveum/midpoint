/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.notifiers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.notifications.api.events.TaskEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Notifier for reports, which is a task notifier.
 * This also supports old legacy tasks with handler URI - at least for now.
 */
@Component
public class SimpleReportNotifier extends AbstractGeneralNotifier<TaskEvent, SimpleReportNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleReportNotifier.class);

    private static final String REPORT_TASK_URI = "http://midpoint.evolveum.com/xml/ns/public/report/handler-3";

    @Autowired private ModelService modelService;
    @Autowired private ObjectResolver resolver;

    @Override
    public Class<TaskEvent> getEventType() {
        return TaskEvent.class;
    }

    @Override
    public Class<SimpleReportNotifierType> getEventHandlerConfigurationType() {
        return SimpleReportNotifierType.class;
    }

    @Override
    protected boolean quickCheckApplicability(
            TaskEvent event, SimpleReportNotifierType configuration, OperationResult result) {
        @NotNull Task task = event.getTask();
        String legacyHandlerUri = task.getHandlerUri();
        if (!(legacyHandlerUri != null && task.getHandlerUri().equals(REPORT_TASK_URI)
                || hasReportTaskActivityDefinition(task))) {
            LOGGER.trace("{} is not applicable for this event: {}", getClass().getSimpleName(), event);
            return false;
        }

        if (!event.isSuccess() || !event.isFinished()) {
            LOGGER.trace("Report task event not successful or not finished: {}", event);
            return false;
        }

        return true;
    }

    private boolean hasReportTaskActivityDefinition(Task task) {
        ActivityDefinitionType activity = task.getRootActivityDefinitionOrClone();
        if (activity == null) {
            return false;
        }
        WorkDefinitionsType workDef = activity.getWork();
        if (workDef == null) {
            return false;
        }
        return workDef.getReportExport() != null || workDef.getDistributedReportExport() != null;
    }

    @Override
    protected String getSubject(TaskEvent event, SimpleReportNotifierType configuration,
            String transport, Task task, OperationResult result) {
        final String taskName = PolyString.getOrig(event.getTask().getName());

        if (event.isAdd()) {
            return "Task '" + taskName + "' start notification";
        } else if (event.isDelete()) {
            return "Task '" + taskName + "' finish notification: " + event.getOperationResultStatus();
        } else {
            return "(unknown " + taskName + " operation)";
        }
    }

    @Override
    protected List<NotificationMessageAttachmentType> getAttachment(TaskEvent event,
            SimpleReportNotifierType generalNotifierType, String transportName, Task task, OperationResult result) {

        String outputOid = getReportDataOid(event.getTask());

        if (outputOid == null || outputOid.isEmpty()) {
            throw new IllegalStateException("Unexpected oid of report output, oid is null or empty");
        }

        PrismObject<ReportDataType> reportOutput;
        try {
            reportOutput = modelService.getObject(ReportDataType.class, outputOid, null, task, result);
        } catch (ObjectNotFoundException | SecurityViolationException | CommunicationException | ConfigurationException
                | ExpressionEvaluationException | SchemaException e) {
            getLogger().error("Couldn't get Report output with oid " + outputOid, e);
            throw new SystemException("Couldn't get report output " + outputOid, e);
        }

        NotificationMessageAttachmentType attachment = new NotificationMessageAttachmentType();
        String type;
        String filePath = reportOutput.asObjectable().getFilePath();
        if (reportOutput.asObjectable().getFileFormat() != null) {
            type = reportOutput.asObjectable().getFileFormat().value().toLowerCase();
        } else {
            type = FilenameUtils.getExtension(filePath);
        }
        if (StringUtils.isBlank(type)) {
            type = "plain";
        }
        attachment.setContentType("text/" + type);
        attachment.setContentFromFile(filePath);
        List<NotificationMessageAttachmentType> attachments = new ArrayList<>();
        attachments.add(attachment);
        return attachments;
    }

    @Override
    protected String getBody(TaskEvent event, SimpleReportNotifierType configuration,
            String transport, Task opTask, OperationResult opResult) throws SchemaException {
        Task task = event.getTask();
        PrismObject<ReportType> report = getReportFromTask(task, opTask, opResult);

        return "Notification about creating of report.\n\n"
                + "Report: " + report.getName() + "\n\n"
                + "You can see report output in attachment." + "\n";
    }

    private PrismObject<ReportType> getReportFromTask(Task task, Task opTask, OperationResult opResult) {
        try {
            if (hasReportTaskActivityDefinition(task)) {
                ObjectReferenceType ref;
                WorkDefinitionsType workDef = task.getRootActivityDefinitionOrClone().getWork();
                if (workDef.getReportExport() != null) {
                    ref = workDef.getReportExport().getReportRef();
                } else {
                    ref = workDef.getDistributedReportExport().getReportRef();
                }
                ReportType report = resolver.resolve(
                        ref,
                        ReportType.class,
                        null,
                        "resolving report",
                        opTask,
                        opResult);
                return report.asPrismObject();
            }
            return task.getObject(ReportType.class, opResult);
        } catch (CommonException e) {
            getLogger().error("Couldn't get Report from task " + task.debugDump(), e);
            throw new SystemException("Couldn't get Report from task " + task.debugDump(), e);
        }
    }

    private @Nullable String getReportDataOid(Task task) {
        PrismReference reportData = task.getExtensionReferenceOrClone(ReportConstants.REPORT_DATA_PROPERTY_NAME);
        if (reportData == null || reportData.getRealValue() == null) {
            return task.getExtensionPropertyRealValue(ReportConstants.REPORT_OUTPUT_OID_PROPERTY_NAME);
        }

        return reportData.getRealValue().getOid();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}
