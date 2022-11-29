/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.action;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.MiscUtil;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.sync.reactions.ActionInstantiationContext;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * A synchronization action that involves clockwork processing.
 *
 * Its concrete children have to implement {@link #prepareContext(LensContext, OperationResult)} method.
 * (Before 4.6 it was called `handle`. Now the {@link SynchronizationAction#handle(OperationResult)} is more
 * generic, as it's not bound to the clockwork execution.)
 */
abstract class BaseClockworkAction<F extends FocusType> extends BaseAction<F> {

    private static final String OP_HANDLE = BaseClockworkAction.class.getName() + ".handle";

    BaseClockworkAction(@NotNull ActionInstantiationContext<F> ctx) {
        super(ctx);
    }

    @Override
    public void handle(@NotNull OperationResult parentResult) throws CommonException {

        OperationResult result = parentResult.subresult(OP_HANDLE).build();
        try {
            LensContext<F> lensContext = createLensContext();
            lensContext.setDoReconciliationForAllProjections(BooleanUtils.isTrue(actionDefinition.isReconcileAll()));
            LOGGER.trace("---[ SYNCHRONIZATION context before action execution ]-------------------------\n"
                    + "{}\n------------------------------------------", lensContext.debugDumpLazily());

            prepareContext(lensContext, result);

            beans.medic.enterModelMethod(false);
            try {
                Task task = syncCtx.getTask();
                // Temporary code - we'll have a single entry point in the future
                if (task.isPersistentExecution()) {
                    beans.clockwork.run(lensContext, task, result);
                } else {
                    beans.clockwork.previewChanges(lensContext, null, task, result);
                }
            } finally {
                beans.medic.exitModelMethod(false);
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @NotNull
    private LensContext<F> createLensContext() throws SchemaException, ConfigurationException {

        ModelExecuteOptions options = createOptions();

        LensContext<F> context = beans.contextFactory.createSyncContext(syncCtx.getFocusClass(), change, syncCtx.getTask());
        context.setLazyAuditRequest(true);
        context.setSystemConfiguration(ObjectTypeUtil.asPrismObject(syncCtx.getSystemConfiguration()));
        context.setOptions(options);
        context.setItemProcessingIdentifier(syncCtx.getItemProcessingIdentifier());

        ResourceType resource = change.getResource().asObjectable();
        if (ModelExecuteOptions.isLimitPropagation(options)) {
            context.setTriggeringResourceOid(resource);
        }

        context.rememberResource(resource);

        createProjectionContext(options, context);
        createFocusContextIfKnown(context);

        ObjectReferenceType objectTemplateRef = actionDefinition.getObjectTemplateRef();
        if (objectTemplateRef != null) {
            context.setExplicitFocusTemplateOid(
                    MiscUtil.configNonNull(
                            objectTemplateRef.getOid(),
                            () -> "Dynamic (non-OID) object template references are not supported in synchronization actions"));
        }

        return context;
    }

    private @NotNull ModelExecuteOptions createOptions() {

        ModelExecuteOptionsType explicitOptions = actionDefinition.getExecuteOptions();
        ModelExecuteOptions options = explicitOptions != null ?
                ModelExecuteOptions.fromModelExecutionOptionsType(explicitOptions) :
                ModelExecuteOptions.create();

        if (options.getReconcile() == null) {
            Boolean isReconcile = actionDefinition.isReconcile();
            if (isReconcile != null) {
                options = options.reconcile(isReconcile);
            } else {
                // We have to do reconciliation if we have got a full shadow and no delta.
                // There is no other good way how to reflect the changes from the shadow.
                if (change.getObjectDelta() == null) {
                    options = options.reconcile();
                }
            }
        }

        if (options.getLimitPropagation() == null) {
            options = options.limitPropagation(isLimitPropagation());
        }

        return options;
    }

    private Boolean isLimitPropagation() {
        String channel = syncCtx.getChannel();
        SynchronizationSituationType situation = syncCtx.getSituation();
        if (StringUtils.isNotBlank(channel)) {
            QName channelQName = QNameUtil.uriToQName(channel);
            // Discovery channel is used when compensating some inconsistent
            // state. Therefore we do not want to propagate changes to other
            // resources. We only want to resolve the problem and continue in
            // previous provisioning/synchronization during which this
            // compensation was triggered.
            if (SchemaConstants.CHANNEL_DISCOVERY.equals(channelQName)
                    && situation != SynchronizationSituationType.DELETED) {
                return true;
            }
        }

        return actionDefinition.isLimitPropagation();
    }

    private void createProjectionContext(ModelExecuteOptions options, LensContext<F> context) {
        ResourceType resource = change.getResource().asObjectable();
        ShadowType shadow = syncCtx.getShadowedResourceObject();
        ResourceObjectTypeIdentification typeIdentification = syncCtx.getTypeIdentification();
        boolean tombstone = isTombstone(change);
        LensProjectionContext projectionContext =
                context.createProjectionContext(
                        ProjectionContextKey.forKnownResource(
                                resource.getOid(), typeIdentification, shadow.getTag(), 0, tombstone));
        projectionContext.setResource(resource);
        projectionContext.setOid(change.getShadowOid());
        projectionContext.setSynchronizationSituationDetected(syncCtx.getSituation());
        projectionContext.setShadowExistsInRepo(syncCtx.isShadowExistsInRepo());
        projectionContext.setSynchronizationSource(true);

        // insert object delta if available in change
        ObjectDelta<ShadowType> delta = change.getObjectDelta();
        if (delta != null) {
            projectionContext.setSyncDelta(delta);
        } else {
            projectionContext.setSyncAbsoluteTrigger(true);
        }

        // This will set both old and current object: and that's how it should be.
        projectionContext.setInitialObject(shadow.asPrismObject());

        if (!tombstone && !containsIncompleteItems(shadow)) {
            projectionContext.setFullShadow(true);
        }
        projectionContext.setFresh(true);
        projectionContext.setExists(!change.isDelete()); // TODO is this correct?
        projectionContext.setDoReconciliation(ModelExecuteOptions.isReconcile(options));
    }

    private void createFocusContextIfKnown(LensContext<F> context) {
        if (syncCtx.getLinkedOwner() != null) {
            F owner = syncCtx.getLinkedOwner();
            LensFocusContext<F> focusContext = context.createFocusContext();
            //noinspection unchecked
            focusContext.setInitialObject((PrismObject<F>) owner.asPrismObject());
        }
    }

    private boolean containsIncompleteItems(ShadowType shadow) {
        ShadowAttributesType attributes = shadow.getAttributes();
        //noinspection SimplifiableIfStatement
        if (attributes == null) {
            return false; // strictly speaking this is right; but we perhaps should not consider this shadow as fully loaded :)
        } else {
            return ((PrismContainerValue<?>) (attributes.asPrismContainerValue())).getItems().stream()
                    .anyMatch(Item::isIncomplete);
        }
    }

    // TODO is this OK? What if the dead flag is obsolete?
    private boolean isTombstone(ResourceObjectShadowChangeDescription change) {
        PrismObject<? extends ShadowType> shadow = change.getShadowedResourceObject();
        if (shadow.asObjectable().isDead() != null) {
            return shadow.asObjectable().isDead();
        } else {
            return change.isDelete();
        }
    }

    abstract void prepareContext(
            @NotNull LensContext<F> context,
            @NotNull OperationResult result) throws SchemaException;
}
