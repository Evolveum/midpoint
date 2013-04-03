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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.sync.SynchronizeAccountResultHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * Task handler for "Import from resource" task.
 * <p/>
 * The import task will search for all the accounts on a specific resource. It will
 * pretend that all the accounts were just created and notify other components (mode)
 * using the ResourceObjectChangeListener interface. This will efficiently result in
 * importing all the accounts. Depending on the sync policy, appropriate user objects
 * may be created, accounts may be linked to existing users, etc.
 * <p/>
 * The handler will execute the import in background. It is using Task Manager
 * for that purpose, so the Task Manager instance needs to be injected. Most of the "import"
 * action is actually done in the callbacks from provisioning searchObjectsIterative() operation.
 * <p/>
 * The import task may be executed on a different node (as usual for async tasks).
 *
 * @author Radovan Semancik
 * @see TaskHandler
 * @see ResourceObjectChangeListener
 */
@Component
public class ImportAccountsFromResourceTaskHandler implements TaskHandler {

    public static final String HANDLER_URI = ImportConstants.IMPORT_URI_PREFIX + "/handler-accounts-resource-1";

    @Autowired(required = true)
    private ProvisioningService provisioning;

    @Autowired(required = true)
    private TaskManager taskManager;

    @Autowired(required = true)
    private ChangeNotificationDispatcher changeNotificationDispatcher;
    
    @Autowired(required = true)
    private PrismContext prismContext;
    
    private Map<Task, SynchronizeAccountResultHandler> handlers;
    private PrismPropertyDefinition objectclassPropertyDefinition;

    private static final Trace LOGGER = TraceManager.getTrace(ImportAccountsFromResourceTaskHandler.class);

    public ImportAccountsFromResourceTaskHandler() {
        super();
        handlers = new HashMap<Task, SynchronizeAccountResultHandler>();
        objectclassPropertyDefinition = new PrismPropertyDefinition(ImportConstants.OBJECTCLASS_PROPERTY_NAME, 
        		ImportConstants.OBJECTCLASS_PROPERTY_NAME, DOMUtil.XSD_QNAME, prismContext);
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
     * @param parentResult
     */
    public void launch(ResourceType resource, QName objectclass, Task task, OperationResult parentResult) {

        LOGGER.info("Launching import from resource {} as asynchronous task", ObjectTypeUtil.toShortString(resource));

        OperationResult result = parentResult.createSubresult(ImportAccountsFromResourceTaskHandler.class.getName() + ".launch");
        result.addParam("resource", resource);
        result.addParam("objectclass", objectclass);
        // TODO

        // Set handler URI so we will be called back
        task.setHandlerUri(HANDLER_URI);

        // Readable task name
        PolyStringType polyString = new PolyStringType("Import from resource " + resource.getName());
        task.setName(polyString);
        

        // Set reference to the resource
        task.setObjectRef(ObjectTypeUtil.createObjectRef(resource));

        try {
        	PrismProperty<?> objectclassProp = objectclassPropertyDefinition.instantiate();
        	objectclassProp.setRealValue(objectclass);
        	task.setExtensionProperty(objectclassProp);
        	task.savePendingModifications(result);		// just to be sure (if the task was already persistent)
//          task.modify(modifications, result);
        } catch (ObjectNotFoundException e) {
            LOGGER.error("Task object not found, expecting it to exist (task {})", task, e);
            result.recordFatalError("Task object not found", e);
            throw new IllegalStateException("Task object not found, expecting it to exist", e);
        } catch (ObjectAlreadyExistsException e) {
            LOGGER.error("Task object wasn't updated (task {})", task, e);
            result.recordFatalError("Task object wasn't updated", e);
            throw new IllegalStateException("Task object wasn't updated", e);
        } catch (SchemaException e) {
            LOGGER.error("Error dealing with schema (task {})", task, e);
            result.recordFatalError("Error dealing with schema", e);
            throw new IllegalStateException("Error dealing with schema", e);
        }

        // Switch task to background. This will start new thread and call
        // the run(task) method.
        // Note: the thread may be actually started on a different node
        taskManager.switchToBackground(task, result);
        result.computeStatus("Import launch failed");

        LOGGER.trace("Import from resource {} switched to background, control thread returning with task {}", ObjectTypeUtil.toShortString(resource), task);
    }

    /**
     * The body of the task. This will start the import "loop".
     */
    @Override
    public TaskRunResult run(Task task) {

        LOGGER.trace("Import from resource run (task {})", task);

        // This is an operation result for the entire import task. Therefore use the constant for
        // operation name.
        OperationResult opResult = task.getResult().createSubresult(OperationConstants.IMPORT_ACCOUNTS_FROM_RESOURCE);
        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(opResult);
        runResult.setProgress(0);

        // Determine resource for the import

        ResourceType resource = null;
        try {

            resource = task.getObject(ResourceType.class, opResult).asObjectable();

        } catch (ObjectNotFoundException ex) {
            String resourceOid = null;
            if (task.getObjectRef() != null) {
                resourceOid = task.getObjectRef().getOid();
            }
            LOGGER.error("Import: Resource not found: {}", resourceOid, ex);
            // This is bad. The resource does not exist. Permanent problem.
            opResult.recordFatalError("Resource not found " + resourceOid, ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        } catch (SchemaException ex) {
            LOGGER.error("Import: Error dealing with schema: {}", ex.getMessage(), ex);
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            opResult.recordFatalError("Error dealing with schema: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            return runResult;
        } catch (RuntimeException ex) {
            LOGGER.error("Import: Internal Error: {}", ex.getMessage(), ex);
            // Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense to retry.
            opResult.recordFatalError("Internal Error: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        if (resource == null) {
            LOGGER.error("Import: No resource specified");
            opResult.recordFatalError("No resource specified");
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        RefinedResourceSchema refinedSchema;
        try {
            refinedSchema = RefinedResourceSchema.getRefinedSchema(resource, LayerType.MODEL, prismContext);
        } catch (SchemaException e) {
            LOGGER.error("Import: Schema error during processing account definition: {}",e.getMessage());
            opResult.recordFatalError("Schema error during processing account definition: "+e.getMessage(),e);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }
        RefinedObjectClassDefinition refinedAccountDefinition = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);

        // Determine object class to import
        QName objectclass = null;
        PrismProperty<QName> objectclassProperty = task.getExtension(ImportConstants.OBJECTCLASS_PROPERTY_NAME);
        if (objectclassProperty != null) {
            objectclass = objectclassProperty.getValue().getValue();
        }

        if (objectclass == null) {
            if (refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT) != null) {
                objectclass = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT).getObjectClassDefinition().getTypeName();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Import: No objectclass specified, using default value of " + objectclass + ".");
                }
            }
        }

        if (objectclass == null) {
            LOGGER.error("Import: No objectclass specified and no default can be determined.");
            opResult.recordFatalError("No objectclass specified and no default can be determined");
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        LOGGER.info("Start executing import from resource {}, importing object class {}", ObjectTypeUtil.toShortString(resource), objectclass);

        // Instantiate result handler. This will be called with every search result in the following iterative search
        SynchronizeAccountResultHandler handler = new SynchronizeAccountResultHandler(resource, refinedAccountDefinition, task, changeNotificationDispatcher);
        handler.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_IMPORT);
        handler.setForceAdd(true);
        handler.setStopOnError(false);
        handler.setProcessShortName("import");

        // TODO: error checking - already running
        handlers.put(task, handler);

        try {

            provisioning.searchObjectsIterative(ResourceObjectShadowType.class,
            		ObjectQueryUtil.createResourceAndAccountQuery(resource.getOid(), objectclass, prismContext), handler, opResult);

        } catch (ObjectNotFoundException ex) {
            LOGGER.error("Import: Object not found: {}", ex.getMessage(), ex);
            // This is bad. The resource does not exist. Permanent problem.
            opResult.recordFatalError("Object not found " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            runResult.setProgress(handler.getProgress());
            return runResult;
        } catch (CommunicationException ex) {
            LOGGER.error("Import: Communication error: {}", ex.getMessage(), ex);
            // Error, but not critical. Just try later.
            opResult.recordPartialError("Communication error: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            runResult.setProgress(handler.getProgress());
            return runResult;
        } catch (SchemaException ex) {
            LOGGER.error("Import: Error dealing with schema: {}", ex.getMessage(), ex);
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            opResult.recordFatalError("Error dealing with schema: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            runResult.setProgress(handler.getProgress());
            return runResult;
        } catch (RuntimeException ex) {
            LOGGER.error("Import: Internal Error: {}", ex.getMessage(), ex);
            // Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense to retry.
            opResult.recordFatalError("Internal Error: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            runResult.setProgress(handler.getProgress());
            return runResult;
        } catch (ConfigurationException ex) {
        	LOGGER.error("Import: Configuration error: {}", ex.getMessage(), ex);
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            opResult.recordFatalError("Configuration error: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            runResult.setProgress(handler.getProgress());
            return runResult;
		} catch (SecurityViolationException ex) {
			LOGGER.error("Import: Security violation: {}", ex.getMessage(), ex);
            opResult.recordFatalError("Security violation: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            runResult.setProgress(handler.getProgress());
            return runResult;
		}

        // TODO: check last handler status

        handlers.remove(task);
        opResult.computeStatus("Errors during import");
        runResult.setProgress(handler.getProgress());
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);

        LOGGER.info("Finished import from resource {}, importing object class {}. Processed {} objects, {} errors",
                new Object[]{ObjectTypeUtil.toShortString(resource), objectclass, handler.getProgress(), handler.getErrors()});
        LOGGER.trace("Import from resource run finished (task {}, run result {})", task, runResult);

        return runResult;
    }

    private SynchronizeAccountResultHandler getHandler(Task task) {
        return handlers.get(task);
    }

    @Override
    public Long heartbeat(Task task) {
        // Delegate heartbeat to the result handler
        if (getHandler(task) != null) {
            return getHandler(task).heartbeat();
        } else {
            // most likely a race condition.
            return null;
        }
    }

    @Override
    public void refreshStatus(Task task) {
        // Local task. No refresh needed. The Task instance has always fresh data.
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.IMPORTING_ACCOUNTS;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }
}
