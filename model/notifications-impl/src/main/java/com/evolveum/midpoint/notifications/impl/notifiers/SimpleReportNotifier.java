/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.notifications.api.events.TaskEvent;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleReportNotifierType;

/**
 * @author mederly
 * @author skublik
 */
@Component
public class SimpleReportNotifier extends AbstractGeneralNotifier<TaskEvent, SimpleReportNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleReportNotifier.class);

    private static final String REPORT_HTML_CREATE_TASK_URI = "http://midpoint.evolveum.com/xml/ns/public/report/html/create/handler-3";

    @Autowired private ModelService modelService;

    @Override
    public Class<TaskEvent> getEventType() {
        return TaskEvent.class;
    }

    @Override
    public Class<SimpleReportNotifierType> getEventHandlerConfigurationType() {
        return SimpleReportNotifierType.class;
    }

    @Override
    protected boolean quickCheckApplicability(TaskEvent event, SimpleReportNotifierType configuration, OperationResult result) {
        if (event.getTask().getCategory() == null || !event.getTask().getHandlerUri().equals(REPORT_HTML_CREATE_TASK_URI)) {
            LOGGER.trace("{} is not applicable for this kind of event, continuing in the handler chain; event class = {}",
                    getClass().getSimpleName(), event.getClass());
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected String getSubject(TaskEvent event, SimpleReportNotifierType configuration, String transport, Task task, OperationResult result) {
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
    protected String getBody(TaskEvent event, SimpleReportNotifierType configuration, String transport, Task opTask, OperationResult opResult) throws SchemaException {
        Task task = event.getTask();

        String outputOid = getReportDataOid(task);

        if (outputOid == null || outputOid.isEmpty()) {
            throw new IllegalStateException("Unexpected oid of report output, oid is null or empty");
        }

        PrismObject<ReportDataType> reportOutput;
        try {
            reportOutput = modelService.getObject(ReportDataType.class, outputOid, null, opTask, opResult);
        } catch (ObjectNotFoundException | SecurityViolationException | CommunicationException | ConfigurationException
                | ExpressionEvaluationException e) {
            getLogger().error("Could't get Report output with oid " + outputOid, e);
            throw new SystemException("Couldn't get report output " + outputOid, e);
        }

        try {
            byte[] encoded = Files.readAllBytes(Paths.get(reportOutput.asObjectable().getFilePath()));
            return new String(encoded, Charset.defaultCharset());
        } catch (IOException e) {
            getLogger().error("Couldn't create body from ReportOutput with oid " + outputOid, e);
            return "";
        }
    }

    private String getReportDataOid(Task task) {
        PrismReference reportData = task.getExtensionReferenceOrClone(ReportConstants.REPORT_DATA_PROPERTY_NAME);
        if (reportData == null && reportData.getRealValue() == null) {
            PrismProperty<String> reportOutputOid = task.getExtensionPropertyRealValue(ReportConstants.REPORT_OUTPUT_OID_PROPERTY_NAME);
            if (reportOutputOid == null){
                return null;
            }
            return reportOutputOid.getRealValue();
        }

        return reportData.getRealValue().getOid();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }

    protected String getContentType() {
        return "text/html";
    }
}
