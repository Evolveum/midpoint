/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.TaskEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleReportNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * @author mederly
 * @author skublik
 */
@Component
public class SimpleReportNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleReportNotifier.class);

    private static final String REPORT_HTML_CREATE_TASK_URI = "http://midpoint.evolveum.com/xml/ns/public/report/html/create/handler-3";

    @Autowired private ModelService modelService;

    @PostConstruct
    public void init() {
        register(SimpleReportNotifierType.class);
    }

    @Override
    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!(event instanceof TaskEvent) || ((TaskEvent)event).getTask() == null
                || ((TaskEvent)event).getTask().getCategory() == null || !((TaskEvent)event).getTask().getHandlerUri().equals(REPORT_HTML_CREATE_TASK_URI)) {
            LOGGER.trace("{} is not applicable for this kind of event, continuing in the handler chain; event class = {}", getClass().getSimpleName(), event.getClass());
            return false;
        }
        return true;
    }

    @Override
    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        return true;
    }

    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {
        final TaskEvent taskEvent = (TaskEvent) event;
        final String taskName = PolyString.getOrig(taskEvent.getTask().getName());

        if (event.isAdd()) {
            return "Task '" + taskName + "' start notification";
        } else if (event.isDelete()) {
            return "Task '" + taskName + "' finish notification: " + taskEvent.getOperationResultStatus();
        } else {
            return "(unknown " + taskName + " operation)";
        }
    }

    @Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, Task opTask, OperationResult opResult) throws SchemaException {
        final TaskEvent taskEvent = (TaskEvent) event;
        final Task task = taskEvent.getTask();


        String outputOid = task.getExtensionPropertyRealValue(ReportConstants.REPORT_OUTPUT_OID_PROPERTY_NAME);

        if (outputOid == null || outputOid.isEmpty()) {
            throw new IllegalStateException("Unexpected oid of report output, oid is null or empty");
        }

        PrismObject<ReportOutputType> reportOutput = null;
        try {
            reportOutput = modelService.getObject(ReportOutputType.class, outputOid, null, opTask, opTask.getResult());
        } catch (ObjectNotFoundException | SecurityViolationException | CommunicationException | ConfigurationException
                | ExpressionEvaluationException e) {
            getLogger().error("Could't get Report output with oid " + outputOid, e);
        }

        if (reportOutput == null) {
            throw new IllegalStateException("Unexpected report output, report output is null");
        }

        String body = "";
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(reportOutput.asObjectable().getFilePath()));
            body = new String(encoded, Charset.defaultCharset());
        } catch (IOException e) {
            getLogger().error("Couldn't create body from ReportOutput with oid " + outputOid, e);
        }
          return body;
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }

    protected String getContentType() {
        return "text/html";
    }

}
