/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.tasks.imp;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.sync.tasks.NullSynchronizationObjectFilterImpl;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncTaskHelper;
import com.evolveum.midpoint.model.impl.sync.tasks.Synchronizer;
import com.evolveum.midpoint.model.impl.tasks.AbstractModelTaskHandler;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.repo.common.task.PartExecutionClass;
import com.evolveum.midpoint.repo.common.task.TaskExecutionClass;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Task handler for "Import from resource" task.
 * <p/>
 * The import task will search for all the accounts on a specific resource. It will
 * pretend that all the accounts were just created and notify other components (mode)
 * using the ResourceObjectChangeListener interface. This will effectively result in
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
@TaskExecutionClass(ImportFromResourceTaskExecution.class)
@PartExecutionClass(ImportFromResourceTaskPartExecution.class)
public class ImportFromResourceTaskHandler
        extends AbstractModelTaskHandler
        <ImportFromResourceTaskHandler, ImportFromResourceTaskExecution> {

    public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/import/handler-3";

    private static final String OP_IMPORT_SINGLE_SHADOW = ImportFromResourceTaskHandler.class.getName() + ".importSingleShadow";

    private static final Trace LOGGER = TraceManager.getTrace(ImportFromResourceTaskHandler.class);

    public ImportFromResourceTaskHandler() {
        super(LOGGER, "Import", OperationConstants.IMPORT_ACCOUNTS_FROM_RESOURCE);
        reportingOptions.setPreserveStatistics(false);
        reportingOptions.setEnableSynchronizationStatistics(true);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.IMPORTING_ACCOUNTS;
    }

    /**
     * Imports a single shadow. Synchronously. The task is NOT switched to background by default.
     */
    public boolean importSingleShadow(String shadowOid, Task task, OperationResult parentResult) throws ObjectNotFoundException,
            CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(OP_IMPORT_SINGLE_SHADOW);
        try {
            ShadowType shadow = provisioningService.getObject(ShadowType.class, shadowOid, null, task, result).asObjectable();
            SyncTaskHelper.TargetInfo targetInfo = syncTaskHelper.getTargetInfoForShadow(shadow, task, result);
            Synchronizer synchronizer = new Synchronizer(
                    targetInfo.getResource(),
                    targetInfo.getObjectClassDefinition(),
                    new NullSynchronizationObjectFilterImpl(),
                    eventDispatcher,
                    SchemaConstants.CHANNEL_IMPORT,
                    null,
                    true);
            synchronizer.synchronize(shadow.asPrismObject(), null, task, result);
            result.computeStatusIfUnknown();
            return !result.isError();
        } catch (TaskException t) {
            result.recordFatalError(t);
            throw new SystemException(t); // FIXME unwrap the exception
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown(); // just for sure
        }
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_IMPORT_TASK.value();
    }

    @Override
    public String getDefaultChannel() {
        return Channel.IMPORT.getUri();
    }

    /**
     * Launch an import. Calling this method will start import in a new
     * thread, possibly on a different node.
     */
    public void launch(ResourceType resource, QName objectclass, Task task, OperationResult parentResult) {

        LOGGER.info("Launching import from resource {} as asynchronous task", ObjectTypeUtil.toShortString(resource));

        OperationResult result = parentResult.createSubresult(ImportFromResourceTaskHandler.class.getName() + ".launch");
        result.addParam("resource", resource);
        result.addParam("objectclass", objectclass);
        // TODO

        task.setHandlerUri(HANDLER_URI);
        task.setName(new PolyStringType("Import from resource " + resource.getName()));
        task.setObjectRef(ObjectTypeUtil.createObjectRef(resource, prismContext));

        try {
            PrismPropertyDefinition<QName> objectclassPropertyDefinition = prismContext.definitionFactory()
                    .createPropertyDefinition(ModelConstants.OBJECTCLASS_PROPERTY_NAME, DOMUtil.XSD_QNAME);
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

    public ResourceObjectChangeListener getObjectChangeListener() {
        return eventDispatcher;
    }
}
