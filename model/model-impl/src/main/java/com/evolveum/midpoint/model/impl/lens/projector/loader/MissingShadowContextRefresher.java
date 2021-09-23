/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.loader;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;

/**
 * Refreshes a projection context after the shadow was found to be missing.
 *
 * Basically, if there is a chance that the discovery was run, we re-load the focus and try to find
 * matching linkRef: the one with compatible RSD and without projection context.
 *
 * Side-effect: unlinks shadows (by scheduling delete linkRef deltas) that are believed to be missing.
 * See uses of {@link #swallowUnlinkDelta(LensFocusContext, ObjectReferenceType)}.
 */
public class MissingShadowContextRefresher<F extends ObjectType> {

    private static final Trace LOGGER = TraceManager.getTrace(MissingShadowContextRefresher.class);

    @NotNull private final LensContext<F> context;
    @NotNull private final LensProjectionContext projectionContext;
    @Nullable private final Collection<SelectorOptions<GetOperationOptions>> options;
    @NotNull private final Task task;
    @NotNull private final ModelBeans beans = ModelBeans.get();

    MissingShadowContextRefresher(
            @NotNull LensContext<F> context,
            @NotNull LensProjectionContext projectionContext,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task) {
        this.context = context;
        this.projectionContext = projectionContext;
        this.options = options;
        this.task = task;
    }

    public void refresh(OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {

        if (projectionContext.isDelete()) {
            // This is OK: shadow was deleted, but we will continue in processing with the old shadow.
            // And we set it as full to prevent from other full loading.
            projectionContext.setFullShadow(true);
            return;
        }

        LOGGER.trace("Trying to compensate for ObjectNotFoundException.");
        boolean compensated;
        if (!GetOperationOptions.isDoNotDiscovery(SelectorOptions.findRootOptions(options))) {
            // The account might have been re-created by the discovery.
            // Reload focus, try to find out if there is a new matching link (and the old is gone)
            LOGGER.trace(" -> reloading the focus with the goal of finding matching link");
            compensated = reloadFocusAndFindMatchingLink(result);
        } else {
            LOGGER.trace(" -> discovery is disabled, no point in reloading the focus");
            compensated = false;
        }

        if (compensated) {
            LOGGER.trace("ObjectNotFound error is compensated, continuing");
        } else {
            LOGGER.trace("ObjectNotFound error is not compensated, setting context to tombstone");
            projectionContext.markTombstone();
        }
    }

    private boolean reloadFocusAndFindMatchingLink(OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        PrismObject<F> reloadedFocusObject = reloadFocus(result);
        if (reloadedFocusObject == null) {
            return false; // reason already logged
        }
        FocusType reloadedFocus = (FocusType) reloadedFocusObject.asObjectable();
        // TODO shouldn't we put the reloaded focus back into the focus context? Beware that it was fetched as read-only.

        LensFocusContext<F> focusContext = context.getFocusContextRequired();
        for (ObjectReferenceType linkRef : reloadedFocus.getLinkRef()) {
            if (linkRef.getOid().equals(projectionContext.getOid())) {
                // The deleted shadow is still in the linkRef. This should not happen, but it obviously happens sometimes.
                // Maybe some strange race condition? Anyway, we want a robust behavior and this linkRef should NOT be there.
                // So simple remove it.
                LOGGER.warn("The OID {} of deleted shadow still exists in the linkRef after discovery ({}), removing it",
                        projectionContext.getOid(), reloadedFocusObject);
                swallowUnlinkDelta(focusContext, linkRef);
                continue;
            }
            boolean found = context.getProjectionContexts().stream()
                    .anyMatch(pCtx -> linkRef.getOid().equals(pCtx.getOid()));
            if (found) {
                // This linkRef corresponds to an existing projection context.
                // Continue searching.
                continue;
            }

            // This linkRef is new, it is not in the existing lens context. Is it matching?
            PrismObject<ShadowType> newLinkRepoShadow;
            try {
                // Consider read-only option later (but now the object is inserted into projection context, so using R/W mode).
                newLinkRepoShadow = beans.cacheRepositoryService.getObject(ShadowType.class, linkRef.getOid(), null, result);
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't resolve {}, unlinking it from the focus {}",
                        e, linkRef.getOid(), reloadedFocus);
                swallowUnlinkDelta(focusContext, linkRef);
                continue;
            }

            if (!ShadowUtil.matches(newLinkRepoShadow, projectionContext.getResourceShadowDiscriminator())) {
                LOGGER.trace("Found new link: {}, but skipping it because it does not match the projection context",
                        newLinkRepoShadow);
                continue;
            }

            LOGGER.trace("Found new matching link: {}, updating projection context", newLinkRepoShadow);
            LOGGER.trace("Applying definition from provisioning first."); // MID-3317
            beans.provisioningService.applyDefinition(newLinkRepoShadow, task, result);
            projectionContext.setObjectCurrent(newLinkRepoShadow);
            projectionContext.setOid(newLinkRepoShadow.getOid());
            // The "exists" information in the projection context can be obsolete - reflecting the fact that
            // resource object couldn't be found.
            projectionContext.setExists(ShadowUtil.isExists(newLinkRepoShadow));
            projectionContext.recompute();
            return true;
        }

        LOGGER.trace(" -> no suitable link found");
        return false;
    }

    private void swallowUnlinkDelta(LensFocusContext<F> focusContext, ObjectReferenceType linkRef) throws SchemaException {
        ReferenceDelta unlinkDelta = beans.prismContext.deltaFactory().reference().createModificationDelete(
                FocusType.F_LINK_REF, focusContext.getObjectDefinition(), linkRef.asReferenceValue().clone());
        focusContext.swallowToSecondaryDelta(unlinkDelta); // TODO will it work even with not-updated focus current?
    }

    private PrismObject<F> reloadFocus(OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext == null) {
            LOGGER.trace(" -> no focus context");
            return null;
        }
        Class<F> focusClass = focusContext.getObjectTypeClass();
        if (!FocusType.class.isAssignableFrom(focusClass)) {
            LOGGER.trace(" -> no focus context of FocusType");
            return null;
        }
        try {
            return beans.cacheRepositoryService.getObject(focusClass, focusContext.getOid(), createReadOnlyCollection(), result);
        } catch (ObjectNotFoundException e) {
            if (focusContext.isDelete()) {
                // This may be OK. This may be later wave and the focus may be already deleted.
                // Therefore let's just keep what we have. We have no way how to refresh context
                // in that situation.
                result.muteLastSubresultError();
                LOGGER.trace(" -> focus does not exist any more");
                return null;
            } else {
                throw e;
            }
        }
    }
}
