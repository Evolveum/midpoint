/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.RawRepoShadow;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.evolveum.midpoint.prism.polystring.PolyString.toPolyStringType;
import static com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowComputerUtil.*;

/**
 * Computes a shadow to be stored in the repository.
 *
 * @see ShadowDeltaComputerAbsolute
 * @see ShadowDeltaComputerRelative
 */
@Component
class ShadowObjectComputer {

    @Autowired private Clock clock;
    @Autowired private Protector protector;

    /**
     * Creates a copy of a resource object (or other) shadow that is suitable for repository storage.
     */
    @NotNull RawRepoShadow createShadowForRepoStorage(
            ProvisioningContext ctx, AbstractShadow resourceObjectOrOtherShadow)
            throws SchemaException, EncryptionException {

        resourceObjectOrOtherShadow.checkConsistence();

        // An alternative would be to start with a clean shadow and fill-in the data from resource object.
        // But we could easily miss something. So let's clone the shadow instead.
        var repoShadowBean = resourceObjectOrOtherShadow.getBean().clone();
        var repoShadowObject = repoShadowBean.asPrismObject();

        // Associations are not stored in the repository at all.
        repoShadowObject.removeContainer(ShadowType.F_ASSOCIATIONS);

        // Attributes will be created anew, not as RAC but as PrismContainer. This is because normalization-aware
        // attributes are no longer ResourceAttribute instances. We delete them also because the application of the
        // raw PCD definition (below) would fail on a RAC.
        repoShadowObject.removeContainer(ShadowType.F_ATTRIBUTES);

        // For similar reason, we remove any traces of RACD from the definition.
        PrismObjectDefinition<ShadowType> standardDefinition =
                PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
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
                    repoShadowObject
                            .findOrCreateContainer(ShadowType.F_REFERENCE_ATTRIBUTES)
                            .add(repoAttr);
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
                preparePasswordForStorage(ctx, password);
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

    private void preparePasswordForStorage(ProvisioningContext ctx, PasswordType password)
            throws SchemaException, EncryptionException {
        ProtectedStringType passwordValue = password.getValue();
        if (passwordValue == null) {
            return;
        }
        if (ctx.getObjectDefinitionRequired().areCredentialsCached()) {
            if (!passwordValue.isHashed()) {
                protector.hash(passwordValue);
            }
        } else {
            cleanupShadowPassword(password);
        }
    }
}
