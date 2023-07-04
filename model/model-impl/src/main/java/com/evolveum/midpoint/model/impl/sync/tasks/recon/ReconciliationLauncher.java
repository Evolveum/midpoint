/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncTaskHelper;
import com.evolveum.midpoint.repo.common.AuditHelper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Launches reconciliation activity tasks.
 */
@Component
public class ReconciliationLauncher {

    @Autowired PrismContext prismContext;
    @Autowired TaskManager taskManager;
    @Autowired EventDispatcher eventDispatcher;
    @Autowired AuditHelper auditHelper;
    @Autowired protected ExpressionFactory expressionFactory;
    @Autowired SchemaService schemaService;
    @Autowired SyncTaskHelper syncTaskHelper;

    @Autowired CacheConfigurationManager cacheConfigurationManager;

    private static final Trace LOGGER = TraceManager.getTrace(ReconciliationLauncher.class);

    /**
     * Launch an import. Calling this method will start import in a new
     * thread, possibly on a different node.
     */
    public void launch(ResourceType resource, QName objectclass, Task task, OperationResult parentResult) {

        LOGGER.info("Launching reconciliation for resource {} as asynchronous task", ObjectTypeUtil.toShortString(resource));

        OperationResult result = parentResult.createSubresult(ReconciliationLauncher.class.getName() + ".launch");
        result.addParam("resource", resource);
        result.addParam("objectclass", objectclass);

        PolyStringType polyString = new PolyStringType("Reconciling " + resource.getName());
        task.setName(polyString);

        ObjectReferenceType resourceRef = ObjectTypeUtil.createObjectRef(resource, prismContext);

        // Not strictly necessary but nice to do (activity would fill-in these when started)
        task.setObjectRef(resourceRef.clone());
        task.addArchetypeInformationIfMissing(SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value());

        try {
            // @formatter:off
            task.setRootActivityDefinition(
                    new ActivityDefinitionType()
                            .beginWork()
                                .beginReconciliation()
                                    .beginResourceObjects()
                                        .resourceRef(resourceRef.clone())
                                        .objectclass(objectclass)
                                    .<ReconciliationWorkDefinitionType>end()
                                .<WorkDefinitionsType>end()
                            .end());
            // @formatter:on
            task.flushPendingModifications(result); // just to be sure (if the task was already persistent)
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

        LOGGER.trace("Reconciliation for resource {} switched to background, control thread returning with task {}",
                ObjectTypeUtil.toShortString(resource), task);
    }
}
