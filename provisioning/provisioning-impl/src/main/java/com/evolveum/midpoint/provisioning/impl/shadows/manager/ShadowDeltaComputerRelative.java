/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.Collection;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.provisioning.impl.RepoShadowModifications;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.common.ObjectOperationPolicyHelper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Computes deltas to be applied to repository shadows.
 *
 * Unlike {@link ShadowDeltaComputerAbsolute}, this class starts with a known modifications to be applied to the resource
 * object or the repository shadow itself ({@link #allModifications}). It derives modifications relevant to the repository
 * object - for the most time, ignoring irrelevant ones (e.g. with the path of `attributes/xyz` for uncached attributes).
 * But also derives new ones, like changing shadow name when the naming attribute changes; or changing
 * the primary identifier value.
 *
 * @see ShadowDeltaComputerAbsolute
 */
class ShadowDeltaComputerRelative {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowDeltaComputerRelative.class);

    private final ProvisioningContext ctx;
    private final Collection<? extends ItemDelta<?, ?>> allModifications;
    private final Protector protector;

    // Needed only for computation of effectiveMarkRefs
    private final RepoShadow repoShadow;

    ShadowDeltaComputerRelative(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            @NotNull Collection<? extends ItemDelta<?, ?>> allModifications,
            @NotNull Protector protector) {
        this.ctx = ctx;
        this.allModifications = allModifications;
        this.protector = protector;
        this.repoShadow = repoShadow;
    }

    RepoShadowModifications computeShadowModifications() throws SchemaException {
        ResourceObjectDefinition objectDefinition = ctx.getObjectDefinitionRequired(); // If type is not present, OC def is fine
        boolean cachingEnabled = ctx.isCachingEnabled(); // FIXME partial caching?

        // The former of these two (explicit name change) takes precedence over the latter.
        ItemDelta<?, ?> explicitNameMod = null; // Shadow name modification requested explicitly by the client.
        ItemDelta<?, ?> attributeBasedNameMod = null; // Shadow name modification as determined by looking at attributes.

        RepoShadowModifications resultingRepoModifications = new RepoShadowModifications();

        Collection<? extends QName> associationValueAttributes = objectDefinition.getAssociationValueAttributes();

        for (ItemDelta<?, ?> modification : allModifications) {
            if (ShadowType.F_ATTRIBUTES.equivalent(modification.getParentPath())) {
                QName attrName = modification.getElementName();
                var attrDef = objectDefinition.findAttributeDefinitionRequired(attrName);
                if (isNamingAttribute(attrName, objectDefinition)) {
                    // Naming attribute is changed -> the shadow name should change as well.
                    // TODO: change this to displayName attribute later
                    attributeBasedNameMod = nameModFromAttributeMod(modification, attributeBasedNameMod);
                }
                if (objectDefinition.isPrimaryIdentifier(attrName)) {
                    // Change of primary identifier induces a modification on $shadow/primaryIdentifierValue.
                    // FIXME this should not be executed for dead shadows!
                    //  (or going-to-be dead ones? -> to be reviewed)
                    resultingRepoModifications.add(
                            primaryIdentifierValueModFromAttributeMod(modification));
                }
                if (ctx.shouldStoreAttributeInShadow(objectDefinition, attrDef, associationValueAttributes)) {
                    resultingRepoModifications.add(modification, attrDef);
                }
            } else if (ShadowType.F_ACTIVATION.equivalent(modification.getParentPath())) {
                if (ProvisioningUtil.shouldStoreActivationItemInShadow(modification.getElementName(), cachingEnabled)) {
                    resultingRepoModifications.add(modification);
                }
            } else if (ShadowType.F_ACTIVATION.equivalent(modification.getPath())) {// should not occur, but for completeness...
                //noinspection unchecked
                ContainerDelta<ActivationType> activationModification = (ContainerDelta<ActivationType>) modification;
                for (PrismContainerValue<ActivationType> value : emptyIfNull(activationModification.getValuesToAdd())) {
                    ProvisioningUtil.cleanupShadowActivation(value.asContainerable());
                }
                for (PrismContainerValue<ActivationType> value : emptyIfNull(activationModification.getValuesToReplace())) {
                    ProvisioningUtil.cleanupShadowActivation(value.asContainerable());
                }
                resultingRepoModifications.add(activationModification);
            } else if (SchemaConstants.PATH_PASSWORD.equivalent(modification.getParentPath())) {
                addPasswordDelta(resultingRepoModifications, modification, objectDefinition);
            } else if (ShadowType.F_NAME.equivalent(modification.getPath())) {
                explicitNameMod = modification;
            } else if (ShadowType.F_POLICY_STATEMENT.equivalent(modification.getPath())) {
                resultingRepoModifications.add(modification);
                ItemDelta<?, ?> effectiveMarkDelta = computeEffectiveMarkDelta(modification);
                if (effectiveMarkDelta != null) {
                    resultingRepoModifications.add(effectiveMarkDelta);
                }
            } else if (modification.getPath().startsWith(ShadowType.F_ASSOCIATION)) {
                // associations are currently not stored in the shadow
            } else {
                resultingRepoModifications.add(modification);
            }
        }

        if (explicitNameMod != null) {
            resultingRepoModifications.add(explicitNameMod);
        } else if (attributeBasedNameMod != null) {
            resultingRepoModifications.add(attributeBasedNameMod);
        }

        return resultingRepoModifications;
    }

    private ItemDelta<?, ?> nameModFromAttributeMod(
            ItemDelta<?, ?> attributeMod, ItemDelta<?, ?> originalNameMod)
            throws SchemaException {

        Collection<? extends PrismValue> newValues = attributeMod.getNewValues();

        if (newValues.isEmpty()) {
            // Strange but not impossible. So we do not throw an exception here.
            LOGGER.warn("Naming attribute value removal? Object: {}, modifications:\n{}",
                    repoShadow, DebugUtil.debugDump(allModifications, 1));
            return originalNameMod; // nothing to do
        } else if (newValues.size() > 1) {
            LOGGER.warn("Adding more values for a naming attribute? Using the first one. Object: {}, modifications:\n{}",
                    repoShadow, DebugUtil.debugDump(allModifications, 1));
        }
        PrismValue newValue = newValues.iterator().next();
        Object newRealValue = MiscUtil.stateNonNull(newValue.getRealValue(), "No real value in %s", attributeMod);
        String newStringOrigValue;
        if (newRealValue instanceof PolyString polyString) {
            newStringOrigValue = polyString.getOrig();
        } else {
            newStringOrigValue = newRealValue.toString();
        }

        return PrismContext.get().deltaFor(ShadowType.class)
                .item(ShadowType.F_NAME)
                .replace(PolyString.fromOrig(newStringOrigValue))
                .asItemDelta();
    }

    private @NotNull ItemDelta<?, ?> primaryIdentifierValueModFromAttributeMod(ItemDelta<?, ?> attributeMod)
            throws SchemaException {
        Collection<? extends PrismValue> newValues = attributeMod.getNewValues();
        PrismValue newValue;
        if (newValues.isEmpty()) {
            throw new SchemaException("Primary identifier value removal: %s for %s".formatted(attributeMod, repoShadow));
        } else if (newValues.size() > 1) {
            throw new SchemaException(
                    "Adding more values for a primary identifier attribute: %s for %s".formatted(newValues, repoShadow));
        } else {
            newValue = newValues.iterator().next();
        }
        Object newRealValue = MiscUtil.stateNonNull(newValue.getRealValue(), "No real value in %s", attributeMod);
        String newStringNormValue;
        if (newRealValue instanceof PolyString polyString) {
            newStringNormValue = polyString.getNorm();
        } else {
            newStringNormValue = newRealValue.toString();
        }
        return PrismContext.get().deltaFor(ShadowType.class)
                .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE)
                .replace(newStringNormValue)
                .asItemDelta();
    }

    private ItemDelta<?, ?> computeEffectiveMarkDelta(ItemDelta<?, ?> modification) throws SchemaException {
        return ObjectOperationPolicyHelper.get().computeEffectiveMarkDelta(repoShadow.getBean(), modification);
    }

    /**
     * See also {@link ShadowUtil#determineShadowStringName(ShadowType)}.
     * Note that these implementations are not quite in sync - e.g., regarding handling of `icfs:name`.
     */
    private static boolean isNamingAttribute(QName attrName, ResourceObjectDefinition objectDefinition) {
        QName namingAttributeName = objectDefinition.getNamingAttributeName();
        if (namingAttributeName != null) {
            // This may provide ambiguous results e.g. when having both ri:name and icfs:name and delta contains attributes/name
            // modification. But in such a case, the whole delta is ambiguous.
            return QNameUtil.match(namingAttributeName, attrName);
        }

        return objectDefinition.isSecondaryIdentifier(attrName)
                || (objectDefinition.getAllIdentifiers().size() == 1 && objectDefinition.isPrimaryIdentifier(attrName));
    }

    private void addPasswordDelta(
            RepoShadowModifications repoModifications,
            ItemDelta<?, ?> requestedPasswordDelta,
            ResourceObjectDefinition objectDefinition) throws SchemaException {
        if (!(requestedPasswordDelta.getPath().equivalent(SchemaConstants.PATH_PASSWORD_VALUE))) {
            return;
        }
        CachingStrategyType cachingStrategy = ProvisioningUtil.getPasswordCachingStrategy(objectDefinition);
        if (cachingStrategy == null || cachingStrategy == CachingStrategyType.NONE) {
            return;
        }
        //noinspection unchecked
        PropertyDelta<ProtectedStringType> passwordValueDelta = (PropertyDelta<ProtectedStringType>) requestedPasswordDelta;
        hashValues(passwordValueDelta.getValuesToAdd());
        hashValues(passwordValueDelta.getValuesToReplace());
        repoModifications.add(requestedPasswordDelta);
    }

    private void hashValues(Collection<PrismPropertyValue<ProtectedStringType>> propertyValues) throws SchemaException {
        if (propertyValues == null) {
            return;
        }
        for (PrismPropertyValue<ProtectedStringType> propertyValue : propertyValues) {
            ProtectedStringType psVal = propertyValue.getValue();
            if (psVal == null) {
                return;
            }
            if (psVal.isHashed()) {
                return;
            }
            try {
                protector.hash(psVal);
            } catch (EncryptionException e) {
                throw new SchemaException("Cannot hash value", e);
            }
        }
    }
}
