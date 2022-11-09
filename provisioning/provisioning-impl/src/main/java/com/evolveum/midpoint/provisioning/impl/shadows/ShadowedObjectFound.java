/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.util.MiscUtil.getClassWithMessage;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType.FETCH_RESULT;

import java.util.Objects;

import com.google.common.base.MoreObjects;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.impl.InitializableMixin;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectFound;
import com.evolveum.midpoint.provisioning.util.InitializationState;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Represents an object found on the resource (using `searchObjects` call) and then "shadowed" by connecting with repo shadow;
 * updating the shadow if necessary.
 */
public class ShadowedObjectFound implements InitializableMixin {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowedObjectFound.class);

    /**
     * The resource object as obtained from the resource object converter. It has no connection to the repo.
     */
    @NotNull private final ShadowType resourceObject;

    /**
     * Real value of the object primary identifier (e.g. ConnId UID).
     * Usually not null (e.g. in ConnId 1.x), but this can change in the future.
     *
     * See {@link ResourceObjectFound#primaryIdentifierValue}.
     */
    private final Object primaryIdentifierValue;

    /**
     * The object after "shadowization". Fulfills the following:
     *
     * In initialized/OK state:
     *
     * 1. has an OID and exists in repository,
     * 2. has classification applied (plus all the other things done when acquiring a shadow),
     * 3. the repo shadow is updated: meaning cached attributes and activation, shadow name, aux object classes,
     * exists flag, caching metadata,
     * 4. contains all information merged from the resource object and the pre-existing shadow,
     *
     * In all other states:
     *
     * All reasonable attempts are done in order to create at least minimalistic shadow (because of error handling).
     * This object points to such a shadow. The other parts (from resource object) can be missing.
     */
    private ShadowType shadowedObject;

    /** State of the processing. */
    private final InitializationState initializationState;

    /** Information used to initialize this object. */
    @NotNull private final InitializationContext ictx;

    ShadowedObjectFound(ResourceObjectFound resourceObjectFound, ShadowsLocalBeans localBeans, ProvisioningContext ctx) {
        this.resourceObject = resourceObjectFound.getResourceObject();
        this.primaryIdentifierValue = resourceObjectFound.getPrimaryIdentifierValue();
        this.initializationState = InitializationState.fromPreviousState(resourceObjectFound.getInitializationState());
        this.ictx = new InitializationContext(localBeans, ctx);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(this.getClass().getSimpleName());
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObject", resourceObject, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "primaryIdentifierValue", String.valueOf(primaryIdentifierValue), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "shadowedObject", shadowedObject, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "initializationState", String.valueOf(initializationState), indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "resourceObject=" + resourceObject +
                ", primaryIdentifierValue=" + primaryIdentifierValue +
                ", shadowedObject=" + shadowedObject +
                ", state=" + initializationState +
                '}';
    }

    /**
     * Acquires repo shadow, updates it, and prepares the shadowedObject.
     * In emergency does a minimalistic processing aimed at acquiring a shadow.
     */
    @Override
    public void initializeInternal(Task task, OperationResult result)
            throws CommonException, EncryptionException {

        if (!initializationState.isInitialStateOk()) {
            // The object is somehow flawed. However, we try to create a shadow.
            //
            // To avoid any harm, we are minimalistic here: If a shadow can be found, it is used "as is". No updates here.
            // If it cannot be found, it is created. We will skip kind/intent/tag determination.
            // Most probably these would not be correct anyway.
            shadowedObject = acquireRepoShadowInEmergency(result);
            return;
        }

        ShadowType repoShadow = acquireRepoShadow(result);
        try {

            // This determines the definitions exactly. Now the repo shadow should have proper kind/intent.
            ProvisioningContext preciseCtx = ictx.localBeans.shadowCaretaker.applyAttributesDefinition(ictx.ctx, repoShadow);
            ictx.localBeans.shadowCaretaker.updateShadowState(preciseCtx, repoShadow);

            ShadowType updatedRepoShadow = updateRepoShadow(preciseCtx, repoShadow, result);
            shadowedObject = createShadowedObject(preciseCtx, updatedRepoShadow, result);

        } catch (Exception e) {

            // No need to log stack trace now. It will be logged at the place where the exception is processed.
            LOGGER.error("Couldn't initialize {}. Continuing with previously acquired repo shadow: {}. Error: {}",
                    resourceObject, repoShadow, getClassWithMessage(e));
            shadowedObject = repoShadow;
            throw e;
        }
    }

    private @NotNull ShadowType acquireRepoShadow(OperationResult result) throws SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, ExpressionEvaluationException, EncryptionException,
            SecurityViolationException {
        // The resource object does not have any kind or intent at this point.
        // But at least locate the definition using object classes.
        ProvisioningContext estimatedCtx = ictx.localBeans.shadowCaretaker.reapplyDefinitions(ictx.ctx, resourceObject);

        // Now find or create repository shadow, along with its classification (maybe it is not a good idea to merge the two).
        try {
            return ictx.localBeans.shadowAcquisitionHelper
                    .acquireRepoShadow(estimatedCtx, resourceObject, false, result);
        } catch (Exception e) {
            // No need to log stack trace now. It will be logged at the place where the exception is processed.
            LOGGER.error("Couldn't acquire shadow for {}. Creating shadow in emergency mode. Error: {}", resourceObject, getClassWithMessage(e));
            shadowedObject = acquireRepoShadowInEmergency(result);
            throw e;
        }
    }

    private ShadowType updateRepoShadow(
            ProvisioningContext ctx, ShadowType repoShadow, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        // TODO: provide shadowState - it is needed when updating exists attribute (because of quantum effects)
        return ictx.localBeans.shadowManager
                .updateShadowInRepository(ctx, resourceObject, null, repoShadow, null, result);
    }

    private @NotNull ShadowType createShadowedObject(ProvisioningContext ctx, ShadowType repoShadow, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException, EncryptionException {
        // TODO do we want also to futurize the shadow like in getObject?
        return ictx.localBeans.shadowedObjectConstructionHelper
                .constructShadowedObject(ctx, repoShadow, resourceObject, result);
    }

    /**
     * Acquires repo shadow in non-OK situations. If not possible, steps down to "ultra-emergency", where
     * attributes are stripped down to a bare primary identifier.
     */
    @NotNull
    private ShadowType acquireRepoShadowInEmergency(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, ExpressionEvaluationException, EncryptionException, SecurityViolationException {
        LOGGER.trace("Acquiring repo shadow in emergency:\n{}", DebugUtil.debugDumpLazily(resourceObject, 1));
        try {
            return ictx.localBeans.shadowAcquisitionHelper
                    .acquireRepoShadow(ictx.ctx, resourceObject, true, result);
        } catch (Exception e) {
            shadowedObject = shadowResourceObjectInUltraEmergency(result);
            throw e;
        }
    }

    /**
     * Something prevents us from creating a shadow (most probably). Let us be minimalistic, and create
     * a shadow having only the primary identifier.
     */
    private ShadowType shadowResourceObjectInUltraEmergency(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, ExpressionEvaluationException, EncryptionException, SecurityViolationException {
        ShadowType minimalResourceObject = Util.minimize(resourceObject, ictx.ctx.getObjectDefinitionRequired());
        LOGGER.trace("Minimal resource object to acquire a shadow for:\n{}",
                DebugUtil.debugDumpLazily(minimalResourceObject, 1));
        if (minimalResourceObject != null) {
            return ictx.localBeans.shadowAcquisitionHelper
                    .acquireRepoShadow(ictx.ctx, minimalResourceObject, true, result);
        } else {
            return null;
        }
    }

    @Override
    public Trace getLogger() {
        return LOGGER;
    }

    @Override
    public InitializationState getInitializationState() {
        return initializationState;
    }

    @Override
    public void checkConsistence() {
        if (shadowedObject != null) {
            ProvisioningUtil.validateShadow(shadowedObject, true);
        } else {
            ProvisioningUtil.validateShadow(resourceObject, false);
        }
    }

    public @NotNull ShadowType getResourceObject() {
        return resourceObject;
    }

    private @NotNull ShadowType getAdoptedOrOriginalObject() {
        return MoreObjects.firstNonNull(shadowedObject, resourceObject);
    }

    // TEMPORARY (for migration)
    @NotNull
    private ShadowType getResourceObjectWithFetchResult() {
        initializationState.checkAfterInitialization();

        if (initializationState.isOk()) {
            return getAdoptedOrOriginalObject();
        } else {
            ShadowType mostRelevantObject = getAdoptedOrOriginalObject();
            ShadowType clone = mostRelevantObject.clone();
            if (clone.getName() == null) {
                if (CollectionUtils.isEmpty(ShadowUtil.getPrimaryIdentifiers(clone))) {
                    // HACK HACK HACK
                    clone.setName(PolyStringType.fromOrig(String.valueOf(primaryIdentifierValue)));
                } else {
                    try {
                        PolyString name = ShadowUtil.determineShadowName(clone);
                        if (name != null) {
                            clone.setName(new PolyStringType(name));
                        }
                    } catch (SchemaException e) {
                        LOGGER.warn("Couldn't determine the name for {}", clone, e);
                    }
                }
            }
            OperationResult result = new OperationResult("adoptObject"); // TODO HACK HACK HACK
            Throwable exceptionEncountered = initializationState.getExceptionEncountered();
            // TODO HACK HACK
            result.recordFatalError(Objects.requireNonNullElseGet(
                    exceptionEncountered, () -> new IllegalStateException("Object was not initialized")));
            ObjectTypeUtil.recordFetchError(clone, result);
            return clone;
        }
    }

    // Maybe temporary
    ShadowType getResultingObject(FetchErrorReportingMethodType errorReportingMethod) {
        initializationState.checkAfterInitialization();

        Throwable exception = initializationState.getExceptionEncountered();
        if (exception == null) {
            return getAdoptedOrOriginalObject();
        } else if (errorReportingMethod != FETCH_RESULT) {
            throw new TunnelException(exception);
        } else {
            ShadowType resultingObject = getResourceObjectWithFetchResult();
            LOGGER.error("An error occurred while processing resource object {}. Recording it into object "
                    + "fetch result: {}", resultingObject, exception.getMessage(), exception);
            return resultingObject;
        }
    }

    private static class InitializationContext {

        private final ShadowsLocalBeans localBeans;
        private final ProvisioningContext ctx;

        private InitializationContext(ShadowsLocalBeans localBeans, ProvisioningContext ctx) {
            this.localBeans = localBeans;
            this.ctx = ctx;
        }
    }
}
