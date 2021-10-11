/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.impl.importer.ImportAccountsFromResourceTaskHandler;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.repo.common.util.RepoCommonUtils;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.SynchronizationInformation;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

/**
 * Iterative search result handler for account synchronization. Works both for
 * reconciliation and import from resource.
 *
 * This class is called back from the searchObjectsIterative() operation of the
 * provisioning service. It does most of the work of the "import" and resource
 * reconciliation operations.
 *
 * @see ImportAccountsFromResourceTaskHandler
 * @see ReconciliationTaskHandler
 *
 * @author Radovan Semancik
 *
 */
public class SynchronizeAccountResultHandler extends AbstractSearchIterativeResultHandler<ShadowType> {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizeAccountResultHandler.class);

    private ResourceObjectChangeListener objectChangeListener;
    private String resourceOid;
    // TODO Is PCV really not thread safe for reading?! Even now, after Xerces-related issues were eliminated?
    private ThreadLocal<ResourceType> resourceWorkingCopy = new ThreadLocal<>();       // because PrismContainer is not thread safe even for reading, each thread must have its own copy
    private ResourceType resourceReadOnly;                // this is a "master copy", not to be touched by getters - its content is copied into resourceWorkingCopy content when needed
    private ObjectClassComplexTypeDefinition objectClassDef;
    private QName sourceChannel;
    private boolean forceAdd;
    private boolean intentIsNull;

    public SynchronizeAccountResultHandler(ResourceType resource, ObjectClassComplexTypeDefinition objectClassDef,
            String processShortName, RunningTask coordinatorTask, ResourceObjectChangeListener objectChangeListener,
            TaskPartitionDefinitionType stageType, TaskManager taskManager) {
        super(coordinatorTask, SynchronizeAccountResultHandler.class.getName(), processShortName, "from "+resource, stageType, taskManager);
        this.objectChangeListener = objectChangeListener;
        this.resourceReadOnly = resource;
        this.resourceOid = resource.getOid();
        this.objectClassDef = objectClassDef;
        forceAdd = false;
        setRecordIterationStatistics(false);        // we do statistics ourselves in handler, because in case of reconciliation
                                                    // we are not called via AbstractSearchIterativeResultHandler.processRequest
    }

    void setIntentIsNull(boolean intentIsNull) {
        this.intentIsNull = intentIsNull;
    }

    public void setForceAdd(boolean forceAdd) {
        this.forceAdd = forceAdd;
    }

    public void setSourceChannel(QName sourceChannel) {
        this.sourceChannel = sourceChannel;
    }

    public String getResourceOid() {
        return resourceOid;
    }

    private ResourceType getResourceWorkingCopy() {
        ResourceType existingWorkingCopy = resourceWorkingCopy.get();
        if (existingWorkingCopy != null) {
            return existingWorkingCopy;
        } else {
            ResourceType newWorkingCopy = resourceReadOnly.clone();
            resourceWorkingCopy.set(newWorkingCopy);
            return newWorkingCopy;
        }
    }

    public ObjectClassComplexTypeDefinition getObjectClass() {
        return objectClassDef;
    }

    /**
     * This methods will be called for each search result. It means it will be
     * called for each account on a resource. We will pretend that the account
     * was created and invoke notification interface.
     */
    @Override
    protected boolean handleObject(PrismObject<ShadowType> shadowObject, RunningTask workerTask, OperationResult result) {
        long started = System.currentTimeMillis();
        ShadowType shadow = shadowObject.asObjectable();
        try {
            workerTask.recordIterativeOperationStart(shadow);
            boolean rv = handleObjectInternal(shadowObject, started, workerTask, result);
            result.computeStatusIfUnknown();
            if (result.isError()) {
                workerTask.recordIterativeOperationEnd(shadow, started, RepoCommonUtils.getResultException(result));
            } else {
                workerTask.recordIterativeOperationEnd(shadow, started, null);
            }
            return rv;
        } catch (Throwable t) {
            workerTask.recordIterativeOperationEnd(shadow, started, t);
            throw t;
        }
    }

    private boolean handleObjectInternal(PrismObject<ShadowType> shadowObject, long started, RunningTask workerTask, OperationResult result) {

        LOGGER.trace("{} considering object:\n{}", getProcessShortNameCapitalized(), shadowObject.debugDumpLazily(1));

        ShadowType shadow = shadowObject.asObjectable();
        if (shadow.isProtectedObject() != null && shadow.isProtectedObject()) {
            LOGGER.trace("{} skipping {} because it is protected", getProcessShortNameCapitalized(), shadowObject);
            SynchronizationInformation.Record record = SynchronizationInformation.Record.createProtected();
            workerTask.recordSynchronizationOperationEnd(shadow, started, null, record, record);
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Skipped because it is protected");
            return workerTask.canRun();
        }

        boolean isMatches;
        if (intentIsNull && objectClassDef instanceof RefinedObjectClassDefinition) {
            isMatches = ((RefinedObjectClassDefinition)objectClassDef).matchesWithoutIntent(shadow);
        } else {
            isMatches = objectClassDef.matches(shadow);
        }

        if (objectClassDef != null && !isShadowUnknown(shadow) && !isMatches) {
            LOGGER.trace("{} skipping {} because it does not match objectClass/kind/intent specified in {}",
                    getProcessShortNameCapitalized(), shadowObject, objectClassDef);
            SynchronizationInformation.Record record = SynchronizationInformation.Record.createNotApplicable();
            workerTask.recordSynchronizationOperationEnd(shadow, started, null, record, record);
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Skipped because it does not match objectClass/kind/intent");
            return workerTask.canRun();
        }

        if (objectChangeListener == null) {
            LOGGER.warn("No object change listener set for {} task, ending the task", getProcessShortName());
            result.recordFatalError("No object change listener set for " + getProcessShortName() + " task, ending the task");
            return false;
        }

        // We are going to pretend that all of the objects were just created.
        // That will effectively import them to the IDM repository

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setSourceChannel(QNameUtil.qNameToUri(sourceChannel));
        change.setResource(getResourceWorkingCopy().asPrismObject());
        if (getStageType() != null && ExecutionModeType.SIMULATE == getStageType().getStage()) {
            change.setSimulate(true);
        }

        if (forceAdd) {
            // We should provide shadow in the state before the change. But we are
            // pretending that it has not existed before, so we will not provide it.
            ObjectDelta<ShadowType> shadowDelta = shadowObject.getPrismContext().deltaFactory().object()
                    .create(ShadowType.class, ChangeType.ADD);
            shadowDelta.setObjectToAdd(shadowObject);
            shadowDelta.setOid(shadowObject.getOid());
            change.setObjectDelta(shadowDelta);
            // Need to also set current shadow. This will get reflected in "old" object in lens context
        } else {
            // No change, therefore the delta stays null. But we will set the current
        }
        change.setCurrentShadow(shadowObject);

        try {
            change.checkConsistence();
        } catch (RuntimeException ex) {
            LOGGER.trace("Check consistence failed: {}\nChange:\n{}", ex, change.debugDumpLazily());
            throw ex;
        }

        // Invoke the change notification
        ModelImplUtils.clearRequestee(workerTask);
        objectChangeListener.notifyChange(change, workerTask, result);

        LOGGER.debug("#### notify change finished");

        // No exception thrown here. The error is indicated in the result. Will be processed by superclass.
        return workerTask.canRun();
    }

    private boolean isShadowUnknown(ShadowType shadowType) {
        return ShadowKindType.UNKNOWN == shadowType.getKind()
                || SchemaConstants.INTENT_UNKNOWN.equals(shadowType.getIntent());
    }
}
