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

import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.sync.SynchronizeAccountResultHandler;
import com.evolveum.midpoint.model.util.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.model.util.AbstractSearchIterativeTaskHandler;
import com.evolveum.midpoint.model.util.Utils;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
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
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
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
public class ImportAccountsFromResourceTaskHandler extends AbstractSearchIterativeTaskHandler<ShadowType> {

    public static final String HANDLER_URI = ImportConstants.IMPORT_URI_TASK_PREFIX + "/accounts-resource/handler-2";

    @Autowired(required = true)
    private TaskManager taskManager;

    @Autowired(required = true)
    private ChangeNotificationDispatcher changeNotificationDispatcher;
    
    private ResourceType resource;
    private RefinedObjectClassDefinition rObjectClass;
    private PrismPropertyDefinition<QName> objectclassPropertyDefinition;

    private static final Trace LOGGER = TraceManager.getTrace(ImportAccountsFromResourceTaskHandler.class);

    public ImportAccountsFromResourceTaskHandler() {
        super(ShadowType.class, "Import from resource", OperationConstants.IMPORT_ACCOUNTS_FROM_RESOURCE);
        objectclassPropertyDefinition = new PrismPropertyDefinition<QName>(ImportConstants.OBJECTCLASS_PROPERTY_NAME, 
        		ImportConstants.OBJECTCLASS_PROPERTY_NAME, DOMUtil.XSD_QNAME, prismContext);
        setLogFinishInfo(true);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    /**
     * Launch an import. Calling this method will start import in a new
     * thread, possibly on a different node.
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

    
    
    @Override
	protected boolean initialize(TaskRunResult runResult, Task task, OperationResult opResult) {
		boolean cont = super.initialize(runResult, task, opResult);
		if (!cont) {
			return cont;
		}
		
		resource = resolveObjectRef(ResourceType.class, runResult, task, opResult);
		if (resource == null) {
			return false;
		}
		
		RefinedResourceSchema refinedSchema;
        try {
            refinedSchema = RefinedResourceSchema.getRefinedSchema(resource, LayerType.MODEL, prismContext);
        } catch (SchemaException e) {
            LOGGER.error("Import: Schema error during processing account definition: {}",e.getMessage());
            opResult.recordFatalError("Schema error during processing account definition: "+e.getMessage(),e);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return false;
        }

        if (LOGGER.isTraceEnabled()) {
        	LOGGER.trace("Refined schema:\n{}", refinedSchema.dump());
        }
        
        rObjectClass = Utils.determineObjectClass(refinedSchema, task);        
        if (rObjectClass == null) {
            LOGGER.error("Import: No objectclass specified and no default can be determined.");
            opResult.recordFatalError("No objectclass specified and no default can be determined");
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return false;
        }
        
        LOGGER.info("Start executing import from resource {}, importing object class {}", resource, rObjectClass.getTypeName());
        
        return true;
	}

	@Override
	protected ObjectQuery createQuery(TaskRunResult runResult, Task task, OperationResult opResult) {
        try {
			return ObjectQueryUtil.createResourceAndAccountQuery(resource.getOid(), rObjectClass.getTypeName(), prismContext);
		} catch (SchemaException e) {
			LOGGER.error("Import: Schema error during creating search query: {}",e.getMessage());
            opResult.recordFatalError("Schema error during creating search query: "+e.getMessage(),e);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
		}
	}

	@Override
	protected AbstractSearchIterativeResultHandler<ShadowType> createHandler(TaskRunResult runResult, Task task,
			OperationResult opResult) {
		
		SynchronizeAccountResultHandler handler = new SynchronizeAccountResultHandler(resource, rObjectClass, "import", 
        		task, changeNotificationDispatcher);
        handler.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_IMPORT);
        handler.setForceAdd(true);
        handler.setStopOnError(false);
        handler.setContextDesc("from "+resource);
        
        return handler;
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
