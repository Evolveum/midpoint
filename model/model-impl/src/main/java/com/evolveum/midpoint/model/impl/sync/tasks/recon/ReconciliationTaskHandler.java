/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.sync.tasks.SyncTaskHelper;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.tasks.AbstractModelTaskHandler;
import com.evolveum.midpoint.model.impl.util.AuditHelper;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.task.TaskExecutionClass;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * The task handler for reconciliation.
 *
 * This handler takes care of executing reconciliation "runs". It means that the
 * handler "run" method will be as scheduled (every few days). The
 * responsibility is to iterate over accounts and compare the real state with
 * the assumed IDM state.
 *
 * @author Radovan Semancik
 *
 */
@Component
@TaskExecutionClass(ReconciliationTaskExecution.class)
public class ReconciliationTaskHandler
        extends AbstractModelTaskHandler
        <ReconciliationTaskHandler, ReconciliationTaskExecution> {

    /**
     * Just for testability. Used in tests. Injected by explicit call to a
     * setter.
     */
    private ReconciliationTaskResultListener reconciliationTaskResultListener;

    @Autowired EventDispatcher eventDispatcher;
    @Autowired AuditHelper auditHelper;
    @Autowired protected ExpressionFactory expressionFactory;
    @Autowired SchemaService schemaService;
    @Autowired SyncTaskHelper syncTaskHelper;

    @Autowired CacheConfigurationManager cacheConfigurationManager;

    private static final Trace LOGGER = TraceManager.getTrace(ReconciliationTaskHandler.class);

    protected ReconciliationTaskHandler() {
        super(LOGGER, "Reconciliation", OperationConstants.RECONCILIATION);
        reportingOptions.setPreserveStatistics(false);
        reportingOptions.setEnableSynchronizationStatistics(true);
    }

    public ReconciliationTaskResultListener getReconciliationTaskResultListener() {
        return reconciliationTaskResultListener;
    }

    public void setReconciliationTaskResultListener(ReconciliationTaskResultListener reconciliationTaskResultListener) {
        this.reconciliationTaskResultListener = reconciliationTaskResultListener;
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(ModelPublicConstants.RECONCILIATION_TASK_HANDLER_URI, this);
        taskManager.registerAdditionalHandlerUri(ModelPublicConstants.PARTITIONED_RECONCILIATION_TASK_HANDLER_URI_1, this);
        taskManager.registerAdditionalHandlerUri(ModelPublicConstants.PARTITIONED_RECONCILIATION_TASK_HANDLER_URI_2, this);
        taskManager.registerAdditionalHandlerUri(ModelPublicConstants.PARTITIONED_RECONCILIATION_TASK_HANDLER_URI_3, this);
    }

    /**
     * Launch an import. Calling this method will start import in a new
     * thread, possibly on a different node.
     */
    public void launch(ResourceType resource, QName objectclass, Task task, OperationResult parentResult) {

        LOGGER.info("Launching reconciliation for resource {} as asynchronous task", ObjectTypeUtil.toShortString(resource));

        OperationResult result = parentResult.createSubresult(ReconciliationTaskHandler.class.getName() + ".launch");
        result.addParam("resource", resource);
        result.addParam("objectclass", objectclass);
        // TODO

        // Set handler URI so we will be called back
        task.setHandlerUri(ModelPublicConstants.RECONCILIATION_TASK_HANDLER_URI);

        // Readable task name
        PolyStringType polyString = new PolyStringType("Reconciling " + resource.getName());
        task.setName(polyString);

        // Set reference to the resource
        task.setObjectRef(ObjectTypeUtil.createObjectRef(resource, prismContext));

        try {
            task.setExtensionPropertyValue(ModelConstants.OBJECTCLASS_PROPERTY_NAME, objectclass);
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

        task.addArchetypeInformationIfMissing(SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value());

        // Switch task to background. This will start new thread and call
        // the run(task) method.
        // Note: the thread may be actually started on a different node
        taskManager.switchToBackground(task, result);
        result.setBackgroundTaskOid(task.getOid());
        result.computeStatus("Reconciliation launch failed");

        LOGGER.trace("Reconciliation for resource {} switched to background, control thread returning with task {}", ObjectTypeUtil.toShortString(resource), task);
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.RECONCILIATION;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value();
    }

    @Override
    public String getDefaultChannel() {
        return Channel.RECONCILIATION.getUri();
    }

    public ResourceObjectChangeListener getObjectChangeListener() {
        return eventDispatcher;
    }
}
