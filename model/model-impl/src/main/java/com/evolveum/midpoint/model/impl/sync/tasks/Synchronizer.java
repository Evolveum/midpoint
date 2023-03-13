/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.tasks;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.common.util.RepoCommonUtils;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
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
    @NotNull private final PostSearchFilter postSearchFilter;
    @NotNull private final ResourceObjectChangeListener objectChangeListener;
    @NotNull private final QName sourceChannel;
    private final boolean forceAdd;

    public Synchronizer(@NotNull ResourceType resource,
            @NotNull PostSearchFilter postSearchFilter,
            @NotNull ResourceObjectChangeListener objectChangeListener,
            @NotNull QName sourceChannel,
            boolean forceAdd) {
        this.resource = resource;
        this.postSearchFilter = postSearchFilter;
        this.objectChangeListener = objectChangeListener;
        this.sourceChannel = sourceChannel;
        this.forceAdd = forceAdd;
    }

    /**
     * This methods will be called for each search result. It means it will be
     * called for each account on a resource. We will pretend that the account
     * was created and invoke notification interface.
     */
    public void synchronize(
            PrismObject<ShadowType> shadowObject,
            String itemProcessingIdentifier,
            Task workerTask,
            OperationResult result) {
        ShadowType shadow = shadowObject.asObjectable();
        if (ObjectTypeUtil.hasFetchError(shadowObject)) {
            // Not used in iterative tasks. There we filter out these objects before processing.
            OperationResultType fetchResult = shadow.getFetchResult();
            // The following exception will be artificial, without stack trace because
            // of operation result conversions (native -> bean -> native).
            Throwable fetchResultException = RepoCommonUtils.getResultException(fetchResult);
            throw new SystemException("Skipped malformed resource object: " + fetchResultException.getMessage(), fetchResultException);
        }
        // TODO I think that "is shadow unknown" condition can be removed. All shadows should be classified by provisioning.
        //  And, if any one is not, and the filter requires classification, we should not pass it.
        if (!isShadowUnknown(shadow) && !postSearchFilter.matches(shadowObject)) {
            LOGGER.trace("Skipping {} because it does not match objectClass/kind/intent", shadowObject);
            workerTask.onSynchronizationExclusion(itemProcessingIdentifier, SynchronizationExclusionReasonType.NOT_APPLICABLE_FOR_TASK);
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Skipped because it does not match objectClass/kind/intent");
            return;
        }

        handleObjectInternal(shadowObject, itemProcessingIdentifier, workerTask, result);
    }

    private void handleObjectInternal(PrismObject<ShadowType> shadowObject, String itemProcessingIdentifier, Task workerTask,
            OperationResult result) {
        // We are going to pretend that all of the objects were just created.
        // That will effectively import them to the repository

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setSourceChannel(QNameUtil.qNameToUri(sourceChannel));
        change.setResource(resource.asPrismObject());
        change.setItemProcessingIdentifier(itemProcessingIdentifier);

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
        change.setShadowedResourceObject(shadowObject);

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

    private boolean isShadowUnknown(ShadowType shadowType) {
        return ShadowKindType.UNKNOWN == shadowType.getKind()
                || SchemaConstants.INTENT_UNKNOWN.equals(shadowType.getIntent());
    }
}
