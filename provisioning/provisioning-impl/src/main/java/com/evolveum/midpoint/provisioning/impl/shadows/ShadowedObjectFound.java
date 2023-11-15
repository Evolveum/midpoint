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

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.AbstractResourceEntity;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObject;

import com.google.common.base.MoreObjects;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectFound;
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

import org.jetbrains.annotations.Nullable;

/**
 * Represents the processing of an object found on the resource using `searchObjects` call
 * and then "shadowed" by connecting with repo shadow; updating the shadow if necessary.
 */
public class ShadowedObjectFound extends AbstractShadowedEntity {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowedObjectFound.class);

    /** The resource object that corresponds to this instance. */
    @NotNull private final ResourceObjectFound resourceObjectFound;

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

    ShadowedObjectFound(@NotNull ResourceObjectFound resourceObjectFound, @NotNull ProvisioningContext globalCtx) {
        super(resourceObjectFound);
        this.resourceObjectFound = resourceObjectFound;
    }

    @Override
    public @NotNull AbstractResourceEntity getPrerequisite() {
        return resourceObjectFound;
    }

    /**
     * Acquires repo shadow, updates it, and prepares the shadowedObject.
     */
    @Override
    public void initializeInternalForPrerequisiteOk(Task task, OperationResult result)
            throws CommonException, EncryptionException {

        ShadowType repoShadow = acquireRepoShadow(result);
        try {

            // The repo shadow is properly classified at this point. So we determine the definitions (etc) definitely.
            ProvisioningContext shadowCtx = globalCtx.adoptShadow(repoShadow);

            ShadowType updatedRepoShadow = updateShadowInRepository(shadowCtx, repoShadow, result);
            shadowedObject = createShadowedObject(shadowCtx, updatedRepoShadow, result);

        } catch (Exception e) {

            // No need to log stack trace now. It will be logged at the place where the exception is processed.
            // It is questionable whether to log anything at all.
            LOGGER.warn("Couldn't initialize {}. Continuing with previously acquired repo shadow: {}. Error: {}",
                    getResourceObjectRequired(), repoShadow, getClassWithMessage(e));
            shadowedObject = repoShadow;
            throw e;
        }
    }

    /**
     * The object is somehow flawed. However, we try to create a shadow.
     *
     * To avoid any harm, we are minimalistic here: If a shadow can be found, it is used "as is". No updates here.
     * If it cannot be found, it is created. We will skip kind/intent/tag determination. Most probably these would
     * not be correct anyway.
     */
    @Override
    public void initializeInternalForPrerequisiteError(Task task, OperationResult result)
            throws CommonException, EncryptionException {
        acquireAndSetRepoShadowInEmergency(result);
    }

    @Override
    public void initializeInternalForPrerequisiteNotApplicable(Task task, OperationResult result)
            throws CommonException, EncryptionException {
        acquireAndSetRepoShadowInEmergency(result);
    }

    @Override
    public void setAcquiredRepoShadowInEmergency(ShadowType repoShadow) {
        this.shadowedObject = repoShadow;
    }

    @Override
    public @NotNull Trace getLogger() {
        return LOGGER;
    }

    @Override
    public void checkConsistence() {
        if (shadowedObject != null) {
            ProvisioningUtil.validateShadow(shadowedObject, true);
        } else {
            ProvisioningUtil.validateShadow(getResourceObjectRequired().getBean(), false);
        }
    }

    private @NotNull ShadowType getAdoptedOrOriginalObject() {
        return MoreObjects.firstNonNull(shadowedObject, getResourceObjectRequired().getBean());
    }

    /**
     * Returns object that should be passed to provisioning module client.
     * (Maybe temporary, until we return this instance - or similar data structure - directly.)
     */
    @NotNull ShadowType getResultingObject(@Nullable FetchErrorReportingMethodType errorReportingMethod) {
        checkInitialized();

        Throwable exception = getExceptionEncountered();
        if (exception == null) {
            return getAdoptedOrOriginalObject();
        } else if (errorReportingMethod != FETCH_RESULT) {
            throw new TunnelException(exception);
        } else {
            ShadowType resultingObject = getResourceObjectWithFetchResult();
            LOGGER.error("An error occurred while processing resource object {}. Recording it into object fetch result: {}",
                    resultingObject, exception.getMessage(), exception);
            return resultingObject;
        }
    }

    @NotNull private ShadowType getResourceObjectWithFetchResult() {
        if (isOk()) {
            return getAdoptedOrOriginalObject();
        } else {
            ShadowType mostRelevantObject = getAdoptedOrOriginalObject();
            ShadowType clone = mostRelevantObject.clone();
            if (clone.getName() == null) {
                if (CollectionUtils.isEmpty(ShadowUtil.getPrimaryIdentifiers(clone))) {
                    // HACK HACK HACK
                    clone.setName(PolyStringType.fromOrig(String.valueOf(getPrimaryIdentifierValue())));
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
            Throwable exceptionEncountered = getExceptionEncountered();
            // TODO HACK HACK
            result.recordFatalError(Objects.requireNonNullElseGet(
                    exceptionEncountered, () -> new IllegalStateException("Object was not initialized")));
            ObjectTypeUtil.recordFetchError(clone, result);
            return clone;
        }
    }

    /**
     * The resource object as obtained from the resource object converter. It has no connection to the repo.
     *
     * @see ResourceObjectFound#resourceObject
     */
    @Override
    public @NotNull ResourceObject getResourceObjectRequired() {
        return resourceObjectFound.getResourceObject();
    }

    @Override
    public @Nullable ObjectDelta<ShadowType> getResourceObjectDelta() {
        return null; // no delta for searched resource objects
    }

    /** @see ResourceObject#primaryIdentifierValue */
    private Object getPrimaryIdentifierValue() {
        return resourceObjectFound.getPrimaryIdentifierValue();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(this.getClass().getSimpleName());
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObjectFound", resourceObjectFound, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "shadowedObject", shadowedObject, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "initializationState", String.valueOf(initializationState), indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "resourceObjectFound=" + resourceObjectFound +
                ", shadowedObject=" + shadowedObject +
                ", state=" + initializationState +
                '}';
    }
}
