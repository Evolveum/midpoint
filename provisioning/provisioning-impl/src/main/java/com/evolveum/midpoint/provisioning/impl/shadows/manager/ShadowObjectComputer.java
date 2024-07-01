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
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAttributesContainer;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
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
     * Create a copy of a resource object (or another shadow) that is suitable for repository storage.
     *
     * @see ShadowDeltaComputerAbsolute
     */
    @NotNull RawRepoShadow createShadowForRepoStorage(ProvisioningContext ctx, AbstractShadow resourceObjectOrShadow)
            throws SchemaException, EncryptionException {

        resourceObjectOrShadow.checkConsistence();

        ResourceObjectDefinition objectDef = resourceObjectOrShadow.getObjectDefinition();
        ShadowAttributesContainer originalAttributesContainer = resourceObjectOrShadow.getAttributesContainer();

        // An alternative would be to start with a clean shadow and fill-in the data from resource object.
        // But we could easily miss something. So let's clone the shadow instead.
        ShadowType repoShadowBean = resourceObjectOrShadow.getBean().clone();

        // Attributes will be created anew, not as RAC but as PrismContainer. This is because normalization-aware
        // attributes are no longer ResourceAttribute instances. We delete them also because the application of the
        // raw PCD definition (below) would fail on a RAC. The same reasons for associations.
        repoShadowBean.asPrismObject().removeContainer(ShadowType.F_ATTRIBUTES);
        repoShadowBean.asPrismObject().removeContainer(ShadowType.F_ASSOCIATIONS);

        // For similar reason, we remove any traces of RACD from the definition.
        PrismObjectDefinition<ShadowType> standardDefinition =
                PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
        repoShadowBean.asPrismObject().applyDefinition(standardDefinition);

        // For resource objects, this information is obviously limited, as there are no pending operations known.
        // But the exists and dead flags can tell us something.
        var shadowLifecycleState = ctx.determineShadowState(repoShadowBean);

        Object primaryIdentifierValue =
                ShadowManagerMiscUtil.determinePrimaryIdentifierValue(resourceObjectOrShadow, shadowLifecycleState);
        repoShadowBean.setPrimaryIdentifierValue(primaryIdentifierValue != null ? primaryIdentifierValue.toString() : null);

        var repoAttributesContainer = repoShadowBean.asPrismObject().findOrCreateContainer(ShadowType.F_ATTRIBUTES);
        for (ShadowSimpleAttribute<?> attribute : originalAttributesContainer.getSimpleAttributes()) {
            // TODO or should we use attribute.getDefinition()?
            var attrDef = objectDef.findSimpleAttributeDefinitionRequired(attribute.getElementName());
            if (ctx.shouldStoreAttributeInShadow(objectDef, attrDef)) {
                var repoAttrDef = attrDef.toNormalizationAware();
                var repoAttr = repoAttrDef.adoptRealValuesAndInstantiate(attribute.getRealValues());
                repoAttributesContainer.add(repoAttr);
            }
        }

        if (ctx.isCachingEnabled()) {
            CachingMetadataType cachingMetadata = new CachingMetadataType();
            cachingMetadata.setRetrievalTimestamp(clock.currentTimeXMLGregorianCalendar());
            repoShadowBean.setCachingMetadata(cachingMetadata);
        } else {
            repoShadowBean.setCachingMetadata(null);
            ProvisioningUtil.cleanupShadowActivation(repoShadowBean); // TODO deal with this more precisely
        }

        // Store only password meta-data in repo - unless there is explicit caching
        CredentialsType credentials = repoShadowBean.getCredentials();
        if (credentials != null) {
            PasswordType password = credentials.getPassword();
            if (password != null) {
                preparePasswordForStorage(password, ctx);
                ObjectReferenceType owner = ctx.getTask().getOwnerRef();
                ProvisioningUtil.addPasswordMetadata(password, clock.currentTimeXMLGregorianCalendar(), owner);
            }
            // TODO: other credential types - later
        }

        if (repoShadowBean.getName() == null) {
            PolyString name = MiscUtil.requireNonNull(
                    resourceObjectOrShadow.determineShadowName(),
                    () -> "Cannot determine the shadow name for " + resourceObjectOrShadow);
            repoShadowBean.setName(toPolyStringType(name));
        }

        repoShadowBean.setProtectedObject(null);
        repoShadowBean.setEffectiveOperationPolicy(null);

        MetadataUtil.addCreationMetadata(repoShadowBean);

        // the resource ref and object class are always there

        return RawRepoShadow.of(repoShadowBean);
    }

    private void preparePasswordForStorage(PasswordType password, ProvisioningContext ctx)
            throws SchemaException, EncryptionException {
        ProtectedStringType passwordValue = password.getValue();
        if (passwordValue == null) {
            return;
        }
        CachingStrategyType cachingStrategy = ctx.getPasswordCachingStrategy();
        if (cachingStrategy != null && cachingStrategy != CachingStrategyType.NONE) {
            if (!passwordValue.isHashed()) {
                protector.hash(passwordValue);
            }
        } else {
            ProvisioningUtil.cleanupShadowPassword(password);
        }
    }
}
