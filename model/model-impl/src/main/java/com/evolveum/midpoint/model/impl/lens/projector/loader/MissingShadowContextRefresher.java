/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.loader;

import java.util.Collection;
import java.util.Objects;

import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.impl.lens.FocusGoneException;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;

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

import static com.evolveum.midpoint.schema.GetOperationOptions.*;

/**
 * Refreshes a projection context after the shadow was found to be missing.
 *
 * Basically, if there is a chance that the discovery was run, we re-load the focus and try to find
 * matching linkRef: the one with compatible coordinates and without projection context.
 *
 * Side-effect: unlinks shadows (by scheduling delete linkRef deltas) that are believed to be missing.
 * See uses of {@link #swallowUnlinkDelta(ObjectReferenceType)}.
 */
public class MissingShadowContextRefresher<F extends ObjectType> {

    private static final Trace LOGGER = TraceManager.getTrace(MissingShadowContextRefresher.class);

    @NotNull private final LensContext<F> context;
    @NotNull private final LensProjectionContext deadProjectionContext;
    @Nullable private final Collection<SelectorOptions<GetOperationOptions>> options;
    @NotNull private final Task task;
    @NotNull private final ModelBeans beans = ModelBeans.get();

    MissingShadowContextRefresher(
            @NotNull LensContext<F> context,
            @NotNull LensProjectionContext deadProjectionContext,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task) {
        this.context = context;
        this.deadProjectionContext = deadProjectionContext;
        this.options = options;
        this.task = task;
    }

    public void refresh(OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {

        if (deadProjectionContext.isDelete()) {
            // This is OK: shadow was deleted, but we will continue in processing with the old shadow.
            // And we set it as full to prevent from other full loading.
            deadProjectionContext.setFullShadow(true);
            return;
        }

        LOGGER.trace("Trying to compensate for ObjectNotFoundException or resource object not found; dead projection context: {}",
                deadProjectionContext);
        boolean compensated;
        if (!deadProjectionContext.isClassified()) {
            LOGGER.trace(" -> The dead context was unclassified: there is no hope of matching any (re-created) shadow found"
                    + " with it. So we won't even try.");
            compensated = false;
        } else if (!GetOperationOptions.isDoNotDiscovery(options)) {
            // The account might have been re-created by the discovery.
            // Reload focus, try to find out if there is a new matching link (and the old is gone)
            LOGGER.trace(" -> reloading the focus with the goal of finding matching link");
            compensated = reloadFocusAndFindMatchingLink(result);
        } else {
            LOGGER.trace(" -> discovery is disabled, no point in reloading the focus");
            compensated = false;
        }

        if (compensated) {
            LOGGER.trace("'No resource object' error is compensated, continuing");
        } else {
            if (deadProjectionContext.isReaping()) {
                LOGGER.trace("'No resource object' error is not compensated, but the shadow is in reaping state -"
                        + " NOT setting context as gone");
            } else {
                LOGGER.trace("'No resource object' error is not compensated, setting context as gone");
                deadProjectionContext.markGone();
            }
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

        LOGGER.trace("Trying to find a (new) linkRef that has no projection context and is compatible with the dead one");
        for (ObjectReferenceType linkRef : reloadedFocus.getLinkRef()) {
            LOGGER.trace("Considering {}", linkRef);

            if (linkRef.getOid().equals(deadProjectionContext.getOid())) {
                LOGGER.trace("-> the link points to the same shadow as the dead projection context does (i.e. not good for us)");
                processDanglingLinkRef(linkRef, reloadedFocusObject);
                continue;
            }
            boolean hasProjectionContext = context.getProjectionContexts().stream()
                    .anyMatch(pCtx -> linkRef.getOid().equals(pCtx.getOid()));
            if (hasProjectionContext) {
                LOGGER.trace("-> This linkRef corresponds to an existing projection context. Continue checking other links.");
                continue;
            }

            // This linkRef is new, it is not in the existing lens context. Is it matching? If yes, here we get the shadow.
            LOGGER.trace("-> A light of hope? This linkRef has no OID-matching projection context. Let's check "
                    + "if it is usable (compatible with the dead context)");
            ShadowType shadow = getShadowIfCompatible(linkRef, reloadedFocus, result);
            if (shadow != null) {
                LOGGER.trace("Found new matching link: {}, updating projection context", shadow);
                LOGGER.trace("Applying definition from provisioning first."); // MID-3317
                beans.provisioningService.applyDefinition(shadow.asPrismObject(), task, result);
                deadProjectionContext.setCurrentObjectAndOid(shadow.asPrismObject());
                // The "exists" information in the projection context can be obsolete - reflecting the fact that
                // resource object couldn't be found.
                deadProjectionContext.setExists(ShadowUtil.isExists(shadow));
                return true;
            } else {
                LOGGER.trace("-> Unfortunately, this shadow is not usable; continuing through the linkRefs.");
            }
        }

        LOGGER.trace("No suitable link found; reloadFocusAndFindMatchingLink finishes with no match.");
        return false;
    }

    /** We have a linkRef with OID not matching any existing projection context. Is it compatible with the dead one? */
    private @Nullable ShadowType getShadowIfCompatible(
            ObjectReferenceType linkRef, FocusType reloadedFocus, OperationResult result)
            throws SchemaException, ConfigurationException {
        ShadowType shadow;
        try {
            // Consider read-only option later (but now the object is inserted into projection context, so using R/W mode).
            shadow = beans.provisioningService
                    .getShadow(linkRef.getOid(), createNoFetchCollection(), task, result)
                    .getBean();
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't resolve {}, unlinking it from the focus {}",
                    e, linkRef.getOid(), reloadedFocus);
            swallowUnlinkDelta(linkRef);
            return null;
        } catch (ExpressionEvaluationException | CommunicationException | SecurityViolationException e) {
            throw SystemException.unexpected(e, "while getting shadow with OID " + linkRef.getOid() + " (no fetch mode)");
        }

        if (shadowMatchesDeadContext(shadow)) {
            LOGGER.trace("Found new link and it's suitable: {}", shadow);
            return shadow;
        } else {
            LOGGER.trace("Found new link: {}, but skipping it because it does not match the projection context", shadow);
            return null;
        }
    }

    private boolean shadowMatchesDeadContext(@NotNull ShadowType shadow) {
        assert deadProjectionContext.isClassified();
        ProjectionContextKey deadCtxKey = deadProjectionContext.getKey();
        String shadowResourceOid = ShadowUtil.getResourceOid(shadow);
        ResourceObjectTypeIdentification shadowTypeId = ShadowUtil.getTypeIdentification(shadow);
        String shadowTag = shadow.getTag();
        boolean matches = Objects.equals(deadCtxKey.getResourceOid(), shadowResourceOid)
                && Objects.equals(deadCtxKey.getTypeIdentification(), shadowTypeId)
                && Objects.equals(deadCtxKey.getTag(), shadowTag);
        LOGGER.trace("Checked if candidate shadow matches the dead projection context; shadow params = {}/{}/{}; result = {}",
                shadowResourceOid, shadowTypeId, shadowTag, matches);
        return matches;
    }

    private void processDanglingLinkRef(ObjectReferenceType linkRef, PrismObject<F> reloadedFocusObject)
            throws SchemaException {
        if (deadProjectionContext.isReaping()) {
            LOGGER.trace("  -> Not deleting linkRef for {} because the shadow is being reaped", deadProjectionContext.getOid());
        } else {
            // The deleted shadow is still in the linkRef. This should not happen, but it obviously happens sometimes.
            // Maybe some strange race condition? Anyway, we want a robust behavior and this linkRef should NOT be there.
            // So simply remove it.
            LOGGER.warn("  -> The OID {} of deleted shadow still exists in the linkRef after discovery ({}), removing it",
                    deadProjectionContext.getOid(), reloadedFocusObject);
            swallowUnlinkDelta(linkRef);
        }
    }

    private void swallowUnlinkDelta(ObjectReferenceType linkRef) throws SchemaException {
        LensFocusContext<F> focusContext = context.getFocusContextRequired();
        ReferenceDelta unlinkDelta = beans.prismContext.deltaFactory().reference().createModificationDelete(
                FocusType.F_LINK_REF, focusContext.getObjectDefinition(), linkRef.asReferenceValue().clone());
        focusContext.swallowToSecondaryDelta(unlinkDelta); // TODO will it work even with not-updated focus current?
    }

    private PrismObject<F> reloadFocus(OperationResult result)
            throws SchemaException {
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
            return beans.cacheRepositoryService.getObject(focusClass, focusContext.getOid(), readOnly(), result);
        } catch (ObjectNotFoundException e) {
            if (focusContext.isDelete()) {
                // This may be OK. This may be later wave and the focus may be already deleted.
                // Therefore let's just keep what we have. We have no way how to refresh context
                // in that situation.
                result.muteLastSubresultError();
                LOGGER.trace(" -> focus does not exist any more");
                return null;
            } else {
                throw new FocusGoneException();
            }
        }
    }
}
