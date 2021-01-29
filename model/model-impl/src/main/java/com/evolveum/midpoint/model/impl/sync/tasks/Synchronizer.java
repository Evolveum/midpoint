/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.tasks;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.statistics.SynchronizationInformation;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.common.util.RepoCommonUtils;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Synchronizes a single resource object. Works both for reconciliation and import from resource
 * (iterative + single shadow).
 *
 * @author Radovan Semancik
 */
public class Synchronizer {

    private static final Trace LOGGER = TraceManager.getTrace(Synchronizer.class);

    @NotNull private final ResourceType resource;
    @NotNull private final ObjectClassComplexTypeDefinition objectClassDef;
    @NotNull private final SynchronizationObjectsFilter objectsFilter;
    @NotNull private final ResourceObjectChangeListener objectChangeListener;
    @NotNull private final String taskTypeName;
    @NotNull private final QName sourceChannel;
    private final TaskPartitionDefinitionType partDefinition;
    private final boolean forceAdd;

    public Synchronizer(@NotNull ResourceType resource,
            @NotNull ObjectClassComplexTypeDefinition objectClassDef,
            @NotNull SynchronizationObjectsFilter objectsFilter,
            @NotNull ResourceObjectChangeListener objectChangeListener,
            @NotNull String taskTypeName,
            @NotNull QName sourceChannel,
            TaskPartitionDefinitionType partDefinition,
            boolean forceAdd) {
        this.resource = resource;
        this.objectClassDef = objectClassDef;
        this.objectsFilter = objectsFilter;
        this.objectChangeListener = objectChangeListener;
        this.taskTypeName = taskTypeName;
        this.sourceChannel = sourceChannel;
        this.partDefinition = partDefinition;
        this.forceAdd = forceAdd;
    }

    public ObjectClassComplexTypeDefinition getObjectClass() {
        return objectClassDef;
    }

    /**
     * This methods will be called for each search result. It means it will be
     * called for each account on a resource. We will pretend that the account
     * was created and invoke notification interface.
     */
    public void handleObject(PrismObject<ShadowType> shadowObject, Task workerTask, OperationResult result) {
        long started = System.currentTimeMillis(); // TODO temporary
        ShadowType shadow = shadowObject.asObjectable();
        if (ObjectTypeUtil.hasFetchError(shadowObject)) {
            // Not used in iterative tasks. There we filter out these objects before processing.
            OperationResultType fetchResult = shadow.getFetchResult();
            // The following exception will be artificial, without stack trace because
            // of operation result conversions (native -> bean -> native).
            Throwable fetchResultException = RepoCommonUtils.getResultException(fetchResult);
            throw new SystemException("Skipped malformed resource object: " + fetchResultException.getMessage(), fetchResultException);
        }
        if (Boolean.TRUE.equals(shadow.isProtectedObject())) {
            LOGGER.trace("Skipping {} because it is protected", shadowObject);
            SynchronizationInformation.Record record = SynchronizationInformation.Record.createProtected(); // TODO temporary
            workerTask.recordSynchronizationOperationEnd(shadow, started, null, record, record); // TODO temporary
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Skipped because it is protected");
            return;
        }
        if (!isShadowUnknown(shadow) && !objectsFilter.matches(shadowObject)) {
            LOGGER.trace("Skipping {} because it does not match objectClass/kind/intent", shadowObject);
            SynchronizationInformation.Record record = SynchronizationInformation.Record.createNotApplicable(); // TODO temporary
            workerTask.recordSynchronizationOperationEnd(shadow, started, null, record, record); // TODO temporary
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Skipped because it does not match objectClass/kind/intent");
            return;
        }

        handleObjectInternal(shadowObject, workerTask, result);
    }

    private void handleObjectInternal(PrismObject<ShadowType> shadowObject, Task workerTask, OperationResult result) {
        // We are going to pretend that all of the objects were just created.
        // That will effectively import them to the repository

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setSourceChannel(QNameUtil.qNameToUri(sourceChannel));
        change.setResource(resource.asPrismObject());
        change.setSimulate(isSimulate());

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
    }

    private boolean isSimulate() {
        return partDefinition != null && partDefinition.getStage() == ExecutionModeType.SIMULATE;
    }

    private boolean isShadowUnknown(ShadowType shadowType) {
        return ShadowKindType.UNKNOWN == shadowType.getKind()
                || SchemaConstants.INTENT_UNKNOWN.equals(shadowType.getIntent());
    }
}
