package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.provisioning.impl.shadows.ConstraintsChecker;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 * Creates shadows as needed.
 *
 * This is a result of preliminary split of {@link ShadowManager} functionality that was done in order
 * to make it more understandable. Most probably it is not good enough and should be improved.
 */
@Component
@Experimental
class ShadowCreator {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowManager.class);

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private Clock clock;
    @Autowired private PrismContext prismContext;
    @Autowired private Protector protector;
    @Autowired private ShadowFinder shadowFinder;
    @Autowired private Helper helper;
    @Autowired private ShadowManager shadowManager;
    @Autowired private CreatorUpdaterHelper creatorUpdaterHelper;
    @Autowired private PendingOperationsHelper pendingOperationsHelper;

    @NotNull
    public PrismObject<ShadowType> addDiscoveredRepositoryShadow(ProvisioningContext ctx,
            PrismObject<ShadowType> resourceObject, OperationResult parentResult) throws SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException,
            EncryptionException {
        LOGGER.trace("Adding new shadow from resource object:\n{}", resourceObject.debugDumpLazily(1));
        PrismObject<ShadowType> repoShadow = createRepositoryShadow(ctx, resourceObject);
        ConstraintsChecker.onShadowAddOperation(repoShadow.asObjectable()); // TODO eventually replace by repo cache invalidation
        String oid = repositoryService.addObject(repoShadow, null, parentResult);
        repoShadow.setOid(oid);
        LOGGER.debug("Added new shadow (from resource object): {}", repoShadow);
        LOGGER.trace("Added new shadow (from resource object):\n{}", repoShadow.debugDumpLazily(1));
        return repoShadow;
    }

    public void addNewProposedShadow(ProvisioningContext ctx, PrismObject<ShadowType> shadowToAdd,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, ObjectAlreadyExistsException, EncryptionException {

        if (!creatorUpdaterHelper.isUseProposedShadows(ctx)) {
            return;
        }

        PrismObject<ShadowType> existingRepoShadow = opState.getRepoShadow();
        if (existingRepoShadow != null) {
            // TODO: should we add pending operation here?
            return;
        }

        // This is wrong: MID-4833
        PrismObject<ShadowType> newRepoShadow = createRepositoryShadow(ctx, shadowToAdd);
        newRepoShadow.asObjectable().setLifecycleState(SchemaConstants.LIFECYCLE_PROPOSED);
        opState.setExecutionStatus(PendingOperationExecutionStatusType.REQUESTED);
        pendingOperationsHelper.addPendingOperationAdd(newRepoShadow, shadowToAdd, opState, task.getTaskIdentifier());

        ConstraintsChecker.onShadowAddOperation(newRepoShadow.asObjectable()); // TODO migrate to cache invalidation process
        String oid = repositoryService.addObject(newRepoShadow, null, result);
        newRepoShadow.setOid(oid);
        LOGGER.trace("Proposed shadow added to the repository: {}", newRepoShadow);
        opState.setRepoShadow(newRepoShadow);
    }

    /**
     * Create a copy of a shadow that is suitable for repository storage.
     */
    @NotNull PrismObject<ShadowType> createRepositoryShadow(ProvisioningContext ctx, PrismObject<ShadowType> shadow)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException, EncryptionException {

        ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(shadow);

        PrismObject<ShadowType> repoShadow = shadow.clone();
        ShadowType repoShadowType = repoShadow.asObjectable();

        ResourceAttributeContainer repoAttributesContainer = ShadowUtil.getAttributesContainer(repoShadow);
        repoShadowType.setPrimaryIdentifierValue(helper.determinePrimaryIdentifierValue(ctx, shadow));

        CachingStategyType cachingStrategy = ProvisioningUtil.getCachingStrategy(ctx);
        if (cachingStrategy == CachingStategyType.NONE) {
            // Clean all repoShadow attributes and add only those that should be there
            repoAttributesContainer.clear();
            Collection<ResourceAttribute<?>> primaryIdentifiers = attributesContainer.getPrimaryIdentifiers();
            for (PrismProperty<?> p : primaryIdentifiers) {
                repoAttributesContainer.add(p.clone());
            }

            Collection<ResourceAttribute<?>> secondaryIdentifiers = attributesContainer.getSecondaryIdentifiers();
            for (PrismProperty<?> p : secondaryIdentifiers) {
                repoAttributesContainer.add(p.clone());
            }

            // Also add all the attributes that act as association identifiers.
            // We will need them when the shadow is deleted (to remove the shadow from entitlements).
            RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
            for (RefinedAssociationDefinition associationDef : objectClassDefinition.getAssociationDefinitions()) {
                if (associationDef.getResourceObjectAssociationType().getDirection() == ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT) {
                    QName valueAttributeName = associationDef.getResourceObjectAssociationType().getValueAttribute();
                    if (repoAttributesContainer.findAttribute(valueAttributeName) == null) {
                        ResourceAttribute<Object> valueAttribute = attributesContainer.findAttribute(valueAttributeName);
                        if (valueAttribute != null) {
                            repoAttributesContainer.add(valueAttribute.clone());
                        }
                    }
                }
            }

            repoShadowType.setCachingMetadata(null);

            ProvisioningUtil.cleanupShadowActivation(repoShadowType);

        } else if (cachingStrategy == CachingStategyType.PASSIVE) {
            // Do not need to clear anything. Just store all attributes and add metadata.
            CachingMetadataType cachingMetadata = new CachingMetadataType();
            cachingMetadata.setRetrievalTimestamp(clock.currentTimeXMLGregorianCalendar());
            repoShadowType.setCachingMetadata(cachingMetadata);

        } else {
            throw new ConfigurationException("Unknown caching strategy " + cachingStrategy);
        }

        helper.setKindIfNecessary(repoShadowType, ctx.getObjectClassDefinition());
//        setIntentIfNecessary(repoShadowType, objectClassDefinition);

        // Store only password meta-data in repo - unless there is explicit caching
        CredentialsType creds = repoShadowType.getCredentials();
        if (creds != null) {
            PasswordType passwordType = creds.getPassword();
            if (passwordType != null) {
                preparePasswordForStorage(passwordType, ctx.getObjectClassDefinition());
                ObjectReferenceType owner = ctx.getTask() != null ? ctx.getTask().getOwnerRef() : null;
                ProvisioningUtil.addPasswordMetadata(passwordType, clock.currentTimeXMLGregorianCalendar(), owner);
            }
            // TODO: other credential types - later
        }

        // if shadow does not contain resource or resource reference, create it
        // now
        if (repoShadowType.getResourceRef() == null) {
            repoShadowType.setResourceRef(ObjectTypeUtil.createObjectRef(ctx.getResource(), prismContext));
        }

        if (repoShadowType.getName() == null) {
            repoShadowType.setName(new PolyStringType(ShadowUtil.determineShadowName(shadow)));
        }

        if (repoShadowType.getObjectClass() == null) {
            repoShadowType.setObjectClass(attributesContainer.getDefinition().getTypeName());
        }

        if (repoShadowType.isProtectedObject() != null) {
            repoShadowType.setProtectedObject(null);
        }

        helper.normalizeAttributes(repoShadow, ctx.getObjectClassDefinition());

        return repoShadow;
    }

    private void preparePasswordForStorage(PasswordType passwordType,
            RefinedObjectClassDefinition objectClassDefinition) throws SchemaException, EncryptionException {
        ProtectedStringType passwordValue = passwordType.getValue();
        if (passwordValue == null) {
            return;
        }
        CachingStategyType cachingStrategy = ProvisioningUtil.getPasswordCachingStrategy(objectClassDefinition);
        if (cachingStrategy != null && cachingStrategy != CachingStategyType.NONE) {
            if (!passwordValue.isHashed()) {
                protector.hash(passwordValue);
            }
        } else {
            ProvisioningUtil.cleanupShadowPassword(passwordType);
        }
    }

}
