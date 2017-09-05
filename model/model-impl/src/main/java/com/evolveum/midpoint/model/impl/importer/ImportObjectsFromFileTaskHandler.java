/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.impl.importer;

import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.io.File;
import java.util.List;

/**
 * Task handler for "Import objects from file" task.
 * <p/>
 * Import parses the input file and add all objects to the repository.
 * <p/>
 * The import task might be executed on a different node (as usual for async tasks), but this won't work as the file
 * is not serializable. Therefore the task needs to be locked to the executing node. (TODO)
 *
 * @author Radovan Semancik
 * @see TaskHandler
 * @see ResourceObjectChangeListener
 */
@Component
public class ImportObjectsFromFileTaskHandler implements TaskHandler {

    public static final String HANDLER_URI = ModelConstants.NS_IMPORT_OBJECTS_TASK_PREFIX + "/file/handler-3";

    @Autowired(required = true)
    private TaskManager taskManager;

    @Autowired(required = true)
    private ChangeNotificationDispatcher changeNotificationDispatcher;

    @Autowired(required = true)
    private PrismContext prismContext;

    //private Map<Task,ImportAccountsFromResourceResultHandler> handlers;
    private PrismPropertyDefinition filenamePropertyDefinition;

    private static final Trace LOGGER = TraceManager.getTrace(ImportObjectsFromFileTaskHandler.class);

    public ImportObjectsFromFileTaskHandler() {
        super();
        //handlers = new HashMap<Task, ImportAccountsFromResourceResultHandler>();
    }

    @PostConstruct
    private void initialize() {
        filenamePropertyDefinition = new PrismPropertyDefinitionImpl(ModelConstants.FILENAME_PROPERTY_NAME,
                DOMUtil.XSD_STRING, prismContext);          // must not be in the constructor, because prismContext is null at that time
        taskManager.registerHandler(HANDLER_URI, this);
    }

    /**
     * Launch an import. Calling this method will start import in a new
     * thread, possibly on a different node.
     *
     * @param input
     * @param task
     * @param parentResult
     */
    public void launch(File input, Task task, OperationResult parentResult) {

        LOGGER.debug("Launching import accounts from file {}", input);

        OperationResult result = parentResult.createSubresult(ImportObjectsFromFileTaskHandler.class.getName() + ".launch");
        result.addParam("input", input.getPath());
        // TODO

        // Set handler URI so we will be called back
        task.setHandlerUri(HANDLER_URI);

        // Readable task name
        PolyStringType polyString = new PolyStringType("Import from file " + input);
        task.setName(polyString);

        // TODO: bind task to this node

        // Set filename
//        Collection<? extends ItemDelta> modifications = new ArrayList<ItemDelta>(1);
//        PropertyDelta objectClassDelta = new PropertyDelta<Object>(
//        		new PropertyPath(TaskType.F_EXTENSION, filenamePropertyDefinition.getName()),
//        		filenamePropertyDefinition);
//        objectClassDelta.setValueToReplace(new PrismPropertyValue<Object>(input.getAbsolutePath()));
//        ((Collection)modifications).add(objectClassDelta);
        try {
        	PrismProperty filenameProp = filenamePropertyDefinition.instantiate();
        	filenameProp.setRealValue(input.getAbsolutePath());
        	task.setExtensionProperty(filenameProp);
        	task.savePendingModifications(result);
//            task.modify(modifications, result);
        } catch (ObjectNotFoundException e) {
            LOGGER.error("Task object not found, expecting it to exist (task {})", task, e);
            result.recordFatalError("Task object not found", e);
            throw new IllegalStateException("Task object not found, expecting it to exist", e);
        } catch (ObjectAlreadyExistsException e) {
            LOGGER.error("Task object was not updated (task {})", task, e);
            result.recordFatalError("Task object was not updated", e);
            throw new IllegalStateException("Task object was not updated", e);
        } catch (SchemaException e) {
            LOGGER.error("Error dealing with schema (task {})", task, e);
            result.recordFatalError("Error dealing with schema", e);
            throw new IllegalStateException("Error dealing with schema", e);
        }

        // Switch task to background. This will start new thread and call
        // the run(task) method.
        // Note: the thread may be actually started on a different node
        taskManager.switchToBackground(task, result);
		result.setBackgroundTaskOid(task.getOid());

        LOGGER.trace("Import objects from file {} switched to background, control thread returning with task {}", input, task);
    }

    /**
     * The body of the task. This will start the import "loop".
     */
    @Override
    public TaskRunResult run(Task task) {

        LOGGER.debug("Import objects from file run (task {})", task);

        // This is an operation result for the entire import task. Therefore use the constant for
        // operation name.
        OperationResult opResult = task.getResult().createSubresult(OperationConstants.IMPORT_OBJECTS_FROM_FILE);
        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(opResult);
        runResult.setProgress(0);

        // Determine the input file from task extension

        PrismProperty<String> filenameProperty = task.getExtensionProperty(ModelConstants.FILENAME_PROPERTY_NAME);
        if (filenameProperty == null) {
            LOGGER.error("Import: No file specified");
            opResult.recordFatalError("No file specified");
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        String filename = filenameProperty.getValue().getValue();
        if (filename == null) {
            LOGGER.error("Import: No file specified");
            opResult.recordFatalError("No file specified");
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        File input = new File(filename);

        // TODO: test file existence, etc.

        // TODO: do import

        opResult.computeStatus("Errors during import");
        // TODO: runResult.setProgress(progress);
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);

        LOGGER.debug("Import objects from file run finished (task {}, run result {})", task, runResult);

        return runResult;
    }


    @Override
    public Long heartbeat(Task task) {
        // Delegate heartbeat to the result handler
        //TODO: return getHandler(task).heartbeat();
        throw new NotImplementedException();
    }

    @Override
    public void refreshStatus(Task task) {
        // Local task. No refresh needed. The Task instance has always fresh data.
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.IMPORT_FROM_FILE;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
