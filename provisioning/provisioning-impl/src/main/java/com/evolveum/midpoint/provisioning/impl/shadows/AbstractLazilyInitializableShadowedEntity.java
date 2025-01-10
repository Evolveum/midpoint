/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.impl.LazilyInitializableMixin;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.AbstractLazilyInitializableResourceEntity;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExistingResourceObjectShadow;
import com.evolveum.midpoint.provisioning.impl.shadows.sync.NotApplicableException;
import com.evolveum.midpoint.provisioning.util.InitializationState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import java.util.List;

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
public abstract class AbstractLazilyInitializableShadowedEntity implements LazilyInitializableMixin {

    /** The resource entity (object or change) that this entity is based on. */
    @NotNull private final AbstractLazilyInitializableResourceEntity prerequisite;

    /** State of the processing. */
    @NotNull final InitializationState initializationState = InitializationState.created();

    /**
     * The context from {@link #prerequisite} with the worker task and - in rare cases - the shadow applied.
     * (The original context may be in some degree of refinement, so it may even contain the correct task.)
     *
     * Non-null since initialization start.
     */
    ProvisioningContext effectiveCtx;

    /**
     * Repository shadow obtained by the initial acquisition operation (that is executed always at the beginning).
     *
     * Can be null only for "delete" changes with no corresponding shadows.
     */
    @Nullable RepoShadowWithState repoShadow;

    /**
     * Represents the post-processing of the shadow: classification, update, and combining the repo shadow with resource object.
     * Present here to provide the most current result even in the case of an exception.
     */
    ShadowPostProcessor shadowPostProcessor;

    /** Useful beans local to the Shadows package. */
    @NotNull final ShadowsLocalBeans b = ShadowsLocalBeans.get();

    AbstractLazilyInitializableShadowedEntity(@NotNull AbstractLazilyInitializableResourceEntity prerequisite) {
        this.prerequisite = prerequisite;
    }

    /** Called in all cases (OK, error, not applicable). */
    @Override
    public void initializeInternalCommon(Task task, OperationResult result)
            throws SchemaException, ConfigurationException, EncryptionException {
        effectiveCtx = prerequisite.getEffectiveCtx().spawn(
                List.of(), // assuming the auxiliary OCs were filled in by lower layers
                task);
        repoShadow = acquireOrLookupRepoShadow(result);
        if (effectiveCtx.isWildcard() && repoShadow != null) {
            effectiveCtx = effectiveCtx.spawnForShadow(repoShadow.getBean());
        }
    }

    public void initializeInternalForPrerequisiteOk(Task task, OperationResult result)
            throws CommonException, NotApplicableException {
        classifyUpdateAndCombine(task, result);
        // Note that no futurization is done here, unlike in ShadowGetOperation.
    }

    @Override
    public void initializeInternalForPrerequisiteError(Task task, OperationResult result) {
        // Nothing to do here. We have the shadow but we don't want to do any further actions to avoid breaking it.
    }

    @Override
    public void initializeInternalForPrerequisiteNotApplicable(Task task, OperationResult result) {
        // Nothing to do here.
    }

    /** Classify the repo shadow, update it with the current knowledge, and create the shadowed object. */
    abstract void classifyUpdateAndCombine(Task task, OperationResult result)
            throws CommonException, NotApplicableException;

    /** Usually the full acquisition is done, but for delete deltas we just look for the existing repo (if any). */
    protected abstract RepoShadowWithState acquireOrLookupRepoShadow(OperationResult result)
            throws SchemaException, ConfigurationException, EncryptionException;

    /** Looks up and creates (if needed) a shadow for the resource object. Deals with errors. */
    @NotNull
    RepoShadowWithState acquireRepoShadow(@NotNull ExistingResourceObjectShadow resourceObject, OperationResult result)
            throws SchemaException, ConfigurationException, EncryptionException {

        try {
            return ShadowAcquisition.acquireRepoShadow(effectiveCtx, resourceObject, result);
        } catch (Exception e) {
            // No need to log stack trace now. It will be logged at the place where the exception is processed.
            LoggingUtils.logExceptionAsWarning(
                    getLogger(),
                    "Couldn't acquire shadow for {}. Creating shadow in emergency mode (identifiers only). Error: {}",
                    e, resourceObject);
            try {
                repoShadow = ShadowAcquisition.acquireRepoShadow(effectiveCtx, resourceObject.withIdentifiersOnly(), result);
                throw e;
            } catch (Exception e2) {
                LoggingUtils.logExceptionAsWarning(
                        getLogger(),
                        "Couldn't acquire shadow for {}. Creating shadow in ultra emergency mode "
                                + "(primary identifier only). Error: {}",
                        e2, resourceObject);
                repoShadow = ShadowAcquisition.acquireRepoShadow(effectiveCtx, resourceObject.withPrimaryIdentifierOnly(), result);
                throw e2;
            }
        }
    }

    /** Underlying resource object. Be sure to avoid calling it when there's none. */
    abstract @NotNull ExistingResourceObjectShadow getExistingResourceObjectRequired();

    public @NotNull InitializationState getInitializationState() {
        return initializationState;
    }

    /**
     * FIXME fix this description and review this method [MID-2119]
     *
     * The resulting combination of resource object and its repo shadow. Special cases:
     *
     * 1. For resources without read capability it is based on the cached repo shadow.
     * 2. For delete deltas, it is the current shadow, with applied definitions.
     * 3. In emergency it is the same as the current repo shadow.
     *
     * The point #2 should be perhaps reconsidered.
     */
    public ShadowType getShadowedObject() {
        if (shadowPostProcessor != null) {
            var combinedObject = shadowPostProcessor.getCombinedObject();
            if (combinedObject != null) {
                return combinedObject.getBean();
            }
        }
        if (repoShadow != null) {
            return repoShadow.getBean();
        }
        return null;
    }
}
