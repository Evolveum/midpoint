/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.impl.LazilyInitializableMixin;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;

import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.AbstractLazilyInitializableResourceEntity;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExistingResourceObject;
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
 * @see AbstractLazilyInitializableResourceEntity
 */
public abstract class AbstractShadowedEntity implements LazilyInitializableMixin {

    /** The resource entity (object or change) that this entity is based on. */
    @NotNull private final AbstractLazilyInitializableResourceEntity prerequisite;

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

    AbstractShadowedEntity(@NotNull AbstractLazilyInitializableResourceEntity prerequisite) {
        this.prerequisite = prerequisite;
    }

    @Override
    public void initializeInternalCommon(Task task, OperationResult result) throws SchemaException, ConfigurationException {
        globalCtx = prerequisite.getEffectiveCtx().spawn(task);
    }

    //region Shadow acquisition

    /**
     * Looks up and creates (if needed) a shadow for the resource object (in {@link #getExistingResourceObjectRequired()}).
     * Deals with errors.
     */
    @NotNull RepoShadow acquireRepoShadow(OperationResult result) throws SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, ExpressionEvaluationException, EncryptionException,
            SecurityViolationException {

        ExistingResourceObject resourceObject = getExistingResourceObjectRequired();

        // The resource object does not have any kind or intent at this point.
        // But let us at least apply the definition using object class(es) in the fetched object.
        // TODO remove this! It should not be needed.
        ProvisioningContext estimatedCtx = b.shadowCaretaker.reapplyDefinitions(globalCtx, resourceObject.getBean());

        // Now find or create repository shadow, along with its classification (maybe it is not a good idea to merge the two).
        try {
            return ShadowAcquisition.acquireRepoShadow(estimatedCtx, resourceObject, false, result);
        } catch (Exception e) {
            // No need to log stack trace now. It will be logged at the place where the exception is processed.
            LoggingUtils.logExceptionAsWarning(
                    getLogger(), "Couldn't acquire shadow for {}. Creating shadow in emergency mode. Error: {}",
                    e, resourceObject);
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
    @NotNull RepoShadow acquireRepoShadowInEmergency(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, ExpressionEvaluationException, EncryptionException, SecurityViolationException {
        getLogger().trace("Acquiring repo shadow in emergency:\n{}", debugDumpLazily( 1));
        try {
            return ShadowAcquisition.acquireRepoShadow(
                    globalCtx, getExistingResourceObjectRequired(), true, result);
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
    private @NotNull RepoShadow acquireRepoShadowInUltraEmergency(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, ExpressionEvaluationException, EncryptionException, SecurityViolationException {
        ExistingResourceObject minimalResourceObject = getExistingResourceObjectRequired().minimize();
        getLogger().trace("Minimal resource object to acquire a shadow for:\n{}",
                DebugUtil.debugDumpLazily(minimalResourceObject, 1));
        return ShadowAcquisition.acquireRepoShadow(globalCtx, minimalResourceObject, true, result);
    }
    //endregion

    //region Shadow management
    /** The shadow is reloaded on special occasions (index-only attributes). TODO review that behavior. */
    RepoShadow updateShadowInRepository(
            ProvisioningContext shadowCtx, RepoShadow repoShadow, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        // TODO [from ShadowedChange]:
        //  should we update the repo even if we obtained current resource object from the cache? (except for e.g. metadata)
        return b.shadowUpdater.updateShadowInRepository(
                shadowCtx, getExistingResourceObjectRequired(), getResourceObjectDelta(), repoShadow, result);
    }

    @NotNull ExistingResourceObject createShadowedObject(
            ProvisioningContext shadowCtx, RepoShadow repoShadow, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException, EncryptionException {
        // TODO do we want also to futurize the shadow like in getObject?
        // TODO [from ShadowedChange] should we bother merging if we obtained resource object from the cache?
        return ShadowedObjectConstruction.construct(shadowCtx, repoShadow, getExistingResourceObjectRequired(), result);
    }
    //endregion

    //region "SPI"

    /** Underlying resource object. Be sure to avoid calling it when there's none. */
    abstract @NotNull ExistingResourceObject getExistingResourceObjectRequired();

    /** The delta - always null for search, null or non-null for changes. */
    abstract @Nullable ObjectDelta<ShadowType> getResourceObjectDelta();

    /**
     * Sends the emergency repo shadow to the subclass. Wwe cannot return it directly,
     * as we throw an exception in such cases.
     */
    abstract void setAcquiredRepoShadowInEmergency(RepoShadow repoShadow);

    //endregion

    public @NotNull InitializationState getInitializationState() {
        return initializationState;
    }
}
