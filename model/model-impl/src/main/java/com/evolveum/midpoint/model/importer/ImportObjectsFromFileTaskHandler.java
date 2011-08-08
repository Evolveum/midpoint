/**
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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.model.importer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.common.result.OperationConstants;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.PropertyModification;
import com.evolveum.midpoint.schema.processor.PropertyModification.ModificationType;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * Task handler for "Import objects from file" task.
 * 
 * Import parses the input file and add all objects to the repository.
 * 
 * The import task might be executed on a different node (as usual for async tasks), but this won't work as the file
 * is not serializable. Therefore the task needs to be locked to the executing node. (TODO)
 * 
 * @see TaskHandler
 * @see ResourceObjectChangeListener
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class ImportObjectsFromFileTaskHandler implements TaskHandler {
	
	public static final String HANDLER_URI = ImportConstants.IMPORT_URI_PREFIX + "/handler-objects-file-1";

	@Autowired(required=true)
	private RepositoryService repositoryService;
	
	@Autowired(required=true)
	private TaskManager taskManager;

	@Autowired(required=true)
	private ChangeNotificationDispatcher changeNotificationDispatcher;
	
	//private Map<Task,ImportAccountsFromResourceResultHandler> handlers;
	private PropertyDefinition filenamePropertyDefinition;
	
	private static final Trace logger = TraceManager.getTrace(ImportObjectsFromFileTaskHandler.class);
	
	public ImportObjectsFromFileTaskHandler() {
		super();
		//handlers = new HashMap<Task, ImportAccountsFromResourceResultHandler>();
		filenamePropertyDefinition = new PropertyDefinition(ImportConstants.FILENAME_PROPERTY_NAME, SchemaConstants.XSD_STRING);
	}

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}
	
	/**
	 * Launch an import. Calling this method will start import in a new
	 * thread, possibly on a different node.
	 * 
	 * @param resource
	 * @param task
	 * @param manager
	 */
	public void launch(File input, Task task, OperationResult parentResult) {
				
		logger.debug("Launching import accounts from file {}",input);
		
		OperationResult result = parentResult.createSubresult(ImportObjectsFromFileTaskHandler.class.getName()+".launch");
		result.addParam("input", input);
		// TODO
		
		// Set handler URI so we will be called back
		task.setHandlerUri(HANDLER_URI);
		
		// Readable task name
		task.setName("Import from file "+input);
		
		// TODO: bind task to this node
		
		// Set filename
		Property filenameProperty = filenamePropertyDefinition.instantiate();
		PropertyModification modification = filenameProperty.createModification(
				ModificationType.REPLACE, input.getAbsolutePath());
		List<PropertyModification> modifications = new ArrayList<PropertyModification>();
		modifications.add(modification);
		try {
			task.modifyExtension(modifications, result);
		} catch (ObjectNotFoundException e) {
			logger.error("Task object not found, expecting it to exist (task {})",task,e);
			result.recordFatalError("Task object not found", e);
			throw new IllegalStateException("Task object not found, expecting it to exist",e);
		} catch (SchemaException e) {
			logger.error("Error dealing with schema (task {})",task,e);
			result.recordFatalError("Error dealing with schema", e);
			throw new IllegalStateException("Error dealing with schema",e);
		}
		
		// Switch task to background. This will start new thread and call
		// the run(task) method.
		// Note: the thread may be actually started on a different node
		taskManager.switchToBackground(task, result);
		
		logger.trace("Import objects from file {} switched to background, control thread returning with task {}",input,task);
	}

	/**
	 * The body of the task. This will start the import "loop".
	 */
	@Override
	public TaskRunResult run(Task task) {
		
		logger.debug("Import objects from file run (task {})",task);
		
		// This is an operation result for the entire import task. Therefore use the constant for
		// operation name.
		OperationResult opResult = task.getResult().createSubresult(OperationConstants.IMPORT_OBJECTS_FROM_FILE);
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);
		runResult.setProgress(0);
		
		// Determine the input file from task extension
		
		Property filenameProperty = task.getExtension(ImportConstants.FILENAME_PROPERTY_NAME);
		if (filenameProperty == null) {
			logger.error("Import: No file specified");
			opResult.recordFatalError("No file specified");
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			return runResult;
		}

		String filename = filenameProperty.getValue(String.class);
		if (filename == null) {
			logger.error("Import: No file specified");
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
		
		logger.debug("Import objects from file run finished (task {}, run result {})",task,runResult);
		
		return runResult;
	}
	
	
	@Override
	public long heartbeat(Task task) {
		// Delegate heartbeat to the result handler
		//TODO: return getHandler(task).heartbeat();
		throw new NotImplementedException();
	}

	@Override
	public void refreshStatus(Task task) {
		// Local task. No refresh needed. The Task instance has always fresh data.
	}

}
