package com.evolveum.midpoint.provisioning.impl.shadowcache;

import static java.util.Objects.requireNonNull;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadowmanager.ShadowManager;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Helps with the resource object adoption process (acquiring repo shadows, shadow completion).
 */
@Experimental
@Component
class AdoptionHelper {

    private static final Trace LOGGER = TraceManager.getTrace(AdoptionHelper.class);

    @Autowired protected ShadowManager shadowManager;
    @Autowired private CommonBeans commonBeans;
    @Autowired private CommonHelper commonHelper;

    /**
     * Acquires repository shadow for a provided resource object. The repository shadow is located or created.
     * In case that the shadow is created, all additional ceremonies for a new shadow is done, e.g. invoking
     * change notifications (discovery).
     *
     * Returned shadow is NOT guaranteed to have all the attributes aligned and updated. That is only possible after
     * completeShadow(). But maybe, this method can later invoke completeShadow() and do all the necessary stuff?
     *
     * It may look like this method would rather belong to ShadowManager. But it does NOT. It does too much stuff
     * (e.g. change notification).
     */
    public @NotNull PrismObject<ShadowType> acquireRepoShadow(ProvisioningContext ctx,
            PrismObject<ShadowType> resourceObject, boolean unknownIntent, boolean isDoDiscovery, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, GenericConnectorException, ExpressionEvaluationException, EncryptionException {

        PrismProperty<?> primaryIdentifier = requireNonNull(
                ProvisioningUtil.getSingleValuedPrimaryIdentifier(resourceObject),
                () -> "No primary identifier value in " + ShadowUtil.shortDumpShadow(resourceObject));

        // In theory, this is the same as the object class in the context.
        // But e.g. in the case of entitlements completion the current code creates ctx from definitions,
        // not from the actual objects. So let's be safe and use the actual object class name.
        QName objectClass = requireNonNull(
                resourceObject.asObjectable().getObjectClass(),
                () -> "No object class in " + ShadowUtil.shortDumpShadow(resourceObject));

        PrismObject<ShadowType> existingRepoShadow = shadowManager.lookupLiveShadowByPrimaryId(ctx, primaryIdentifier,
                objectClass, result);

        if (existingRepoShadow != null) {
            LOGGER.trace("Found shadow object in the repository {}", ShadowUtil.shortDumpShadowLazily(existingRepoShadow));
            return existingRepoShadow;
        }

        LOGGER.trace("Shadow object (in repo) corresponding to the resource object (on the resource) was not found. "
                + "The repo shadow will be created. The resource object:\n{}", resourceObject);

        // TODO: do something about shadows with mathcing secondary identifiers? We do not need to care about these any longer, do we?
        // MID-5844
        // we need it for the consistency. following is the use case. Account is beeing created on the resource
        // but the resource cannot be reached by midPoint. Meanwhile the account is created manually in the resource
        // when reconciliation runs, origin account created while resource was not reachable doesn't contain primary identifier
        // so we rather try also secondary identifier to be sure nothing goes wrong
//        PrismObject<ShadowType> repoShadow;

//        PrismObject<ShadowType> shadowBySecondaryIdentifier = shadowManager.lookupConflictingShadowBySecondaryIdentifiers(ctx,
//                resourceShadow, result);
//
//        // check if the shadow contains primary identifier. if it does, we can skip the next session
//        if (shadowBySecondaryIdentifier != null) {
//
//            ShadowType shadowType = shadowBySecondaryIdentifier.asObjectable();
//            String currentPrimaryIdentifier = shadowManager.determinePrimaryIdentifierValue(ctx, shadowBySecondaryIdentifier);
//
//            LOGGER.info("###CURRENT PI: {}", currentPrimaryIdentifier);
//            String expectedPrimaryIdentifier = shadowManager.determinePrimaryIdentifierValue(ctx, resourceShadow);
//            LOGGER.info("### EXPECTED PI: {}", expectedPrimaryIdentifier);
//
//            if (!StringUtils.equals(currentPrimaryIdentifier, expectedPrimaryIdentifier)) {
//                shadowCaretaker.applyAttributesDefinition(ctx, shadowBySecondaryIdentifier);
//                shadowBySecondaryIdentifier = completeShadow(ctx, resourceShadow, shadowBySecondaryIdentifier, isDoDiscovery, result);
//                Task task = taskManager.createTaskInstance();
//                ResourceOperationDescription failureDescription = ProvisioningUtil.createResourceFailureDescription(shadowBySecondaryIdentifier, ctx.getResource(), null, result);
//                shadowBySecondaryIdentifier.asObjectable().setDead(Boolean.TRUE);
//                changeNotificationDispatcher.notifyFailure(failureDescription, task, result);
//                shadowManager.deleteConflictedShadowFromRepo(shadowBySecondaryIdentifier, result);
//            }
//
//        }
//

        // The resource object obviously exists on the resource, but appropriate shadow does
        // not exist in the repository we need to create the shadow to align repo state to the
        // reality (resource)

        PrismObject<ShadowType> createdRepoShadow;
        try {

            createdRepoShadow = shadowManager.addDiscoveredRepositoryShadow(ctx, resourceObject, result);

        } catch (ObjectAlreadyExistsException e) {
            // Conflict! But we haven't supplied an OID and we have checked for existing shadow before,
            // therefore there should not conflict. Unless someone managed to create the same shadow
            // between our check and our create attempt. In that case try to re-check for shadow existence
            // once more.

            OperationResult originalRepoAddSubresult = result.getLastSubresult();

            LOGGER.debug("Attempt to create new repo shadow for {} ended up in conflict, re-trying the search for repo shadow", resourceObject);
            PrismObject<ShadowType> conflictingShadow = shadowManager.lookupLiveShadowByPrimaryId(ctx, primaryIdentifier, objectClass, result);

            if (conflictingShadow == null) {
                // This is really strange. The shadow should not have disappeared in the meantime, dead shadow would remain instead.
                // Maybe we have broken "indexes"? (e.g. primaryIdentifierValue column)

                // Do some "research" and log the results, so we have good data to diagnose this situation.
                String determinedPrimaryIdentifierValue = shadowManager.determinePrimaryIdentifierValue(ctx, resourceObject);
                PrismObject<ShadowType> potentialConflictingShadow = shadowManager.lookupShadowByIndexedPrimaryIdValue(ctx,
                        determinedPrimaryIdentifierValue, result);

                LOGGER.error("Unexpected repository behavior: object already exists error even after we double-checked shadow uniqueness: {}", e.getMessage(), e);
                LOGGER.debug("REPO CONFLICT: resource shadow\n{}", resourceObject.debugDump(1));
                LOGGER.debug("REPO CONFLICT: resource shadow: determined primaryIdentifierValue: {}", determinedPrimaryIdentifierValue);
                LOGGER.debug("REPO CONFLICT: potential conflicting repo shadow (by primaryIdentifierValue)\n{}", potentialConflictingShadow==null?null:potentialConflictingShadow.debugDump(1));

                throw new SystemException(
                        "Unexpected repository behavior: object already exists error even after we double-checked shadow uniqueness: " + e.getMessage(), e);
            }

            originalRepoAddSubresult.muteError();
            return conflictingShadow;
        }

        resourceObject.setOid(createdRepoShadow.getOid());
        ShadowType resourceShadowBean = resourceObject.asObjectable();
        if (resourceShadowBean.getResourceRef() == null) {
            resourceShadowBean.setResourceRef(new ObjectReferenceType());
        }
        resourceShadowBean.getResourceRef().asReferenceValue().setObject(ctx.getResource().asPrismObject());

        if (isDoDiscovery) {
            // We have object for which there was no shadow. Which means that midPoint haven't known about this shadow before.
            // Invoke notifyChange() so the new shadow is properly initialized.

            commonHelper.notifyResourceObjectChangeListeners(resourceObject, ctx.getResource().asPrismObject(), true);
        }

        PrismObject<ShadowType> finalRepoShadow;
        if (unknownIntent) {
            // Intent may have been changed during the notifyChange processing.
            // Re-read the shadow if necessary.
            finalRepoShadow = shadowManager.fixShadow(ctx, createdRepoShadow, result);
        } else {
            finalRepoShadow = createdRepoShadow;
        }

        LOGGER.trace("Final repo shadow (created and possibly re-read):\n{}", finalRepoShadow.debugDumpLazily(1));
        return finalRepoShadow;
    }

    /**
     * Make sure that the shadow is complete, e.g. that all the mandatory fields
     * are filled (e.g name, resourceRef, ...) Also transforms the shadow with
     * respect to simulated capabilities. Also shadowRefs are added to associations.
     */
    @NotNull PrismObject<ShadowType> completeShadow(ProvisioningContext ctx,
            PrismObject<ShadowType> resourceShadow, PrismObject<ShadowType> repoShadow, boolean isDoDiscovery,
            OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException,
            CommunicationException, SecurityViolationException, GenericConnectorException, ExpressionEvaluationException, EncryptionException {

        return ShadowCompletion.create(ctx, resourceShadow, repoShadow, isDoDiscovery, commonBeans)
                .completeShadow(parentResult);
    }

}
