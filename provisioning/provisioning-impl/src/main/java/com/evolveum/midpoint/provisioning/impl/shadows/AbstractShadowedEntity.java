/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.impl.InitializableObjectMixin;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;

import com.evolveum.midpoint.provisioning.impl.resourceobjects.AbstractResourceEntity;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObject;
import com.evolveum.midpoint.provisioning.util.InitializationState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Processing of retrieved resource objects that is common for search and sync operations.
 *
 * Main responsibilities:
 *
 * - shadow acquisition in emergency and ultra-emergency modes
 * - shadow management (update, creation of shadowed object)
 * - support for initialization and provisioning context information
 *
 * Note that resource object, repo shadow, and shadowed objects are not held here, as their semantics are different
 * for objects and changes. They are accessed by abstract ("SPI") methods.
 *
 * @see ShadowedObjectFound
 * @see ShadowedChange
 *
 * @see AbstractResourceEntity
 */
public abstract class AbstractShadowedEntity implements InitializableObjectMixin {

    /** The resource entity (object or change) that this entity is based on. */
    @NotNull private final AbstractResourceEntity prerequisite;

    /**
     * The context from {@link #prerequisite} with the worker task applied. (The original context may be in various
     * degree of refinement, so it may even contain the correct task.)
     *
     * The name of "global" means that the context is most probably not refined to match given (classified) resource object.
     * The best we can hope for here is the object class information.
     *
     * Available since initialization start.
     */
    ProvisioningContext globalCtx;

    /** State of the processing. */
    @NotNull final InitializationState initializationState = InitializationState.created();

    /** Useful beans local to the Shadows package. */
    @NotNull final ShadowsLocalBeans b = ShadowsLocalBeans.get();

    AbstractShadowedEntity(@NotNull AbstractResourceEntity prerequisite) {
        this.prerequisite = prerequisite;
    }

    @Override
    public void initializeInternalCommon(Task task, OperationResult result) {
        globalCtx = prerequisite.getEffectiveCtx().spawn(task);
    }

    //region Shadow acquisition

    /** Looks up and creates (if needed) a shadow for the resource object (in {@link #getResourceObject()}). Deals with errors. */
    @NotNull ShadowType acquireRepoShadow(OperationResult result) throws SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, ExpressionEvaluationException, EncryptionException,
            SecurityViolationException {

        var resourceObjectBean = getResourceObject().getBean();

        // The resource object does not have any kind or intent at this point.
        // But let us at least apply the definition using object class(es) in the fetched object.
        ProvisioningContext estimatedCtx = b.shadowCaretaker.reapplyDefinitions(globalCtx, resourceObjectBean);

        // Now find or create repository shadow, along with its classification (maybe it is not a good idea to merge the two).
        try {
            return ShadowAcquisition.acquireRepoShadow(estimatedCtx, resourceObjectBean, false, result);
        } catch (Exception e) {
            // No need to log stack trace now. It will be logged at the place where the exception is processed.
            LoggingUtils.logExceptionAsWarning(
                    getLogger(), "Couldn't acquire shadow for {}. Creating shadow in emergency mode. Error: {}",
                    e, resourceObjectBean);
            acquireAndSetRepoShadowInEmergency(result);
            throw e;
        }
    }

    /** Called when we know we are in emergency mode. */
    void acquireAndSetRepoShadowInEmergency(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, ExpressionEvaluationException, EncryptionException, SecurityViolationException {
        setAcquiredRepoShadowInEmergency(
                acquireRepoShadowInEmergency(result));
    }

    /** Acquires repo shadow in emergency situations. Falls back to ultra-emergency, if needed. */
    @NotNull ShadowType acquireRepoShadowInEmergency(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, ExpressionEvaluationException, EncryptionException, SecurityViolationException {
        getLogger().trace("Acquiring repo shadow in emergency:\n{}", debugDumpLazily( 1));
        try {
            return ShadowAcquisition.acquireRepoShadow(
                    globalCtx, getResourceObject().getBean(), true, result);
        } catch (Exception e) {
            setAcquiredRepoShadowInEmergency(
                    acquireRepoShadowInUltraEmergency(result));
            throw e;
        }
    }

    /**
     * Something prevents us from creating a shadow (most probably). Let us be minimalistic, and find/create
     * a shadow using only the primary identifier.
     */
    private @Nullable ShadowType acquireRepoShadowInUltraEmergency(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, ExpressionEvaluationException, EncryptionException, SecurityViolationException {
        ShadowType minimalResourceObject = ShadowsUtil.minimize(
                getResourceObject().getBean(),
                globalCtx.getObjectDefinitionRequired());
        getLogger().trace("Minimal resource object to acquire a shadow for:\n{}",
                DebugUtil.debugDumpLazily(minimalResourceObject, 1));
        if (minimalResourceObject != null) {
            return ShadowAcquisition.acquireRepoShadow(globalCtx, minimalResourceObject, true, result);
        } else {
            return null;
        }
    }
    //endregion

    //region Shadow management
    ShadowType updateShadowInRepository(
            ProvisioningContext shadowCtx, ShadowType repoShadow, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        // TODO [from ShadowedChange]:
        //  should we update the repo even if we obtained current resource object from the cache? (except for e.g. metadata)
        return b.shadowUpdater.updateShadowInRepository(
                shadowCtx, getResourceObject(), getResourceObjectDelta(), repoShadow, result);
    }

    @NotNull ShadowType createShadowedObject(
            ProvisioningContext shadowCtx, ShadowType repoShadow, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException, EncryptionException {
        // TODO do we want also to futurize the shadow like in getObject?
        // TODO [from ShadowedChange] should we bother merging if we obtained resource object from the cache?
        return ShadowedObjectConstruction.construct(shadowCtx, repoShadow, getResourceObject(), result);
    }
    //endregion

    //region "SPI"

    /** Underlying resource object. */
    abstract @NotNull ResourceObject getResourceObject();

    /** The delta - always null for search, null or non-null for changes. */
    abstract @Nullable ObjectDelta<ShadowType> getResourceObjectDelta();

    /**
     * Sends the emergency repo shadow to the subclass. Wwe cannot return it directly,
     * as we throw an exception in such cases.
     */
    abstract void setAcquiredRepoShadowInEmergency(ShadowType repoShadow);

    //endregion

    public @NotNull InitializationState getInitializationState() {
        return initializationState;
    }
}
