/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType.FETCH_RESULT;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.AbstractLazilyInitializableResourceEntity;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExistingResourceObjectShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectFound;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Represents the processing of an object found on the resource using `searchObjects` call
 * and then "shadowed" by connecting with repo shadow; updating the shadow if necessary.
 */
public class ShadowedObjectFound extends AbstractLazilyInitializableShadowedEntity {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowedObjectFound.class);

    /** The resource object that corresponds to this instance. */
    @NotNull private final ResourceObjectFound resourceObjectFound;

//    /**
//     * The object after "shadowization". Fulfills the following:
//     *
//     * In initialized/OK state:
//     *
//     * 1. has an OID and exists in repository,
//     * 2. has classification applied (plus all the other things done when acquiring a shadow),
//     * 3. the repo shadow is updated: meaning cached attributes and activation, shadow name, aux object classes,
//     * exists flag, caching metadata,
//     * 4. contains all information merged from the resource object and the pre-existing shadow,
//     *
//     * In all other states:
//     *
//     * All reasonable attempts are done in order to create at least minimalistic shadow (because of error handling).
//     * This object points to such a shadow. The other parts (from resource object) can be missing.
//     */

    ShadowedObjectFound(@NotNull ResourceObjectFound resourceObjectFound) {
        super(resourceObjectFound);
        this.resourceObjectFound = resourceObjectFound;
    }

    @Override
    public @NotNull AbstractLazilyInitializableResourceEntity getPrerequisite() {
        return resourceObjectFound;
    }

    /** Executed for both OK and error scenarios. */
    @Override
    protected RepoShadowWithState acquireOrLookupRepoShadow(OperationResult result)
            throws SchemaException, ConfigurationException, EncryptionException {
        return acquireRepoShadow(resourceObjectFound.getResourceObject(), result);
    }

    @Override
    public void classifyUpdateAndCombine(Task task, OperationResult result) throws CommonException {

        assert repoShadow != null;

        shadowPostProcessor = new ShadowPostProcessor(
                effectiveCtx,
                repoShadow,
                resourceObjectFound.getResourceObject(),
                null);

        shadowPostProcessor.execute(result);
    }

    @Override
    public @NotNull Trace getLogger() {
        return LOGGER;
    }

    @Override
    public void checkConsistence() {
        var shadowedObject = getShadowedObject();
        if (shadowedObject != null) {
            ProvisioningUtil.validateShadow(shadowedObject, true);
        } else {
            ProvisioningUtil.validateShadow(getExistingResourceObjectRequired().getBean(), false);
        }
    }

    private @NotNull ShadowType getMostRelevantObjectForError(@NotNull OperationResult result) {
        if (shadowPostProcessor != null && shadowPostProcessor.getCombinedObject() != null) {
            // Probably will not be the case, but not impossible either.
            return shadowPostProcessor.getCombinedObject().getBean();
        }
        if (repoShadow != null) {
            try {
                return ShadowedObjectConstruction
                        .construct(effectiveCtx, repoShadow.shadow(), getExistingResourceObjectRequired(), result)
                        .getBean();
            } catch (Exception e) {
                LOGGER.debug("Couldn't create the shadowed object during error handling for {}, will use repo shadow", this, e);
                return repoShadow.getBean();
            }
        }
        ExistingResourceObjectShadow resourceObject = getExistingResourceObjectRequired();
        var resourceObjectBean = resourceObject.getBean();
        if (resourceObjectBean.getName() == null) {
            // most probably the case, as the resource objects do not have prism object names
            try {
                resourceObjectBean.setName(PolyString.toPolyStringType(resourceObject.determineShadowName()));
            } catch (SchemaException e) {
                LOGGER.debug("Couldn't determine the name for {}, continuing without one", this, e);
            }
        }
        return resourceObjectBean;
    }

    /**
     * Returns object that should be passed to provisioning module client.
     * (Maybe temporary, until we return this instance - or similar data structure - directly.)
     */
    @NotNull ShadowType getResultingObject(
            @Nullable FetchErrorReportingMethodType errorReportingMethod, @NotNull OperationResult result) {
        checkInitialized();

        Throwable exception = getExceptionEncountered();
        if (exception == null) {
            return shadowPostProcessor.getCombinedObject().getBean();
        } else if (errorReportingMethod != FETCH_RESULT) {
            throw new TunnelException(exception);
        } else {
            ShadowType resultingObject = getResourceObjectWithErrorFetchResult(result);
            // TODO should we log the error like this? MID-2119
            LOGGER.error("An error occurred while processing resource object {}. Recording it into object fetch result: {}",
                    resultingObject, exception.getMessage(), exception);
            return resultingObject;
        }
    }

    @NotNull private ShadowType getResourceObjectWithErrorFetchResult(@NotNull OperationResult result) {
        assert !isOk();
        ShadowType mostRelevantObject = getMostRelevantObjectForError(result).clone();
        OperationResult lResult = new OperationResult("shadowObject"); // TODO HACK HACK HACK MID-2119
        lResult.recordFatalError(Objects.requireNonNullElseGet(
                getExceptionEncountered(), () -> new IllegalStateException("Object was not initialized")));
        ObjectTypeUtil.recordFetchError(mostRelevantObject, lResult);
        return mostRelevantObject;
    }

    /** The resource object as obtained from the resource object converter. It has no connection to the repo. */
    @Override
    public @NotNull ExistingResourceObjectShadow getExistingResourceObjectRequired() {
        return resourceObjectFound.getResourceObject();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(this.getClass().getSimpleName());
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "initializationState", String.valueOf(initializationState), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObjectFound", resourceObjectFound, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "shadowedObject", getShadowedObject(), indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "resourceObjectFound=" + resourceObjectFound +
                ", shadowedObject=" + getShadowedObject() +
                ", state=" + initializationState +
                '}';
    }
}
