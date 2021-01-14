/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.importer;

import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.impl.sync.SynchronizationObjectsFilter;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.sync.SynchronizeAccountResultHandler;
import com.evolveum.midpoint.model.impl.util.AbstractSearchIterativeModelTaskHandler;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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
public class ImportAccountsFromResourceTaskHandler extends AbstractSearchIterativeModelTaskHandler<ShadowType, SynchronizeAccountResultHandler> {

    public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/import/handler-3";

    // WARNING! This task handler is efficiently singleton!
     // It is a spring bean and it is supposed to handle all search task instances
     // Therefore it must not have task-specific fields. It can only contain fields specific to
     // all tasks of a specified type

    @Autowired private TaskManager taskManager;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private ChangeNotificationDispatcher changeNotificationDispatcher;
    @Autowired private SchemaHelper schemaHelper;

    private PrismPropertyDefinition<QName> objectclassPropertyDefinition;

    private static final Trace LOGGER = TraceManager.getTrace(ImportAccountsFromResourceTaskHandler.class);

    public ImportAccountsFromResourceTaskHandler() {
        super("Import from resource", OperationConstants.IMPORT_ACCOUNTS_FROM_RESOURCE);
        setLogFinishInfo(true);
        setPreserveStatistics(false);
        setEnableSynchronizationStatistics(true);
    }

    @PostConstruct
    private void initialize() {
        // this call must not be in the constructor, because prismContext is not yet initialized at that moment
        objectclassPropertyDefinition = prismContext.definitionFactory().createPropertyDefinition(ModelConstants.OBJECTCLASS_PROPERTY_NAME,
                DOMUtil.XSD_QNAME);

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
        task.setObjectRef(ObjectTypeUtil.createObjectRef(resource, prismContext));

        try {
            PrismProperty<QName> objectclassProp = objectclassPropertyDefinition.instantiate();
            objectclassProp.setRealValue(objectclass);
            task.setExtensionProperty(objectclassProp);
            task.flushPendingModifications(result);        // just to be sure (if the task was already persistent)
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

        task.addArchetypeInformationIfMissing(SystemObjectsType.ARCHETYPE_IMPORT_TASK.value());

        // Switch task to background. This will start new thread and call
        // the run(task) method.
        // Note: the thread may be actually started on a different node
        taskManager.switchToBackground(task, result);
        result.setBackgroundTaskOid(task.getOid());
        result.computeStatus("Import launch failed");

        LOGGER.trace("Import from resource {} switched to background, control thread returning with task {}", ObjectTypeUtil.toShortString(resource), task);
    }

    @Override
    protected SynchronizeAccountResultHandler createHandler(TaskPartitionDefinitionType partition, TaskRunResult runResult, RunningTask coordinatorTask,
            OperationResult opResult) {
        ResourceType resource = resolveObjectRef(ResourceType.class, runResult, coordinatorTask, opResult);
        if (resource == null) {
            return null;
        }
        return createHandler(resource, null, coordinatorTask, partition, runResult, opResult);
    }

    // shadowToImport - it is used to derive objectClass/intent/kind when importing a single shadow
    private SynchronizeAccountResultHandler createHandlerForSingleShadow(@NotNull ResourceType resource, @NotNull PrismObject<ShadowType> shadowToImport,
            TaskRunResult runResult, RunningTask task, OperationResult opResult) {
        return createHandler(resource, shadowToImport, task, null, runResult, opResult);
    }

    private SynchronizeAccountResultHandler createHandler(@NotNull ResourceType resource,
            @Nullable PrismObject<ShadowType> shadowToImport, RunningTask coordinatorTask, TaskPartitionDefinitionType partition,
            TaskRunResult runResult, OperationResult opResult) {

        ObjectClassComplexTypeDefinition objectClass;
        try {
            RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource, LayerType.MODEL, prismContext);

            LOGGER.trace("Refined schema:\n{}", refinedSchema.debugDumpLazily());

            if (shadowToImport != null) {
                objectClass = ModelImplUtils.determineObjectClass(refinedSchema, shadowToImport);
            } else {
                objectClass = ModelImplUtils.determineObjectClass(refinedSchema, coordinatorTask);
            }
            if (objectClass == null) {
                LOGGER.error("Import: No objectclass specified and no default can be determined.");
                opResult.recordFatalError("No objectclass specified and no default can be determined");
                runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
                return null;
            }
        } catch (SchemaException e) {
            LOGGER.error("Import: Schema error during processing account definition: {}",e.getMessage());
            opResult.recordFatalError("Schema error during processing account definition: "+e.getMessage(),e);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
        }

        LOGGER.info("Start executing import from resource {}, importing object class {}", resource, objectClass.getTypeName());

        SynchronizationObjectsFilter objectsFilter = ModelImplUtils.determineSynchronizationObjectsFilter(objectClass, coordinatorTask);

        SynchronizeAccountResultHandler handler = new SynchronizeAccountResultHandler(resource, objectClass, objectsFilter, "import",
                coordinatorTask, changeNotificationDispatcher, partition, taskManager);
        handler.setSourceChannel(SchemaConstants.CHANNEL_IMPORT);
        handler.setForceAdd(true);
        handler.setStopOnError(false);
        handler.setContextDesc("from "+resource);
        handler.setLogObjectProgress(true);

        return handler;
    }

    @Override
    protected Class<? extends ObjectType> getType(Task task) {
        return ShadowType.class;
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> createSearchOptions(
            SynchronizeAccountResultHandler resultHandler, TaskRunResult runResult, Task coordinatorTask,
            OperationResult opResult) {
        Collection<SelectorOptions<GetOperationOptions>> defaultOptions = schemaHelper.getOperationOptionsBuilder()
                .doNotDiscovery(false)
                .errorReportingMethod(FetchErrorReportingMethodType.FETCH_RESULT)
                .build();
        Collection<SelectorOptions<GetOperationOptions>> configuredOptions =
                super.createSearchOptions(resultHandler, runResult, coordinatorTask, opResult);

        // It is questionable if "do not discovery" and "error reporting" can be overridden from the task
        // or not. Let us assume reasonable administrators and allow the overriding. Otherwise we would swap the arguments below.
        return GetOperationOptions.merge(prismContext, defaultOptions, configuredOptions);
    }

    @Override
    protected ObjectQuery createQuery(SynchronizeAccountResultHandler handler, TaskRunResult runResult, Task task, OperationResult opResult) {
        try {
            ObjectQuery query = createQueryFromTaskIfExists(handler, runResult, task, opResult);
            if (query != null) {
                return query;
            } else {
                return ObjectQueryUtil.createResourceAndObjectClassQuery(handler.getResourceOid(),
                        handler.getObjectClass().getTypeName(), prismContext);
            }
        } catch (SchemaException e) {
            LOGGER.error("Import: Schema error during creating search query: {}",e.getMessage());
            opResult.recordFatalError("Schema error during creating search query: "+e.getMessage(),e);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
        }
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.IMPORTING_ACCOUNTS;
    }

    /**
     * Imports a single shadow. Synchronously. The task is NOT switched to background by default.
     */
    public boolean importSingleShadow(String shadowOid, Task task, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, shadowOid, null, task, parentResult);
        PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, ShadowUtil.getResourceOid(shadow), null, task, parentResult);

        // Create a result handler just for one object. Invoke the handle() method manually.
        TaskRunResult runResult = new TaskRunResult();
        RunningTask fakeRunningTask = taskManager.createFakeRunningTask(task);
        SynchronizeAccountResultHandler resultHandler = createHandlerForSingleShadow(resource.asObjectable(), shadow, runResult, fakeRunningTask, parentResult);
        if (resultHandler == null) {
            return false;
        }
        // This is required for proper error reporting
        resultHandler.setStopOnError(true);

        if (!initializeRun(resultHandler, runResult, task, parentResult)) {
            return false;
        }

        if (!resultHandler.handle(shadow, parentResult)) {
            return false;
        }

        finish(resultHandler, runResult, fakeRunningTask, parentResult);

        return true;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_IMPORT_TASK.value();
    }

    @Override
    public String getDefaultChannel() {
        return Channel.IMPORT.getUri();
    }
}
