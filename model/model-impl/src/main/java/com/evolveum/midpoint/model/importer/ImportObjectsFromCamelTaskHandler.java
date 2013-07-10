/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.importer;

import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType;
import org.apache.camel.*;
import org.apache.commons.lang.NotImplementedException;
import org.jvnet.jaxb2_commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;

/**
 * Task handler for "Import objects from Camel" task.
 * <p/>
 * Import parses the input message and add all objects to the repository.
 * <p/>
 *
 * @author Radovan Semancik
 * @author Pavol Mederly
 * @see com.evolveum.midpoint.task.api.TaskHandler
 */
@Component
public class ImportObjectsFromCamelTaskHandler implements TaskHandler, Processor {

    public static final String HANDLER_URI = ImportConstants.IMPORT_URI_PREFIX + "/handler-objects-camel-1";

    @Autowired(required = true)
    private TaskManager taskManager;

    @Autowired(required = true)
    private ModelController modelController;

    @Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = false)                // for tests to run without problems (todo fixme later)
    private CamelContext camelContext;

    private static final Trace LOGGER = TraceManager.getTrace(ImportObjectsFromCamelTaskHandler.class);
    private static final long CHECK_TIME_FOR_TASK_SHUTDOWN = 500L;
    private static final long CHECK_TIME_FOR_CAMEL_START = 2000L;

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    /**
     * The body of the task. This will start the import "loop".
     */
    @Override
    public TaskRunResult run(Task task) {

        LOGGER.debug("Import objects from Camel (task {})", task);

        // This is an operation result for the entire import task. Therefore use the constant for
        // operation name.
        OperationResult opResult = task.getResult().createSubresult(OperationConstants.IMPORT_OBJECTS_FROM_CAMEL);
        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(opResult);
        long progress = task.getProgress();
        runResult.setProgress(progress);

        while (camelContext.getStatus() != ServiceStatus.Started && task.canRun()) {

            LOGGER.info("Waiting for Camel to start (sleeping for " + CHECK_TIME_FOR_CAMEL_START + " ms)");
            try {
                Thread.sleep(CHECK_TIME_FOR_CAMEL_START);
            } catch (InterruptedException e) {
                // do nothing; if we should stop, the task.canRun() will return false
            }
        }

        if (!task.canRun()) {
            LOGGER.warn("The import task is shutting down but Camel did not even start; task = " + task);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);      // todo think about this
            return runResult;
        }

        // Determine the input endpoint/route from task extension

        PrismProperty<String> endpointProperty = task.getExtensionProperty(ImportConstants.ENDPOINT_PROPERTY_NAME);
        PrismProperty<String> routeProperty = task.getExtensionProperty(ImportConstants.ROUTE_PROPERTY_NAME);

        if (endpointProperty == null && routeProperty == null) {
            LOGGER.error("Import: Neither endpoint nor route specified");
            opResult.recordFatalError("Import: Neither endpoint nor route specified");
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        if (endpointProperty != null && routeProperty != null) {
            LOGGER.error("Import: Both endpoint and route(s) specified - you must choose one of them");
            opResult.recordFatalError("Import: Both endpoint and route(s) specified - you must choose one of them");
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        if (endpointProperty != null) {

            String endpointName = endpointProperty.getValue().getValue();
            if (StringUtils.isEmpty(endpointName)) {
                LOGGER.error("Import: No endpoint specified");
                opResult.recordFatalError("No endpoint specified");
                runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
                return runResult;
            }

            progress = processEndpoint(endpointName, progress, task, opResult);

        } else {
            Collection<String> routeNames = routeProperty.getRealValues();
            if (routeNames == null || routeNames.isEmpty()) {       // actually, this should not occur, as property IS present
                LOGGER.error("Import: No routes specified");
                opResult.recordFatalError("No routes specified");
                runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
                return runResult;
            }

            processRoutes(routeNames, task, opResult);
        }

        // todo think about reporting status & progress - currently they would get reported only at task shutdown...
        opResult.computeStatus("Errors during import");
        runResult.setProgress(progress);
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);

        LOGGER.debug("Import objects from Camel run finished (task {}, run result {})", task, runResult);

        return runResult;
    }

    // enclosing task is expected to be single-run; it serves just as a "wrapper" around camel service threads
    // that will do the actual work;
    //
    // these threads will be started on task startup, and stopped on task shutdown (via route start/stop mechanism)
    //
    private void processRoutes(Collection<String> routeNames, Task task, OperationResult opResult) {
        LOGGER.info("Starting Camel routes: " + routeNames);

        boolean atLeastOneStarted = false;

        for (String routeName : routeNames) {
            OperationResult result = opResult.createSubresult(OperationConstants.IMPORT_OBJECTS_FROM_CAMEL + ".startRoute");
            result.addParam("routeName", routeName);
            try {
                camelContext.startRoute(routeName);
                result.recordSuccess();
                atLeastOneStarted = true;
            } catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Couldn't start route " + routeName, e);
                result.recordFatalError("Couldn't start route " + routeName + ": " + e.getMessage());
            }
        }

        if (!atLeastOneStarted) {
            return;     // todo: perhaps we should suspend the task, not close it...
        }

        // now let's wait until the task is shut down
        while (task.canRun()) {
            try {
                Thread.sleep(CHECK_TIME_FOR_TASK_SHUTDOWN);
            } catch (InterruptedException e) {
                break;      // probably someone wants to stop this thread
            }
        }

        LOGGER.info("Stopping Camel routes: " + routeNames);

        for (String routeName : routeNames) {
            OperationResult result = opResult.createSubresult(OperationConstants.IMPORT_OBJECTS_FROM_CAMEL + ".stopRoute");
            result.addParam("routeName", routeName);
            try {
                camelContext.stopRoute(routeName);
                result.recordSuccess();
            } catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Couldn't stop route " + routeName, e);
                result.recordFatalError("Couldn't stop route " + routeName + ": " + e.getMessage());
            }
        }

    }

    // this kind of task is meant to be recurring, mainly in order to SUSPEND in case of any problems
    // (resuming a task is easier than restarting closed task, which would be the case if enclosing task
    // would be single-run)
    //
    private long processEndpoint(String endpointName, long progress, Task task, OperationResult opResult) {
        LOGGER.info("Getting messages from Camel endpoint " + endpointName);
        Endpoint endpoint = camelContext.getEndpoint(endpointName);
        try {
            endpoint.start();
        } catch (Exception e) {
            throw new SystemException("Couldn't start the endpoint " + endpointName, e);
        }

        PollingConsumer consumer = null;
        try {
            try {
                consumer = endpoint.createPollingConsumer();
                consumer.start();
            } catch (Exception e) {
                throw new SystemException("Couldn't create polling consumer on Camel endpoint " + endpointName, e);
            }
            while (task.canRun()) {
                Exchange exchange = consumer.receive();

                process1(exchange, task, opResult);

                if (exchange.getUnitOfWork() != null) {
                    LOGGER.trace("Finishing the unit of work.");
                    exchange.getUnitOfWork().done(exchange);
                } else {
                    LOGGER.trace("No unit of work to finish.");
                }

                progress++;
            }
        } finally {
            if (consumer != null) {
                try {
                    consumer.stop();
                } catch (Exception e) {
                    LoggingUtils.logException(LOGGER, "Couldn't stop consumer on endpoint " + endpointName, e);
                }
            }
            try {
                endpoint.stop();
            } catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Couldn't stop endpoint " + endpointName, e);
            }
        }
        return progress;
    }

    @Override
    public void process(Exchange exchange) throws Exception {

        // maybe we could pass the task to this thread via camel context somehow...
        // currently the operation result is not shown in the task; it is only returned to the client (if possible)
        Task dummyTask = taskManager.createTaskInstance();
        OperationResult opResult = new OperationResult(OperationConstants.IMPORT_OBJECTS_FROM_CAMEL);

        process1(exchange, dummyTask, opResult);
    }

    private void process1(Exchange exchange, Task task, OperationResult opResult) {

        String message = exchange.getIn().getBody(String.class);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Got message:\n", message);
        }
        byte[] messageAsBytes;
        try {
            messageAsBytes = message.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new SystemException(e);       // should not occur
        }

        // todo - specify options directly in the request message
        ImportOptionsType options = new ImportOptionsType();
        options.setEncryptProtectedValues(true);
        options.setOverwrite(true);
        options.setSummarizeSucceses(false);
        options.setSummarizeErrors(false);
        options.setReferentialIntegrity(false);
        options.setValidateStaticSchema(true);
        options.setValidateDynamicSchema(true);
        options.setEncryptProtectedValues(true);
        options.setFetchResourceSchema(false);

        modelController.importObjectsFromStream(new ByteArrayInputStream(messageAsBytes), options, task, opResult);

        OperationResult lastResult = opResult.getLastSubresult();
        if (lastResult.isSuccess()) {
            LOGGER.info("Input message was imported successfully; task = " + task);
        } else {
            LOGGER.warn("Input message was NOT imported successfully; task = " + task);
            LOGGER.warn("Operation Result: " + lastResult.debugDump(2));
        }

        // todo for import-form-endpoint here's a problem how to send the response back (TODO!)
        String response;
        try {
            OperationResultType resultType = lastResult.createOperationResultType();
            ObjectFactory of = new ObjectFactory();
            response = prismContext.getPrismJaxbProcessor().marshalElementToString(of.createOperationResult(resultType));
        } catch (JAXBException ex) {
            LoggingUtils.logException(LOGGER, "Can't create xml with OperationResult", ex);     // TODO
            response = ex.getMessage();     // TODO !!!!!!!!!!!!!!!
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Preparing and sending response message having contents:\n" + response);
        }
        exchange.getOut().setBody(response);
    }

    @Override
    public Long heartbeat(Task task) {
        // Delegate heartbeat to the result handler
        //TODO: return getHandler(task).heartbeat();
        throw new NotImplementedException();
    }

    @Override
    public void refreshStatus(Task task) {
        // not implemented yet
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.IMPORT_FROM_FILE;       // todo
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }
}
