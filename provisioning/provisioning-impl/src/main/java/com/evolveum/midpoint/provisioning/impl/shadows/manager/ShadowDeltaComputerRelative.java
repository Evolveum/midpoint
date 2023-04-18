/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.common.ObjectOperationPolicyHelper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsNormalizationUtil.normalizeAttributeDelta;
import static com.evolveum.midpoint.provisioning.util.ProvisioningUtil.shouldStoreAttributeInShadow;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * Computes deltas to be applied to repository shadows.
 *
 * Unlike {@link ShadowDeltaComputerAbsolute}, this class starts with a known modifications to be applied to the resource
 * object or the repository shadow itself ({@link #allModifications}). It derives modifications relevant to the repository
 * object - for the most time, ignoring irrelevant ones (e.g. with the path of `attributes/xyz`). But also derives new ones,
 * like changing shadow name when the naming attribute changes; or changing the primary identifier value.
 *
 * @see ShadowDeltaComputerAbsolute
 */
class ShadowDeltaComputerRelative {

    private final ProvisioningContext ctx;
    private final Collection<? extends ItemDelta<?, ?>> allModifications;
    private final Protector protector;

    // Needed only for computation of effectiveMarkRefs
    private ShadowType repoShadow;

    ShadowDeltaComputerRelative(
            ProvisioningContext ctx, ShadowType repoShadow, Collection<? extends ItemDelta<?, ?>> allModifications, Protector protector) {
        this.ctx = ctx;
        this.allModifications = allModifications;
        this.protector = protector;
        this.repoShadow = repoShadow;
    }

    Collection<ItemDelta<?, ?>> computeShadowModifications() throws SchemaException, ConfigurationException {
        ResourceObjectDefinition objectDefinition = ctx.getObjectDefinitionRequired(); // If type is not present, OC def is fine
        CachingStrategyType cachingStrategy = ctx.getCachingStrategy();

        // The former of these two (explicit name change) takes precedence over the latter.
        ItemDelta<?, ?> explicitNameChange = null; // Shadow name change requested explicitly by the client.
        ItemDelta<?, ?> attributeBasedNameChange = null; // Shadow name change as determined by looking at attributes.

        Collection<ItemDelta<?, ?>> resultingRepoModifications = new ArrayList<>();

        for (ItemDelta<?, ?> modification : allModifications) {
            if (ShadowType.F_ATTRIBUTES.equivalent(modification.getParentPath())) {
                QName attrName = modification.getElementName();
                ItemDelta<?, ?> normalizedModification = normalizeAttributeDelta(modification, objectDefinition);
                if (isNamingAttribute(attrName, objectDefinition)) {
                    // Naming attribute is changed -> the shadow name should change as well.
                    // TODO: change this to displayName attribute later
                    String newName = getNewStringValue(modification); // Or should we take normalized modification?
                    attributeBasedNameChange =
                            PrismContext.get().deltaFor(ShadowType.class)
                                    .item(ShadowType.F_NAME)
                                    .replace(new PolyString(newName))
                                    .asItemDelta();
                }
                if (objectDefinition.isPrimaryIdentifier(attrName)) {
                    // Change of primary identifier induces a modification on $shadow/primaryIdentifierValue.
                    // FIXME this should not be executed for dead shadows!
                    //  (or going-to-be dead ones? -> to be reviewed)
                    String newValueNormalized = getNewStringValue(normalizedModification);
                    resultingRepoModifications.add(
                            PrismContext.get().deltaFor(ShadowType.class)
                                    .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE)
                                    .replace(newValueNormalized)
                                    .asItemDelta());
                }
                if (shouldStoreAttributeInShadow(objectDefinition, attrName, cachingStrategy)) {
                    resultingRepoModifications.add(normalizedModification);
                }
            } else if (ShadowType.F_ACTIVATION.equivalent(modification.getParentPath())) {
                if (ProvisioningUtil.shouldStoreActivationItemInShadow(modification.getElementName(), cachingStrategy)) {
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
                explicitNameChange = modification;
            } else if (ShadowType.F_POLICY_STATEMENT.equivalent(modification.getPath())) {
                resultingRepoModifications.add(modification);
                ItemDelta<?, ?> effectiveMarkDelta = computeEffectiveMarkDelta(modification);
                if (effectiveMarkDelta != null) {
                    resultingRepoModifications.add(effectiveMarkDelta);
                }
            } else {
                resultingRepoModifications.add(modification);
            }
        }

        if (explicitNameChange != null) {
            resultingRepoModifications.add(explicitNameChange);
        } else if (attributeBasedNameChange != null) {
            resultingRepoModifications.add(attributeBasedNameChange);
        }

        return resultingRepoModifications;
    }

    private ItemDelta<?, ?> computeEffectiveMarkDelta(ItemDelta<?, ?> modification) throws SchemaException {
        return ObjectOperationPolicyHelper.get().computeEffectiveMarkDelta(repoShadow, modification);
    }

    // Quite a hack.
    private String getNewStringValue(ItemDelta<?, ?> modification) {
        Collection<?> valuesToReplace = modification.getValuesToReplace();
        if (valuesToReplace != null && !valuesToReplace.isEmpty()) {
            return ((PrismPropertyValue<?>) valuesToReplace.iterator().next()).getValue().toString();
        }
        Collection<?> valuesToAdd = modification.getValuesToAdd();
        if (valuesToAdd != null && !valuesToAdd.isEmpty()) {
            return ((PrismPropertyValue<?>) valuesToAdd.iterator().next()).getValue().toString();
        }
        return null;
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

    private void addPasswordDelta(Collection<ItemDelta<?, ?>> repoChanges, ItemDelta<?, ?> requestedPasswordDelta,
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
        repoChanges.add(requestedPasswordDelta);
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
