/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;

/**
 * TODO naming: effective vs. enabled
 *
 * @author semancik
 */
public class CapabilityUtil {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends CapabilityType> T getCapability(Collection<Object> capabilities, Class<T> capabilityClass) {
        if (capabilities == null) {
            return null;
        }
        for (Object cap : capabilities) {
            if (cap instanceof JAXBElement) {
                JAXBElement element = (JAXBElement) cap;
                if (capabilityClass.isAssignableFrom(element.getDeclaredType())) {
                    return (T) element.getValue();
                }
            } else if (capabilityClass.isAssignableFrom(cap.getClass())) {
                return (T) cap;
            }
        }
        return null;
    }

    public static boolean isCapabilityEnabled(Object capability) {
        if (capability == null) {
            return false;
        }
        if (capability instanceof JAXBElement<?>) {
            capability = ((JAXBElement<?>)capability).getValue();
        }

        if (capability instanceof CapabilityType) {
            return CapabilityUtil.isCapabilityEnabled((CapabilityType)capability);
        } else if (capability instanceof Element) {
            return CapabilityUtil.isCapabilityEnabled((Element)capability);
        } else {
            throw new IllegalArgumentException("Unexpected capability type "+capability.getClass());
        }
    }

    private static boolean isCapabilityEnabled(Element capability) {
        if (capability == null) {
            return false;
        }
        ObjectFactory capabilitiesObjectFactory = new ObjectFactory();
        QName enabledElementName = capabilitiesObjectFactory.createEnabled(true).getName();
        Element enabledElement = DOMUtil.getChildElement(capability, enabledElementName);
        return enabledElement == null || Boolean.parseBoolean(enabledElement.getTextContent());
    }

    public static <T extends CapabilityType> boolean isCapabilityEnabled(T capability) {
        if (capability == null) {
            return false;
        }
        if (capability.isEnabled() == null) {
            return true;
        }
        return capability.isEnabled();
    }

    public static Object getCapabilityWithSameElementName(List<Object> capabilities, Object capability) {
        if (capabilities == null) {
            return false;
        }
        QName capabilityElementName = JAXBUtil.getElementQName(capability);
        for (Object cap: capabilities) {
            QName capElementName = JAXBUtil.getElementQName(cap);
            if (capabilityElementName.equals(capElementName)) {
                return cap;
            }
        }
        return null;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean containsCapabilityWithSameElementName(List<Object> capabilities, Object capability) {
        return getCapabilityWithSameElementName(capabilities, capability) != null;
    }

    public static String getCapabilityDisplayName(Object capability) {
        // TODO: look for schema annotation
        String className;
        if (capability instanceof JAXBElement) {
            className = ((JAXBElement<?>) capability).getDeclaredType().getSimpleName();
        } else {
            className = capability.getClass().getSimpleName();
        }
        if (className.endsWith("CapabilityType")) {
            return className.substring(0, className.length() - "CapabilityType".length());
        }
        return className;
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

    @SuppressWarnings("unchecked")
    public static CapabilityType asCapabilityType(Object capabilityObject) {
        return capabilityObject instanceof CapabilityType ?
                (CapabilityType) capabilityObject :
                ((JAXBElement<? extends CapabilityType>) capabilityObject).getValue();
    }

    public static void fillDefaults(@NotNull CapabilityType capability) {
        if (capability.isEnabled() == null) {
            capability.setEnabled(true);
        }
        if (capability instanceof ActivationCapabilityType) {
            ActivationCapabilityType act = ((ActivationCapabilityType) capability);
            if (act.getStatus() == null) {
                ActivationStatusCapabilityType st = new ActivationStatusCapabilityType();
                act.setStatus(st);
                st.setEnabled(false);                // TODO check if all midPoint code honors this flag!
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

    private static Boolean def(Boolean originalValue, boolean defaultValue) {
        return originalValue != null ? originalValue : defaultValue;
    }

    /**
     * Selects a matching capability:
     *
     * 1. first from configured capabilities,
     * 2. if not present, then from native capabilities.
     */
    public static <T extends CapabilityType> T getEffectiveCapability(CapabilitiesType capabilitiesType, Class<T> capabilityClass) {
        if (capabilitiesType == null) {
            return null;
        }
        if (capabilitiesType.getConfigured() != null) {
            T configuredCapability = CapabilityUtil.getCapability(capabilitiesType.getConfigured().getAny(), capabilityClass);
            if (configuredCapability != null) {
                return configuredCapability;
            }
        }
        // No configured capability entry, fallback to native capability
        if (capabilitiesType.getNative() != null) {
            T nativeCapability = CapabilityUtil.getCapability(capabilitiesType.getNative().getAny(), capabilityClass);
            //noinspection RedundantIfStatement
            if (nativeCapability != null) {
                return nativeCapability;
            }
        }
        return null;
    }

    // TODO what if act is disabled?
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

    public static <T extends CapabilityType>  boolean hasNativeCapability(CapabilitiesType capabilities, Class<T> capabilityClass) {
        if (capabilities == null) {
            return false;
        }
        CapabilityCollectionType nativeCaps = capabilities.getNative();
        if (nativeCaps == null) {
            return false;
        }
        return getCapability(nativeCaps.getAny(), capabilityClass) != null;
    }

    public static <T extends CapabilityType>  boolean hasConfiguredCapability(CapabilitiesType capabilities, Class<T> capabilityClass) {
        if (capabilities == null) {
            return false;
        }
        CapabilityCollectionType configuredCaps = capabilities.getConfigured();
        if (configuredCaps == null) {
            return false;
        }
        return getCapability(configuredCaps.getAny(), capabilityClass) != null;
    }
}
