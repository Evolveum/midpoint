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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

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
 * Task handler for "Import from resource" task.
 * 
 * The import task will search for all the accounts on a specific resource. It will
 * pretend that all the accounts were just created and notify other components (mode)
 * using the ResourceObjectChangeListener interface. This will efficiently result in
 * importing all the accounts. Depending on the sync policy, appropriate user objects
 * may be created, accounts may be linked to existing users, etc.
 * 
 * The handler will execute the import in background. It is using Task Manager
 * for that purpose, so the Task Manager instance needs to be injected. Most of the "import"
 * action is actually done in the callbacks from provisioning searchObjectsIterative() operation.
 * 
 * The import task may be executed on a different node (as usual for async tasks).
 * 
 * @see TaskHandler
 * @see ResourceObjectChangeListener
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class ImportAccountsFromResourceTaskHandler implements TaskHandler {
	
	public static final String HANDLER_URI = ImportConstants.IMPORT_URI_PREFIX + "/handler-accounts-resource-1";

	@Autowired(required=true)
	private ProvisioningService provisioning;
	
	@Autowired(required=true)
	private TaskManager taskManager;

	@Autowired(required=true)
	private ChangeNotificationDispatcher changeNotificationDispatcher;
	
	private Map<Task,ImportAccountsFromResourceResultHandler> handlers;
	private PropertyDefinition objectclassPropertyDefinition;
	
	private static final Trace logger = TraceManager.getTrace(ImportAccountsFromResourceTaskHandler.class);
	
	public ImportAccountsFromResourceTaskHandler() {
		super();
		handlers = new HashMap<Task, ImportAccountsFromResourceResultHandler>();
		objectclassPropertyDefinition = new PropertyDefinition(ImportConstants.OBJECTCLASS_PROPERTY_NAME, DOMUtil.XSD_QNAME);
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
	public void launch(ResourceType resource, QName objectclass, Task task, OperationResult parentResult) {
				
		logger.debug("Launching import from resource {}",ObjectTypeUtil.toShortString(resource));
		
		OperationResult result = parentResult.createSubresult(ImportAccountsFromResourceTaskHandler.class.getName()+".launch");
		result.addParam("resource", resource);
		result.addParam("objectclass",objectclass);
		// TODO
		
		// Set handler URI so we will be called back
		task.setHandlerUri(HANDLER_URI);
		
		// Readable task name
		task.setName("Import from resource "+resource.getName());
		
		// Set reference to the resource
		task.setObjectRef(ObjectTypeUtil.createObjectRef(resource));
		
		// Set objectclass
		Property objectclassProperty = objectclassPropertyDefinition.instantiate();
		objectclassProperty.setValue(objectclass);
		PropertyModification modification = objectclassProperty.createModification(
				ModificationType.REPLACE, objectclass);
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
		
		logger.trace("Import from resource {} switched to background, control thread returning with task {}",ObjectTypeUtil.toShortString(resource),task);
	}

	/**
	 * The body of the task. This will start the import "loop".
	 */
	@Override
	public TaskRunResult run(Task task) {
		
		logger.debug("Import from resource run (task {})",task);
		
		// This is an operation result for the entire import task. Therefore use the constant for
		// operation name.
		OperationResult opResult = task.getResult().createSubresult(OperationConstants.IMPORT_ACCOUNTS_FROM_RESOURCE);
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);
		runResult.setProgress(0);

		// Determine resource for the import
		
		ResourceType resource = null;
		try {
			
			ObjectType object = task.getObject(opResult);
			resource = (ResourceType)object;
			
		} catch (ObjectNotFoundException ex) {
			String resourceOid = null;
			if (task.getObjectRef()!=null) {
				resourceOid = task.getObjectRef().getOid();
			}
			logger.error("Import: Resource not found: {}",resourceOid,ex);
			// This is bad. The resource does not exist. Permanent problem.
			opResult.recordFatalError("Resource not found "+resourceOid,ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			return runResult;
		} catch (SchemaException ex) {
			logger.error("Import: Error dealing with schema: {}",ex.getMessage(),ex);
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			opResult.recordFatalError("Error dealing with schema: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			return runResult;
		} catch (RuntimeException ex) {
			logger.error("Import: Internal Error: {}",ex.getMessage(),ex);
			// Can be anything ... but we can't recover from that.
			// It is most likely a programming error. Does not make much sense to retry.
			opResult.recordFatalError("Internal Error: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			return runResult;
		}
		
		if (resource == null) {
			logger.error("Import: No resource specified");
			opResult.recordFatalError("No resource specified");
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			return runResult;
		}
		
		// Determine object class to import
		Property objectclassProperty = task.getExtension(ImportConstants.OBJECTCLASS_PROPERTY_NAME);
		if (objectclassProperty == null) {
			logger.error("Import: No objectclass specified");
			opResult.recordFatalError("No objectclass specified");
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			return runResult;
		}
		
		QName objectclass = objectclassProperty.getValue(QName.class);
		if (objectclass == null) {
			logger.error("Import: No objectclass specified");
			opResult.recordFatalError("No objectclass specified");
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			return runResult;
		}
		
		// Instantiate result handler. This will be called with every search result in the following iterative search
		ImportAccountsFromResourceResultHandler handler = new ImportAccountsFromResourceResultHandler(resource,task,changeNotificationDispatcher);
		
		// TODO: error checking - already running
		handlers.put(task, handler);
		
		try {
			
			provisioning.searchObjectsIterative(createAccountShadowTypeQuery(resource, objectclass), null, handler, opResult);
			
		} catch (ObjectNotFoundException ex) {
			logger.error("Import: Object not found: {}",ex.getMessage(),ex);
			// This is bad. The resource does not exist. Permanent problem.
			opResult.recordFatalError("Object not found "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(handler.getProgress());
			return runResult;
		} catch (CommunicationException ex) {
			logger.error("Import: Communication error: {}",ex.getMessage(),ex);
			// Error, but not critical. Just try later.
			opResult.recordPartialError("Communication error: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(handler.getProgress());
			return runResult;
		} catch (SchemaException ex) {
			logger.error("Import: Error dealing with schema: {}",ex.getMessage(),ex);
			// Not sure about this. But most likely it is a misconfigured resource or connector
			// It may be worth to retry. Error is fatal, but may not be permanent.
			opResult.recordFatalError("Error dealing with schema: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
			runResult.setProgress(handler.getProgress());
			return runResult;
		} catch (RuntimeException ex) {
			logger.error("Import: Internal Error: {}",ex.getMessage(),ex);
			// Can be anything ... but we can't recover from that.
			// It is most likely a programming error. Does not make much sense to retry.
			opResult.recordFatalError("Internal Error: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(handler.getProgress());
			return runResult;
		}		

		// TODO: check last handler status
		
		handlers.remove(task);
		opResult.computeStatus("Errors during import");
		runResult.setProgress(handler.getProgress());
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		
		logger.debug("Import from resource run finished (task {}, run result {})",task,runResult);
		
		return runResult;
	}
	
	private QueryType createAccountShadowTypeQuery(ResourceType resource, QName objectClass) {
		
		Document doc = DOMUtil.getDocument();
        Element filter =
                QueryUtil.createAndFilter(doc,
	                // TODO: The account type is hardcoded now, it should determined
	                // from the schema later, or maybe we can make it entirely
	                // generic (use ResourceObjectShadowType instead).
	                QueryUtil.createTypeFilter(doc, QNameUtil.qNameToUri(SchemaConstants.I_ACCOUNT_SHADOW_TYPE)),
	                QueryUtil.createEqualRefFilter(doc, null, SchemaConstants.I_RESOURCE_REF,resource.getOid()),
	                QueryUtil.createEqualFilter(doc, null, SchemaConstants.I_OBJECT_CLASS,objectClass)
                );
		
		QueryType query = new QueryType();
        query.setFilter(filter);

        return query;
	}
	
	private ImportAccountsFromResourceResultHandler getHandler(Task task) {
		return handlers.get(task);
	}

	@Override
	public long heartbeat(Task task) {
		// Delegate heartbeat to the result handler
		return getHandler(task).heartbeat();
	}

	@Override
	public void refreshStatus(Task task) {
		// Local task. No refresh needed. The Task instance has always fresh data.
	}

}
