/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.tasks.imp;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.sync.tasks.ProcessingScope;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.sync.tasks.NullPostSearchFilterImpl;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncTaskHelper;
import com.evolveum.midpoint.model.impl.sync.tasks.Synchronizer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Responsible for launching "import from resource" activity task,
 * as well as for importing a single account on foreground.
 */
@Component
public class ImportFromResourceLauncher {

    private static final String OP_IMPORT_SINGLE_SHADOW = ImportFromResourceLauncher.class.getName() + ".importSingleShadow";

    private static final Trace LOGGER = TraceManager.getTrace(ImportFromResourceLauncher.class);

    @Autowired private ProvisioningService provisioningService;
    @Autowired private SyncTaskHelper syncTaskHelper;
    @Autowired private EventDispatcher eventDispatcher;
    @Autowired private PrismContext prismContext;
    @Autowired private TaskManager taskManager;

    public boolean importSingleShadow(String shadowOid, Task task, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(OP_IMPORT_SINGLE_SHADOW);
        try {
            ShadowType shadow = provisioningService
                    .getObject(ShadowType.class, shadowOid, readOnly(), task, result)
                    .asObjectable();
            ProcessingScope spec = syncTaskHelper.createProcessingScopeForShadow(shadow, task, result);
            Synchronizer synchronizer = new Synchronizer(
                    spec.getResource(),
                    new NullPostSearchFilterImpl(),
                    eventDispatcher,
                    SchemaConstants.CHANNEL_IMPORT,
                    true);
            synchronizer.synchronize(shadow.asPrismObject(), null, task, result);
            result.computeStatusIfUnknown();
            return !result.isError();
        } catch (ActivityRunException t) {
            result.recordStatus(t.getOpResultStatus(), t.getMessage(), t);
            throw new SystemException(t); // FIXME unwrap the exception
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown(); // just for sure
        }
    }

    /**
     * Launch an import. Calling this method will start import in a new
     * thread, possibly on a different node.
     */
    public void launch(ResourceType resource, QName objectclass, Task task, OperationResult parentResult) {

        LOGGER.info("Launching import from resource {} as asynchronous task", ObjectTypeUtil.toShortString(resource));

        OperationResult result = parentResult.createSubresult(ImportFromResourceLauncher.class.getName() + ".launch");
        result.addParam("resource", resource);
        result.addParam("objectclass", objectclass);

        task.setName(new PolyStringType("Import from resource " + resource.getName()));

        ObjectReferenceType resourceRef = ObjectTypeUtil.createObjectRef(resource);

        // Not strictly necessary but nice to do (activity would fill-in these when started)
        task.setObjectRef(resourceRef.clone());
        task.addArchetypeInformationIfMissing(SystemObjectsType.ARCHETYPE_IMPORT_TASK.value());

        try {
            // @formatter:off
            task.setRootActivityDefinition(
                    new ActivityDefinitionType()
                            .beginWork()
                                .beginImport()
                                    .beginResourceObjects()
                                        .resourceRef(resourceRef.clone())
                                        .objectclass(objectclass)
                                    .<ImportWorkDefinitionType>end()
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

        // Switch task to background. This will start new thread and call
        // the run(task) method.
        // Note: the thread may be actually started on a different node
        taskManager.switchToBackground(task, result);
        result.setBackgroundTaskOid(task.getOid());
        result.computeStatus("Import launch failed");

        LOGGER.trace("Import from resource {} switched to background, control thread returning with task {}",
                ObjectTypeUtil.toShortString(resource), task);
    }
}
