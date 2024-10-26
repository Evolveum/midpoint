/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.util;

import java.util.Collection;
import java.util.stream.Stream;

import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.util.logging.Trace;

import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowBehaviorType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.BehaviorCapabilityType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.ucf.api.ShadowItemsToReturn;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeFetchStrategyType.*;

/** Determines {@link ShadowItemsToReturn} in a given context. */
public class ShadowItemsToReturnProvider {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowItemsToReturnProvider.class);

    /** We need this to get the capabilities. */
    @NotNull private final ResourceType resource;

    /** Definition of object for which we are to determine the "attributes to return". */
    @NotNull private final ResourceObjectDefinition objectDefinition;

    /** What the client requested. */
    private final Collection<SelectorOptions<GetOperationOptions>> getOperationOptions;

    /** The resulting value that is gradually built. */
    private final ShadowItemsToReturn shadowItemsToReturn = new ShadowItemsToReturn();

    public ShadowItemsToReturnProvider(
            @NotNull ResourceType resource,
            @NotNull ResourceObjectDefinition objectDefinition,
            Collection<SelectorOptions<GetOperationOptions>> getOperationOptions) {
        this.resource = resource;
        this.objectDefinition = objectDefinition;
        this.getOperationOptions = getOperationOptions;
    }

    public @Nullable ShadowItemsToReturn createAttributesToReturn() {

        // Regular shadow items (attributes and associations)

        // Minimal fetch strategy means that we want to avoid fetching this attribute (and we presume it's fetched by default).
        // So if something like that is present, we have to forbid returning default attributes, and provide them one by one.
        // TODO shouldn't we check it is really marked as "returned by default"?
        if (hasItemWithMinimalFetchStrategy()) {
            LOGGER.trace("We have a shadow item with 'minimal' fetch strategy -> suppressing returning default attributes");
            shadowItemsToReturn.setReturnDefaultAttributes(false);
        }

        Collection<? extends ShadowAttributeDefinition<?, ?, ?, ?>> explicit = getItemsToFetchExplicitly();
        if (!explicit.isEmpty()) {
            shadowItemsToReturn.setItemsToReturn(explicit);
        }

        // Password
        var credentialsCapability = ResourceTypeUtil.getEnabledCapability(resource, CredentialsCapabilityType.class);
        if (credentialsCapability != null) {
            if (isFetchingNotDisabledByClient(SchemaConstants.PATH_PASSWORD_VALUE)) {
                shadowItemsToReturn.setReturnPasswordExplicit(true);
            } else if (!CapabilityUtil.isPasswordReturnedByDefault(credentialsCapability)) {
                // The resource is capable of returning password but it does not do it by default.
                if (objectDefinition.getPasswordFetchStrategy() == EXPLICIT) {
                    shadowItemsToReturn.setReturnPasswordExplicit(true);
                }
            }
        }

        // Activation
        var activationCapability = ResourceTypeUtil.getEnabledCapability(resource, ActivationCapabilityType.class);
        if (activationCapability != null) {
            if (CapabilityUtil.isCapabilityEnabled(activationCapability.getStatus())) {
                if (!CapabilityUtil.isActivationStatusReturnedByDefault(activationCapability)) {
                    // The resource is capable of returning enable flag but it does not do it by default.
                    if (isFetchingNotDisabledByClient(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS)) {
                        shadowItemsToReturn.setReturnAdministrativeStatusExplicit(true);
                    } else if (objectDefinition.getActivationFetchStrategy(ActivationType.F_ADMINISTRATIVE_STATUS) == EXPLICIT) {
                        shadowItemsToReturn.setReturnAdministrativeStatusExplicit(true);
                    }
                }
            }
            if (CapabilityUtil.isCapabilityEnabled(activationCapability.getValidFrom())) {
                if (!CapabilityUtil.isActivationValidFromReturnedByDefault(activationCapability)) {
                    if (isFetchingNotDisabledByClient(SchemaConstants.PATH_ACTIVATION_VALID_FROM)) {
                        shadowItemsToReturn.setReturnValidFromExplicit(true);
                    } else if (objectDefinition.getActivationFetchStrategy(ActivationType.F_VALID_FROM) == EXPLICIT) {
                        shadowItemsToReturn.setReturnValidFromExplicit(true);
                    }
                }
            }
            if (CapabilityUtil.isCapabilityEnabled(activationCapability.getValidTo())) {
                if (!CapabilityUtil.isActivationValidToReturnedByDefault(activationCapability)) {
                    if (isFetchingNotDisabledByClient(SchemaConstants.PATH_ACTIVATION_VALID_TO)) {
                        shadowItemsToReturn.setReturnValidToExplicit(true);
                    } else if (objectDefinition.getActivationFetchStrategy(ActivationType.F_VALID_TO) == EXPLICIT) {
                        shadowItemsToReturn.setReturnValidToExplicit(true);
                    }
                }
            }
            if (CapabilityUtil.isCapabilityEnabled(activationCapability.getLockoutStatus())) {
                if (!CapabilityUtil.isActivationLockoutStatusReturnedByDefault(activationCapability)) {
                    // The resource is capable of returning lockout flag but it does not do it by default.
                    if (isFetchingNotDisabledByClient(SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS)) {
                        shadowItemsToReturn.setReturnAdministrativeStatusExplicit(true);
                    } else if (objectDefinition.getActivationFetchStrategy(ActivationType.F_LOCKOUT_STATUS) == EXPLICIT) {
                        shadowItemsToReturn.setReturnLockoutStatusExplicit(true);
                    }
                }
            }
        }

        var behaviorCapability = ResourceTypeUtil.getEnabledCapability(resource, BehaviorCapabilityType.class);
        if (behaviorCapability != null) {
            if (CapabilityUtil.isCapabilityEnabled(behaviorCapability.getLastLoginTimestamp())) {
                if (!CapabilityUtil.isLastLoginTimestampReturnedByDefault(behaviorCapability)) {
                    if (isFetchingNotDisabledByClient(SchemaConstants.PATH_BEHAVIOUR_LAST_LOGIN_TIMESTAMP)) {
                        shadowItemsToReturn.setReturnLastLoginTimestampExplicit(true);
                    }
                }
            }
        }

//        var behaviorCapability = ResourceTypeUtil.getBehaviorCapability(resource, beh);

        if (!shadowItemsToReturn.isAllDefault()) {
            LOGGER.trace("-> Resulting shadow items to return: {}", shadowItemsToReturn);
            return shadowItemsToReturn;
        } else {
            LOGGER.trace("-> Using default items to return");
            return null;
        }
    }

    private @NotNull Collection<? extends ShadowAttributeDefinition<?, ?, ?, ?>> getItemsToFetchExplicitly() {
        return getRelevantItemDefinitionsStream()
                .filter(itemDef -> shouldFetchExplicitly(itemDef))
                .toList();
    }

    private Stream<? extends ShadowAttributeDefinition<?, ?, ?, ?>> getRelevantItemDefinitionsStream() {
        return objectDefinition.getAttributeDefinitions().stream()
                .filter(def -> !def.isSimulated());
    }

    private boolean hasItemWithMinimalFetchStrategy() {
        return getRelevantItemDefinitionsStream()
                .anyMatch(def -> def.getFetchStrategy() == MINIMAL);
    }

    private boolean shouldFetchExplicitly(ShadowAttributeDefinition<?, ?, ?, ?> itemDef) {
        var fetchStrategy = itemDef.getFetchStrategy();
        if (fetchStrategy == EXPLICIT) {
            LOGGER.trace("WILL fetch explicitly because it's configured so: {}", itemDef);
            return true;
        } else if (!itemDef.canRead()) {
            LOGGER.trace("WILL NOT fetch explicitly because the attribute is not readable: {}", itemDef);
            return false;
        } else if (shadowItemsToReturn.isReturnDefaultAttributes()) {
            // We know that the strategy for this attribute is IMPLICIT, because
            //  - if it was MINIMAL, we wouldn't have set "return default ones";
            //  - EXPLICIT value was treated above
            assert fetchStrategy == IMPLICIT;
            LOGGER.trace("NEED NOT be fetched explicitly because it's returned by default anyway: {}", itemDef);
            return false;
        } else if (isFetchingRequestedByClient(itemDef)) { // TODO what about options that BLOCK retrieving given attribute?
            LOGGER.trace("WILL fetch explicitly because client requested it: {}", itemDef);
            return true;
        } else if (fetchStrategy == MINIMAL) {
            LOGGER.trace("WILL NOT fetch explicitly, because the fetch strategy is MINIMAL: {}", itemDef);
            return false;
        }
        assert fetchStrategy == IMPLICIT;
        if (itemDef.isReturnedByDefault()) {
            LOGGER.trace("WILL fetch explicitly, because strategy=IMPLICIT and it's normally returned by default, "
                    + "but we are asking to not return default attributes: {}", itemDef);
            return true;
        }  else {
            LOGGER.trace("WILL NOT fetch explicitly, because strategy=IMPLICIT, and it's not returned by default: {}", itemDef);
            return false;
        }
    }

    private boolean isFetchingRequestedByClient(ShadowAttributeDefinition<?, ?, ?, ?> itemDef) {
        return isFetchingRequestedByClient(itemDef.getStandardPath())
                // This is a legacy (wrong) behavior we want to preserve (MID-5838)
                || isFetchingRequestedByClient(itemDef.getItemName());
    }

    private boolean isFetchingRequestedByClient(ItemPath path) {
        return SelectorOptions.hasToIncludePath(path, getOperationOptions, false);
    }

    private boolean isFetchingNotDisabledByClient(ItemPath path) {
        return SelectorOptions.hasToIncludePath(path, getOperationOptions, true);
    }
}
