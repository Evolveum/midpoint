/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorInstanceSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

import org.jetbrains.annotations.VisibleForTesting;

/**
 * Various useful methods related to capabilities.
 *
 * Notes:
 *
 * - To minimize confusion, the term "effective" has been removed from the methods' names, as it somehow implied that
 * the capability in question is enabled. We now use either names without adjective (like {@link #getCapability(CapabilitiesType,
 * Class)}) or explicitly mention "enabled" if enabled capabilities are to be returned, like
 * {@link #getEnabledActivationStatus(ActivationCapabilityType)}.
 *
 * @author semancik
 */
public class CapabilityUtil {

    /**
     * Selects a capability of given type from a {@link CapabilityCollectionType}.
     *
     * NOTE: Instead of calling this method with a specific capabilityClass, it is now easier to simply
     * call the appropriate getter method on the {@link CapabilityCollectionType} (after a non-null check).
     *
     * @throws IllegalArgumentException in the case of ambiguity, because this means that either the capabilityClass
     * is too "wide", and matches multiple capabilities. Or the capabilityCollection is malformed, and contains multiple
     * values of the given capability type. (Although this is currently not possible according to the schema.)
     */
    public static <T extends CapabilityType> @Nullable T getCapability(
            @Nullable CapabilityCollectionType capabilityCollection,
            @NotNull Class<T> capabilityClass) {
        List<CapabilityType> matching = getAllCapabilitiesStream(capabilityCollection)
                .filter(c -> capabilityClass.isAssignableFrom(c.getClass()))
                .collect(Collectors.toList());
        //noinspection unchecked
        return (T) MiscUtil.extractSingleton(
                matching,
                () -> new IllegalArgumentException("Multiple capabilities of type " + capabilityClass + ": " + matching));
    }

    /**
     * Returns the first matching capability in given list of capability collections.
     */
    public static <T extends CapabilityType> @Nullable T getCapability(
            @NotNull List<CapabilityCollectionType> capabilityCollectionsList,
            @NotNull Class<T> capabilityClass) {
        for (CapabilityCollectionType capabilityCollection : capabilityCollectionsList) {
            T inCollection = getCapability(capabilityCollection, capabilityClass);
            if (inCollection != null) {
                return inCollection;
            }
        }
        return null;
    }

    /**
     * Selects a matching capability:
     *
     * 1. first from configured capabilities,
     * 2. if not present, then from native capabilities.
     */
    public static <T extends CapabilityType> @Nullable T getCapability(
            @Nullable CapabilitiesType capabilities,
            @NotNull Class<T> capabilityClass) {
        if (capabilities == null) {
            return null;
        } else {
            return getCapability(
                    Arrays.asList( // Not List.of() because of nullability
                            capabilities.getConfigured(),
                            capabilities.getNative()),
                    capabilityClass);
        }
    }

    /**
     * Returns a stream of all capabilities in this collection. Assumes that there are no items besides the capabilities!
     * E.g. after adding `extension`, this method will have to be adapted.
     */
    private static @NotNull Stream<CapabilityType> getAllCapabilitiesStream(@Nullable CapabilityCollectionType capabilityCollection) {
        if (capabilityCollection == null) {
            return Stream.of();
        } else {
            //noinspection unchecked
            PrismContainerValue<CapabilityCollectionType> pcv = capabilityCollection.asPrismContainerValue();
            return pcv.getItems().stream()
                    .map(i -> i.getRealValue(CapabilityType.class));
        }
    }

    /** Returns all the capabilities from particular {@link CapabilityCollectionType}. */
    public static @NotNull List<CapabilityType> getAllCapabilities(@Nullable CapabilityCollectionType capabilityCollection) {
        return getAllCapabilitiesStream(capabilityCollection)
                .collect(Collectors.toList());
    }

    /**
     * Returns a combination of native and configured capabilities from given {@link CapabilitiesType} bean.
     *
     * @param includeDisabled Whether we want to obtain also capabilities with `enabled` set to `false`.
     */
    public static @NotNull List<CapabilityType> getCapabilities(@Nullable CapabilitiesType capabilities, boolean includeDisabled) {
        if (capabilities == null) {
            return List.of();
        }
        List<CapabilityType> rv = new ArrayList<>();
        List<CapabilityType> configuredCaps = getAllCapabilities(capabilities.getConfigured());
        List<CapabilityType> nativeCaps = getAllCapabilities(capabilities.getNative());
        for (CapabilityType configuredCapability : configuredCaps) {
            if (includeDisabled || isCapabilityEnabled(configuredCapability)) {
                rv.add(configuredCapability);
            }
        }
        for (CapabilityType nativeCapability : nativeCaps) {
            if (!containsCapability(configuredCaps, nativeCapability.getClass())) {
                if (includeDisabled || isCapabilityEnabled(nativeCapability)) {
                    rv.add(nativeCapability);
                }
            }
        }
        return rv;
    }

    public static boolean isCapabilityEnabled(@Nullable CapabilityType capability) {
        return capability != null && !Boolean.FALSE.equals(capability.isEnabled());
    }

    private static boolean containsCapability(
            @NotNull List<CapabilityType> capabilities,
            @NotNull Class<? extends CapabilityType> type) {
        return capabilities.stream()
                .anyMatch(c -> type.isAssignableFrom(c.getClass()));
    }

    public static String getCapabilityDisplayName(@NotNull CapabilityType capability) {
        // TODO: look for schema annotation
        String className = capability.getClass().getSimpleName();
        if (className.endsWith("CapabilityType")) { // should be always the case
            return className.substring(0, className.length() - "CapabilityType".length());
        } else {
            return className;
        }
    }

    public static boolean isPasswordReturnedByDefault(CredentialsCapabilityType capability) {
        if (capability == null) {
            return false;
        }
        PasswordCapabilityType password = capability.getPassword();
        if (password == null) {
            return false;
        }
        if (password.isReturnedByDefault() == null) {
            return true;
        }
        return password.isReturnedByDefault();
    }

    public static boolean isPasswordReadable(CredentialsCapabilityType capabilityType) {
        if (capabilityType == null) {
            return false;
        }
        PasswordCapabilityType passwordCapabilityType = capabilityType.getPassword();
        if (passwordCapabilityType == null) {
            return false;
        }
        if (BooleanUtils.isFalse(passwordCapabilityType.isEnabled())) {
            return false;
        }
        Boolean readable = passwordCapabilityType.isReadable();
        return BooleanUtils.isTrue(readable);
    }

    public static boolean isActivationStatusReturnedByDefault(ActivationCapabilityType capability) {
        if (capability == null) {
            return false;
        }
        ActivationStatusCapabilityType statusCap = getEnabledActivationStatus(capability);
        if (statusCap == null) {
            return false;
        }
        if (statusCap.isReturnedByDefault() == null) {
            return true;
        }
        return statusCap.isReturnedByDefault();
    }

    public static boolean isActivationLockoutStatusReturnedByDefault(ActivationCapabilityType capability) {
        if (capability == null) {
            return false;
        }
        ActivationLockoutStatusCapabilityType statusCap = capability.getLockoutStatus();
        if (statusCap == null) {
            return false;
        }
        if (statusCap.isReturnedByDefault() == null) {
            return true;
        }
        return statusCap.isReturnedByDefault();
    }

    public static boolean isActivationValidFromReturnedByDefault(ActivationCapabilityType capability) {
        if (capability == null) {
            return false;
        }
        ActivationValidityCapabilityType valCap = capability.getValidFrom();
        if (valCap == null) {
            return false;
        }
        if (valCap.isReturnedByDefault() == null) {
            return true;
        }
        return valCap.isReturnedByDefault();
    }

    public static boolean isActivationValidToReturnedByDefault(ActivationCapabilityType capability) {
        if (capability == null) {
            return false;
        }
        ActivationValidityCapabilityType valCap = capability.getValidTo();
        if (valCap == null) {
            return false;
        }
        if (valCap.isReturnedByDefault() == null) {
            return true;
        }
        return valCap.isReturnedByDefault();
    }

    public static boolean isLastLoginTimestampReturnedByDefault(BehaviorCapabilityType capability) {
        if (capability == null) {
            return false;
        }
        LastLoginTimestampCapabilityType lastLoginCap = capability.getLastLoginTimestamp();
        if (lastLoginCap == null) {
            return false;
        }
        if (lastLoginCap.isReturnedByDefault() == null) {
            return true;
        }
        return lastLoginCap.isReturnedByDefault();
    }

    /** Returns a set of classes of native capabilities. */
    public static Collection<Class<? extends CapabilityType>> getNativeCapabilityClasses(@Nullable CapabilitiesType capabilities) {
        if (capabilities == null) {
            return Set.of();
        } else {
            return getAllCapabilitiesStream(capabilities.getNative())
                    .map(CapabilityType::getClass)
                    .collect(Collectors.toSet());
        }
    }

    /**
     * TODO what's the use of this method? It is currently called only from the Resource wizard. We should perhaps delete it.
     */
    public static void fillDefaults(@NotNull CapabilityType capability) {
        if (capability.isEnabled() == null) {
            capability.setEnabled(true);
        }
        if (capability instanceof ActivationCapabilityType) {
            ActivationCapabilityType act = ((ActivationCapabilityType) capability);
            if (act.getStatus() == null) {
                ActivationStatusCapabilityType st = new ActivationStatusCapabilityType();
                act.setStatus(st);
                st.setEnabled(false); // TODO check if all midPoint code honors this flag!
                st.setReturnedByDefault(false);
                st.setIgnoreAttribute(true);
            } else {
                ActivationStatusCapabilityType st = act.getStatus();
                st.setEnabled(def(st.isEnabled(), true));
                st.setReturnedByDefault(def(st.isReturnedByDefault(), true));
                st.setIgnoreAttribute(def(st.isIgnoreAttribute(), true));
            }
            if (act.getLockoutStatus() == null) {
                ActivationLockoutStatusCapabilityType st = new ActivationLockoutStatusCapabilityType();
                act.setLockoutStatus(st);
                st.setEnabled(false);
                st.setReturnedByDefault(false);
                st.setIgnoreAttribute(true);
            } else {
                ActivationLockoutStatusCapabilityType st = act.getLockoutStatus();
                st.setEnabled(def(st.isEnabled(), true));
                st.setReturnedByDefault(def(st.isReturnedByDefault(), true));
                st.setIgnoreAttribute(def(st.isIgnoreAttribute(), true));
            }
            if (act.getValidFrom() == null) {
                ActivationValidityCapabilityType vf = new ActivationValidityCapabilityType();
                act.setValidFrom(vf);
                vf.setEnabled(false);
                vf.setReturnedByDefault(false);
            } else {
                ActivationValidityCapabilityType vf = act.getValidFrom();
                vf.setEnabled(def(vf.isEnabled(), true));
                vf.setReturnedByDefault(def(vf.isReturnedByDefault(), true));
            }
            if (act.getValidTo() == null) {
                ActivationValidityCapabilityType vt = new ActivationValidityCapabilityType();
                act.setValidTo(vt);
                vt.setEnabled(false);
                vt.setReturnedByDefault(false);
            } else {
                ActivationValidityCapabilityType vt = act.getValidTo();
                vt.setEnabled(def(vt.isEnabled(), true));
                vt.setReturnedByDefault(def(vt.isReturnedByDefault(), true));
            }
        } else if (capability instanceof CredentialsCapabilityType) {
            CredentialsCapabilityType cred = ((CredentialsCapabilityType) capability);
            if (cred.getPassword() == null) {
                PasswordCapabilityType pc = new PasswordCapabilityType();
                cred.setPassword(pc);
                pc.setEnabled(false);
                pc.setReturnedByDefault(false);
            } else {
                PasswordCapabilityType pc = cred.getPassword();
                pc.setEnabled(def(pc.isEnabled(), true));
                pc.setReturnedByDefault(def(pc.isReturnedByDefault(), true));
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static Boolean def(Boolean originalValue, boolean defaultValue) {
        return originalValue != null ? originalValue : defaultValue;
    }

    // TODO what if act itself is disabled?
    public static ActivationStatusCapabilityType getEnabledActivationStatus(ActivationCapabilityType act) {
        if (act != null && isEnabled(act.getStatus())) {
            return act.getStatus();
        } else {
            return null;
        }
    }

    /**
     * As {@link #getEnabledActivationStatus(ActivationCapabilityType)} but checks also if the "root" capability is enabled.
     */
    public static ActivationStatusCapabilityType getEnabledActivationStatusStrict(ActivationCapabilityType act) {
        if (isEnabled(act) && isEnabled(act.getStatus())) {
            return act.getStatus();
        } else {
            return null;
        }
    }

    public static boolean isEnabled(CapabilityType cap) {
        return cap != null && !Boolean.FALSE.equals(cap.isEnabled());
    }

    public static LastLoginTimestampCapabilityType getEnabledLastLoginCapabilityStrict(BehaviorCapabilityType act) {
        if (isEnabled(act) && isEnabled(act.getLastLoginTimestamp())) {
            return act.getLastLoginTimestamp();
        } else {
            return null;
        }
    }

    private static boolean isEnabled(ActivationStatusCapabilityType cap) {
        return cap != null && !Boolean.FALSE.equals(cap.isEnabled());
    }

    private static boolean isEnabled(ActivationValidityCapabilityType cap) {
        return cap != null && !Boolean.FALSE.equals(cap.isEnabled());
    }

    private static boolean isEnabled(ActivationLockoutStatusCapabilityType cap) {
        return cap != null && !Boolean.FALSE.equals(cap.isEnabled());
    }

    private static boolean isEnabled(ActivationCapabilityType cap) {
        return cap != null && !Boolean.FALSE.equals(cap.isEnabled());
    }

    // TODO what if act is disabled?
    public static ActivationValidityCapabilityType getEnabledActivationValidFrom(ActivationCapabilityType act) {
        if (act != null && isEnabled(act.getValidFrom())) {
            return act.getValidFrom();
        } else {
            return null;
        }
    }

    // TODO what if act is disabled?
    public static ActivationValidityCapabilityType getEnabledActivationValidTo(ActivationCapabilityType act) {
        if (act != null && isEnabled(act.getValidTo())) {
            return act.getValidTo();
        } else {
            return null;
        }
    }

    // TODO what if act is disabled?
    public static ActivationLockoutStatusCapabilityType getEnabledActivationLockoutStatus(ActivationCapabilityType act) {
        if (act != null && isEnabled(act.getLockoutStatus())) {
            return act.getLockoutStatus();
        } else {
            return null;
        }
    }

    /**
     * As {@link #getEnabledActivationLockoutStatus(ActivationCapabilityType)} but checks also if the "root" capability is enabled.
     */
    public static ActivationLockoutStatusCapabilityType getEnabledActivationLockoutStrict(ActivationCapabilityType act) {
        if (isEnabled(act) && isEnabled(act.getLockoutStatus())) {
            return act.getLockoutStatus();
        } else {
            return null;
        }
    }

    /**
     * Creates {@link CapabilityCollectionType} from a plain list of capabilities.
     *
     * TODO consider whether we shouldn't return `null` if the list is empty.
     */
    public static @NotNull CapabilityCollectionType createCapabilityCollection(@NotNull List<CapabilityType> capabilities) {
        try {
            SchemaRegistry schemaRegistry = PrismContext.get().getSchemaRegistry();
            var def =
                    MiscUtil.requireNonNull(
                            schemaRegistry.findComplexTypeDefinitionByCompileTimeClass(CapabilityCollectionType.class),
                            () -> new IllegalStateException("No CTD for CapabilityCollectionType"));
            CapabilityCollectionType capabilityCollectionBean = new CapabilityCollectionType();
            for (CapabilityType capability : capabilities) {
                PrismContainerDefinition<?> capDef = findCapabilityDefinition(def, capability.getClass());
                PrismContainer<?> capContainer = capDef.instantiate();
                //noinspection unchecked
                capContainer.add(capability.asPrismContainerValue());
                //noinspection unchecked
                capabilityCollectionBean.asPrismContainerValue().add(capContainer);
            }
            return capabilityCollectionBean;
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "When creating capability collection bean");
        }
    }

    private static @NotNull PrismContainerDefinition<?> findCapabilityDefinition(
            ComplexTypeDefinition def, Class<? extends CapabilityType> type) {
        return (PrismContainerDefinition<?>) def.getDefinitions().stream()
                .filter(itemDef -> type.equals(itemDef.getTypeClass()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No definition for capability " + type));
    }

    /**
     * Normally, `null` and empty states are substantially different: the former means "unknown", the latter "none".
     * So this method should be used only for testing, e.g. to check that there are _some_ capabilities present.
     */
    @VisibleForTesting
    public static boolean isEmpty(CapabilityCollectionType capabilities) {
        return capabilities == null
                || capabilities.asPrismContainerValue().hasNoItems(); // Adapt after some non-capability items are added
    }

    public static int size(CapabilityCollectionType capabilities) {
        return capabilities != null ? capabilities.asPrismContainerValue().size() : 0;
    }

    public static <T extends CapabilityType> T getCapability(
            @NotNull ResourceType resource, @NotNull Class<T> capabilityClass) {
        return getCapability(resource, (CapabilityCollectionType) null, capabilityClass);
    }

    /**
     * Gets a specific capability from resource/connectors/object-class.
     *
     * Notes:
     *
     * - Resource vs connector: The capability from specific connector is used only if it's enabled.
     *
     * TODO allow configured capabilities also for refined object classes
     */
    public static <T extends CapabilityType> T getCapability(
            @NotNull ResourceType resource,
            @Nullable ResourceObjectDefinition objectDefinition,
            @NotNull Class<T> capabilityClass) {

        var typeDefinition = objectDefinition != null ? objectDefinition.getTypeDefinition() : null;
        var specificCapabilities = typeDefinition != null ? typeDefinition.getSpecificCapabilities() : null;
        return getCapability(resource, specificCapabilities, capabilityClass);
    }

    public static <T extends CapabilityType> T getCapability(
            @NotNull ResourceType resource,
            @Nullable CapabilityCollectionType specificObjectTypeOrClassCapabilities,
            @NotNull Class<T> capabilityClass) {

        if (specificObjectTypeOrClassCapabilities != null) {
            T inSpecific = CapabilityUtil.getCapability(specificObjectTypeOrClassCapabilities, capabilityClass);
            if (inSpecific != null) {
                return inSpecific;
            }
        }

        for (ConnectorInstanceSpecificationType additionalConnectorBean : resource.getAdditionalConnector()) {
            T inConnector = getEnabledCapability(additionalConnectorBean, capabilityClass);
            if (inConnector != null) {
                return inConnector;
            }
        }

        return getCapability(resource.getCapabilities(), capabilityClass);
    }

    public static boolean isReadingCachingOnly(@NotNull ResourceType resource, @Nullable ResourceObjectDefinition objectDefinition) {
        var readCapability = getEnabledCapability(resource, objectDefinition, ReadCapabilityType.class);
        return readCapability != null && Boolean.TRUE.equals(readCapability.isCachingOnly());
    }

    public static <T extends CapabilityType> T getEnabledCapability(
            @NotNull ResourceType resource,
            @Nullable ResourceObjectDefinition objectDefinition,
            @NotNull Class<T> capabilityClass) {
        T capability = getCapability(resource, objectDefinition, capabilityClass);
        return isCapabilityEnabled(capability) ? capability : null;
    }
    /**
     * Returns the additional connector capability - but only if it's enabled.
     */
    private static <T extends CapabilityType> T getEnabledCapability(
            @NotNull ConnectorInstanceSpecificationType additionalConnectorSpecBean,
            @NotNull Class<T> capabilityClass) {
        T capability = getCapability(additionalConnectorSpecBean.getCapabilities(), capabilityClass);
        return isCapabilityEnabled(capability) ? capability : null;
    }

    public static boolean isActivationStatusCapabilityEnabled(
            @NotNull ResourceType resource, @Nullable ResourceObjectDefinition objectDefinition) {
        return getEnabledActivationStatusStrict(
                getCapability(resource, objectDefinition, ActivationCapabilityType.class)) != null;
    }
}
