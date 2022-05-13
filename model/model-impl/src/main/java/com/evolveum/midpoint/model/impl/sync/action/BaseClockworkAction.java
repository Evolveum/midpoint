/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.action;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;

import com.evolveum.midpoint.model.impl.sync.reactions.ActionInstantiationContext;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.midpoint.util.exception.CommonException;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

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
            LensContext<F> lensContext = createLensContext(parentResult);
            lensContext.setDoReconciliationForAllProjections(BooleanUtils.isTrue(actionDefinition.isReconcileAll()));
            LOGGER.trace("---[ SYNCHRONIZATION context before action execution ]-------------------------\n"
                    + "{}\n------------------------------------------", lensContext.debugDumpLazily());

            prepareContext(lensContext, result);

            beans.medic.enterModelMethod(false);
            try {
                Task task = syncCtx.getTask();
                if (change.isSimulate()) {
                    beans.clockwork.previewChanges(lensContext, null, task, result);
                } else {
                    beans.clockwork.run(lensContext, task, result);
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
    private LensContext<F> createLensContext(OperationResult result) throws ObjectNotFoundException, SchemaException {

        ModelExecuteOptions options = createOptions();

        LensContext<F> context = beans.contextFactory.createSyncContext(syncCtx.getFocusClass(), change);
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
        createFocusContext(context);

        setObjectTemplate(context, result);

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
                options.reconcile(isReconcile);
            } else {
                // We have to do reconciliation if we have got a full shadow and no delta.
                // There is no other good way how to reflect the changes from the shadow.
                if (change.getObjectDelta() == null) {
                    options.reconcile();
                }
            }
        }

        if (options.getLimitPropagation() == null) {
            options.limitPropagation(isLimitPropagation());
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

    private void createProjectionContext(ModelExecuteOptions options, LensContext<F> context) throws SchemaException {
        ResourceType resource = change.getResource().asObjectable();
        ShadowType shadow = syncCtx.getShadowedResourceObject();
        ShadowKindType kind = getKind(shadow, syncCtx.getKind());
        String intent = getIntent(shadow, syncCtx.getIntent());
        boolean tombstone = isTombstone(change);
        ResourceShadowDiscriminator discriminator = new ResourceShadowDiscriminator(resource.getOid(), kind, intent, shadow.getTag(), tombstone);
        LensProjectionContext projectionContext = context.createProjectionContext(discriminator);
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

    private void createFocusContext(LensContext<F> context) {
        if (syncCtx.getLinkedOwner() != null) {
            F owner = syncCtx.getLinkedOwner();
            LensFocusContext<F> focusContext = context.createFocusContext();
            //noinspection unchecked
            focusContext.setInitialObject((PrismObject<F>) owner.asPrismObject());
        }
    }

    private void setObjectTemplate(LensContext<F> context, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        ObjectReferenceType objectTemplateRef = actionDefinition.getObjectTemplateRef();
        if (objectTemplateRef != null) {
            ObjectTemplateType objectTemplate = beans.cacheRepositoryService
                    .getObject(ObjectTemplateType.class, objectTemplateRef.getOid(), createReadOnlyCollection(), parentResult)
                    .asObjectable();
            context.setFocusTemplate(objectTemplate);
            context.setFocusTemplateExternallySet(true); // we do not want to override this template e.g. when subtype changes
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

    // TODO What if shadow.kind is `unknown`?
    private ShadowKindType getKind(ShadowType shadow, ShadowKindType objectSynchronizationKind) {
        ShadowKindType shadowKind = shadow.getKind();
        if (shadowKind != null) {
            return shadowKind;
        }
        return objectSynchronizationKind;
    }

    // TODO What if shadow.intent is `unknown`?
    private String getIntent(ShadowType shadow, String objectSynchronizationIntent) {
        String shadowIntent = shadow.getIntent();
        if (shadowIntent != null) {
            return shadowIntent;
        }
        return objectSynchronizationIntent;
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
