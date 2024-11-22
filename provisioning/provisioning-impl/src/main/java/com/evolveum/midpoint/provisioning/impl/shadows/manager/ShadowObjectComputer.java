/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.prism.polystring.PolyString.toPolyStringType;
import static com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowComputerUtil.*;
import static com.evolveum.midpoint.schema.util.ShadowUtil.removePasswordValueProperty;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.repo.common.security.CredentialsStorageManager;
import com.evolveum.midpoint.repo.common.security.SecurityPolicyFinder;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttribute;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.RawRepoShadow;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Computes a shadow to be stored in the repository.
 *
 * @see ShadowDeltaComputerAbsolute
 * @see ShadowDeltaComputerRelative
 */
@Component
class ShadowObjectComputer {

    @Autowired private Clock clock;
    @Autowired CredentialsStorageManager credentialsStorageManager;
    @Autowired SecurityPolicyFinder securityPolicyFinder;

    /**
     * Creates a copy of a resource object (or other) shadow that is suitable for repository storage.
     */
    @NotNull RawRepoShadow createShadowForRepoStorage(
            ProvisioningContext ctx, AbstractShadow resourceObjectOrOtherShadow, OperationResult result)
            throws SchemaException, EncryptionException {

        resourceObjectOrOtherShadow.checkConsistence();

        // We start from a clone of the resource object shadow.
        // An alternative would be to start with a clean shadow and fill-in the data from resource object.
        // But we could easily miss something. So let's do it this way.
        var repoShadowBean = resourceObjectOrOtherShadow.getBean().clone();
        var repoShadowObject = repoShadowBean.asPrismObject();

        // Associations are not stored in the repository at all. Only underlying reference attributes are.
        repoShadowObject.removeContainer(ShadowType.F_ASSOCIATIONS);

        // Attributes will be created anew, not as RAC but as PrismContainer. This is because normalization-aware
        // attributes are no longer ResourceAttribute instances. We delete them also because the application of the
        // raw PCD definition (below) would fail on a RAC.
        repoShadowObject.removeContainer(ShadowType.F_ATTRIBUTES);

        // For similar reason, we remove any traces of RACD from the definition.
        var standardDefinition = PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
        repoShadowObject.applyDefinition(standardDefinition);

        // For resource objects, this information is obviously limited, as there are no pending operations known.
        // But the exists and dead flags can tell us something.
        var shadowLifecycleState = ctx.determineShadowState(repoShadowBean);

        Object primaryIdentifierValue =
                ShadowManagerMiscUtil.determinePrimaryIdentifierValue(resourceObjectOrOtherShadow, shadowLifecycleState);
        repoShadowBean.setPrimaryIdentifierValue(primaryIdentifierValue != null ? primaryIdentifierValue.toString() : null);

        var objectDef = resourceObjectOrOtherShadow.getObjectDefinition();
        for (var attribute : resourceObjectOrOtherShadow.getAttributes()) {
            if (attribute instanceof ShadowSimpleAttribute<?> simpleAttribute) {
                var attrDef = simpleAttribute.getDefinitionRequired();
                if (shouldStoreSimpleAttributeInShadow(objectDef, attrDef)) {
                    var repoAttrDef = attrDef.toNormalizationAware();
                    var repoAttr = repoAttrDef.adoptRealValuesAndInstantiate(simpleAttribute.getRealValues());
                    repoShadowObject
                            .findOrCreateContainer(ShadowType.F_ATTRIBUTES)
                            .add(repoAttr);
                }
            } else if (attribute instanceof ShadowReferenceAttribute referenceAttribute) {
                var attrDef = referenceAttribute.getDefinitionRequired();
                if (shouldStoreReferenceAttributeInShadow(objectDef, attrDef)) {
                    var repoAttrDef = ShadowComputerUtil.createRepoRefAttrDef(attrDef);
                    var repoAttr = repoAttrDef.instantiate();
                    for (var refAttrValue : referenceAttribute.getReferenceValues()) {
                        ObjectReferenceType inRepoFormat = ShadowComputerUtil.toRepoFormat(ctx, refAttrValue);
                        if (inRepoFormat != null) {
                            repoAttr.addIgnoringEquivalents(inRepoFormat.asReferenceValue());
                        }
                    }
                    // Only add reference attributes if values are present.
                    if (repoAttr.hasAnyValue()) {
                        repoShadowObject
                                .findOrCreateContainer(ShadowType.F_REFERENCE_ATTRIBUTES)
                                .add(repoAttr);
                    }
                }
            } else {
                throw new AssertionError(attribute);
            }
        }
        var objectDefinition = ctx.getObjectDefinitionRequired();

        if (objectDefinition.isCachingEnabled()) {
            CachingMetadataType cachingMetadata = new CachingMetadataType();
            cachingMetadata.setRetrievalTimestamp(clock.currentTimeXMLGregorianCalendar());
            repoShadowBean.setCachingMetadata(cachingMetadata);
        } else {
            repoShadowBean.setCachingMetadata(null);
        }

        ShadowComputerUtil.cleanupShadowActivation(ctx, repoShadowBean.getActivation());

        // Store only password meta-data in repo - unless there is explicit caching
        CredentialsType credentials = repoShadowBean.getCredentials();
        if (credentials != null) {
            PasswordType password = credentials.getPassword();
            if (password != null) {
                preparePasswordForStorage(ctx, password, result);
                addPasswordMetadata(password, clock.currentTimeXMLGregorianCalendar(), ctx.getTask().getOwnerRef());
            }
            // TODO: other credential types - later
        }

        if (repoShadowBean.getName() == null) {
            PolyString name = MiscUtil.requireNonNull(
                    resourceObjectOrOtherShadow.determineShadowName(),
                    () -> "Cannot determine the shadow name for " + resourceObjectOrOtherShadow);
            repoShadowBean.setName(toPolyStringType(name));
        }

        repoShadowBean.setProtectedObject(null);
        repoShadowBean.setEffectiveOperationPolicy(null);

        MetadataUtil.addCreationMetadata(repoShadowBean);

        // the resource ref and object class are always there

        return RawRepoShadow.of(repoShadowBean);
    }

    /**
     * Removes, hashes, or keeps the password value property, depending on the circumstances.
     *
     * See https://docs.evolveum.com/midpoint/devel/design/password-caching-4.9.1/..
     */
    private void preparePasswordForStorage(ProvisioningContext ctx, @NotNull PasswordType password, OperationResult result)
            throws SchemaException, EncryptionException {

        //noinspection unchecked
        PrismProperty<ProtectedStringType> passwordProperty = password.asPrismContainerValue().findProperty(PasswordType.F_VALUE);
        if (passwordProperty == null) {
            return; // nothing to hash or to remove from the shadow
        }

        var definition = ctx.getObjectDefinitionRequired();

        // Caching disabled - we must remove the password value property (even if it is only of incomplete=true type)
        if (!definition.areCredentialsCached()) {
            removePasswordValueProperty(password);
            return;
        }

        // Caching enabled

        var passwordRealValue = passwordProperty.getRealValue();
        var passwordValueIncomplete = passwordProperty.isIncomplete();

        if (passwordRealValue == null) {
            if (passwordValueIncomplete) {
                // Nothing to do; we'll save the property as is
            } else {
                // No value and not incomplete? Looks like an empty property. Just remove it.
                removePasswordValueProperty(password);
            }
        } else if (passwordRealValue.isHashed()) {
            // The password is hashed. Currently, there's probably only one way how this could happen: the "asIs" password
            // mapping took the hashed focus password, and copied the value here. It won't be propagated to the resource
            // (as the plaintext value is unknown, and the connectors do not understand midPoint hashed values), so we simply
            // should not store it in the cache.
            //
            // For the opposite direction (discovered shadows on the resource), there is currently no chance of getting
            // hashed passwords here.
            //
            // See similar code in ShadowDeltaComputerRelative#addPasswordValueDelta.
            //
            // TODO maybe we should not allow obtaining hashed passwords (from clients) in the shadows at all?
            // TODO maybe this should be dealt with in CredentialsStorageManager? Think again about it.
            assert !passwordValueIncomplete : "Incomplete password value with a real value?";
            removePasswordValueProperty(password);
        } else {
            var credentialsPolicy = securityPolicyFinder.locateResourceObjectCredentialsPolicy(definition, result);
            var legacyCaching = definition.areCredentialsCachedLegacy();
            credentialsStorageManager.transformShadowPasswordWithRealValue(credentialsPolicy, legacyCaching, passwordProperty);
        }
    }
}
