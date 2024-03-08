/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.util;

import java.util.Collection;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;

/** Determines {@link AttributesToReturn} in a given context. */
public class AttributesToReturnProvider {

    @NotNull private final ResourceType resource;

    @NotNull private final ResourceObjectDefinition objectDefinition;

    private final Collection<SelectorOptions<GetOperationOptions>> getOperationOptions;

    public AttributesToReturnProvider(
            @NotNull ResourceType resource,
            @NotNull ResourceObjectDefinition objectDefinition,
            Collection<SelectorOptions<GetOperationOptions>> getOperationOptions) {
        this.resource = resource;
        this.objectDefinition = objectDefinition;
        this.getOperationOptions = getOperationOptions;
    }

    public @Nullable AttributesToReturn createAttributesToReturn() {

        boolean apply = false;
        AttributesToReturn attributesToReturn = new AttributesToReturn();

        // Attributes

        boolean hasMinimal = hasMinimalFetchStrategy();
        attributesToReturn.setReturnDefaultAttributes(!hasMinimal);

        Collection<ResourceAttributeDefinition<?>> explicit = getExplicitlyFetchedAttributes(!hasMinimal);

        if (!explicit.isEmpty()) {
            attributesToReturn.setAttributesToReturn(explicit);
            apply = true;
        }

        // Password
        CredentialsCapabilityType credentialsCapability =
                ResourceTypeUtil.getEnabledCapability(resource, CredentialsCapabilityType.class);
        if (credentialsCapability != null) {
            if (isFetchingNotDisabled(SchemaConstants.PATH_PASSWORD_VALUE)) {
                attributesToReturn.setReturnPasswordExplicit(true);
                apply = true;
            } else {
                if (!CapabilityUtil.isPasswordReturnedByDefault(credentialsCapability)) {
                    // The resource is capable of returning password but it does not do it by default.
                    AttributeFetchStrategyType passwordFetchStrategy = objectDefinition.getPasswordFetchStrategy();
                    if (passwordFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
                        attributesToReturn.setReturnPasswordExplicit(true);
                        apply = true;
                    }
                }
            }
        }

        // Activation
        ActivationCapabilityType activationCapability =
                ResourceTypeUtil.getEnabledCapability(resource, ActivationCapabilityType.class);
        if (activationCapability != null) {
            if (CapabilityUtil.isCapabilityEnabled(activationCapability.getStatus())) {
                if (!CapabilityUtil.isActivationStatusReturnedByDefault(activationCapability)) {
                    // The resource is capable of returning enable flag but it does not do it by default.
                    if (isFetchingNotDisabled(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS)) {
                        attributesToReturn.setReturnAdministrativeStatusExplicit(true);
                        apply = true;
                    } else {
                        AttributeFetchStrategyType administrativeStatusFetchStrategy =
                                objectDefinition.getActivationFetchStrategy(ActivationType.F_ADMINISTRATIVE_STATUS);
                        if (administrativeStatusFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
                            attributesToReturn.setReturnAdministrativeStatusExplicit(true);
                            apply = true;
                        }
                    }
                }
            }
            if (CapabilityUtil.isCapabilityEnabled(activationCapability.getValidFrom())) {
                if (!CapabilityUtil.isActivationValidFromReturnedByDefault(activationCapability)) {
                    if (isFetchingNotDisabled(SchemaConstants.PATH_ACTIVATION_VALID_FROM)) {
                        attributesToReturn.setReturnValidFromExplicit(true);
                        apply = true;
                    } else {
                        AttributeFetchStrategyType administrativeStatusFetchStrategy = objectDefinition
                                .getActivationFetchStrategy(ActivationType.F_VALID_FROM);
                        if (administrativeStatusFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
                            attributesToReturn.setReturnValidFromExplicit(true);
                            apply = true;
                        }
                    }
                }
            }
            if (CapabilityUtil.isCapabilityEnabled(activationCapability.getValidTo())) {
                if (!CapabilityUtil.isActivationValidToReturnedByDefault(activationCapability)) {
                    if (isFetchingNotDisabled(SchemaConstants.PATH_ACTIVATION_VALID_TO)) {
                        attributesToReturn.setReturnValidToExplicit(true);
                        apply = true;
                    } else {
                        AttributeFetchStrategyType administrativeStatusFetchStrategy = objectDefinition
                                .getActivationFetchStrategy(ActivationType.F_VALID_TO);
                        if (administrativeStatusFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
                            attributesToReturn.setReturnValidToExplicit(true);
                            apply = true;
                        }
                    }
                }
            }
            if (CapabilityUtil.isCapabilityEnabled(activationCapability.getLockoutStatus())) {
                if (!CapabilityUtil.isActivationLockoutStatusReturnedByDefault(activationCapability)) {
                    // The resource is capable of returning lockout flag but it does not do it by default.
                    if (isFetchingNotDisabled(SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS)) {
                        attributesToReturn.setReturnAdministrativeStatusExplicit(true);
                        apply = true;
                    } else {
                        AttributeFetchStrategyType statusFetchStrategy = objectDefinition
                                .getActivationFetchStrategy(ActivationType.F_LOCKOUT_STATUS);
                        if (statusFetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
                            attributesToReturn.setReturnLockoutStatusExplicit(true);
                            apply = true;
                        }
                    }
                }
            }
        }

        if (apply) {
            return attributesToReturn;
        } else {
            return null;
        }
    }

    private @NotNull Collection<ResourceAttributeDefinition<?>> getExplicitlyFetchedAttributes(boolean returnsDefaultAttributes) {
        return objectDefinition.getAttributeDefinitions().stream()
                .filter(attributeDefinition -> shouldExplicitlyFetch(attributeDefinition, returnsDefaultAttributes))
                .collect(Collectors.toList());
    }

    private boolean hasMinimalFetchStrategy() {
        return objectDefinition.getAttributeDefinitions().stream()
                .anyMatch(def -> def.getFetchStrategy() == AttributeFetchStrategyType.MINIMAL);
    }

    private boolean shouldExplicitlyFetch(ResourceAttributeDefinition<?> attributeDefinition, boolean returnsDefaultAttributes) {
        AttributeFetchStrategyType fetchStrategy = attributeDefinition.getFetchStrategy();
        if (fetchStrategy == AttributeFetchStrategyType.EXPLICIT) {
            return true;
        } else if (returnsDefaultAttributes) {
            // Normal attributes are returned by default. So there's no need to explicitly request this attribute.
            return false;
        } else if (isFetchingRequested(attributeDefinition)) {
            // Client wants this attribute.
            return true;
        } else {
            // If the fetch strategy is MINIMAL, we want to skip. Otherwise, request it.
            return fetchStrategy != AttributeFetchStrategyType.MINIMAL;
        }
    }

    private boolean isFetchingRequested(ResourceAttributeDefinition<?> attributeDefinition) {
        // See MID-5838
        return isFetchingRequested(attributeDefinition.getStandardPath())
                || isFetchingRequested(attributeDefinition.getItemName());
    }

    private boolean isFetchingRequested(ItemPath path) {
        return SelectorOptions.hasToIncludePath(path, getOperationOptions, false);
    }

    private boolean isFetchingNotDisabled(ItemPath path) {
        return SelectorOptions.hasToIncludePath(path, getOperationOptions, true);
    }
}
