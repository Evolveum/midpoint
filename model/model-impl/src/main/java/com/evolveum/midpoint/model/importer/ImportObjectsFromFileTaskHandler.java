/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.importer;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
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
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

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

    public static final String HANDLER_URI = ImportConstants.IMPORT_URI_PREFIX + "/handler-objects-file-1";

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
        filenamePropertyDefinition = new PrismPropertyDefinition(ImportConstants.FILENAME_PROPERTY_NAME, 
        		ImportConstants.FILENAME_PROPERTY_NAME, DOMUtil.XSD_STRING, prismContext);
    }

    @PostConstruct
    private void initialize() {
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
        result.addParam("input", input);
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

        PrismProperty<String> filenameProperty = (PrismProperty<String>) task.getExtension(ImportConstants.FILENAME_PROPERTY_NAME);
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
