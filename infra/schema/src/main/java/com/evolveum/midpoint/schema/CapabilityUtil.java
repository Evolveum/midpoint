/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.schema;

import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
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

	public static boolean isCapabilityEnabled(Object capability) throws SchemaException {
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

	private static boolean isCapabilityEnabled(Element capability) throws SchemaException {
		if (capability == null) {
			return false;
		}
		ObjectFactory capabilitiesObjectFactory = new ObjectFactory();
		QName enabledElementName = capabilitiesObjectFactory.createEnabled(true).getName();
		Element enabledElement = DOMUtil.getChildElement(capability, enabledElementName);
		if (enabledElement == null) {
			return true;
		}
		Boolean enabled = XmlTypeConverter.convertValueElementAsScalar(enabledElement, Boolean.class);
		if (enabled == null) {
			return true;
		}
		return enabled;
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

	public static boolean containsCapabilityWithSameElementName(List<Object> capabilities, Object capability) {
		if (capabilities == null) {
			return false;
		}
		QName capabilityElementName = JAXBUtil.getElementQName(capability);
		for (Object cap: capabilities) {
			QName capElementName = JAXBUtil.getElementQName(cap);
			if (capabilityElementName.equals(capElementName)) {
				return true;
			}
		}
		return false;
	}

	public static String getCapabilityDisplayName(Object capability) {
		// TODO: look for schema annotation
		String className;
		if (capability instanceof JAXBElement) {
			className = ((JAXBElement) capability).getDeclaredType().getSimpleName();
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

	public static boolean isActivationStatusReturnedByDefault(ActivationCapabilityType capability) {
		if (capability == null) {
			return false;
		}
		ActivationStatusCapabilityType statusCap = getEffectiveActivationStatus(capability);
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
				st.setEnabled(false);				// TODO check if all midPoint code honors this flag!
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

	public static <T extends CapabilityType> T getEffectiveCapability(CapabilitiesType capabilitiesType, Class<T> capabilityClass) {
		if (capabilitiesType == null) {
			return null;
		}
		if (capabilitiesType.getConfigured() != null) {
			T configuredCapability = CapabilityUtil.getCapability(capabilitiesType.getConfigured().getAny(), capabilityClass);
			if (configuredCapability != null) {
				return configuredCapability;
			}
			// No configured capability entry, fallback to native capability
		}
		if (capabilitiesType.getNative() != null) {
			T nativeCapability = CapabilityUtil.getCapability(capabilitiesType.getNative().getAny(), capabilityClass);
			if (nativeCapability != null) {
				return nativeCapability;
			}
		}
		return null;
	}

	public static ActivationStatusCapabilityType getEffectiveActivationStatus(ActivationCapabilityType act) {
		if (act != null && act.getStatus() != null && !Boolean.FALSE.equals(act.getStatus().isEnabled())) {
			return act.getStatus();
		} else {
			return null;
		}
	}

	public static ActivationValidityCapabilityType getEffectiveActivationValidFrom(ActivationCapabilityType act) {
		if (act != null && act.getValidFrom() != null && !Boolean.FALSE.equals(act.getValidFrom().isEnabled())) {
			return act.getValidFrom();
		} else {
			return null;
		}
	}

	public static ActivationValidityCapabilityType getEffectiveActivationValidTo(ActivationCapabilityType act) {
		if (act != null && act.getValidTo() != null && !Boolean.FALSE.equals(act.getValidTo().isEnabled())) {
			return act.getValidTo();
		} else {
			return null;
		}
	}

	public static ActivationLockoutStatusCapabilityType getEffectiveActivationLockoutStatus(ActivationCapabilityType act) {
		if (act != null && act.getLockoutStatus() != null && !Boolean.FALSE.equals(act.getLockoutStatus().isEnabled())) {
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
}
