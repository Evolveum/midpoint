/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class ActivationUtil {

	public static boolean hasAdministrativeActivation(ShadowType objectType) {
		ActivationType activation = objectType.getActivation();
		return activation != null && activation.getAdministrativeStatus() != null;
	}

	public static boolean isAdministrativeEnabled(ShadowType objectType) {
		return isAdministrativeEnabled(objectType.getActivation());
	}

	public static boolean isAdministrativeEnabled(ActivationType activation) {
		if (activation == null) {
			return false;
		}
		return activation.getAdministrativeStatus() == ActivationStatusType.ENABLED;
	}

	public static boolean isAdministrativeEnabledOrNull(ActivationType activation) {
		if (activation == null) {
			return true;
		}
		return activation.getAdministrativeStatus() == ActivationStatusType.ENABLED || activation.getAdministrativeStatus() == null;
	}

	public static boolean hasValidFrom(ShadowType objectType) {
		ActivationType activation = objectType.getActivation();
		return activation != null && activation.getValidFrom() != null;
	}

	public static boolean hasValidTo(ShadowType objectType) {
		ActivationType activation = objectType.getActivation();
		return activation != null && activation.getValidTo() != null;
	}

	public static boolean hasLockoutStatus(ShadowType objectType) {
		ActivationType activation = objectType.getActivation();
		return activation != null && activation.getLockoutStatus() != null;
	}

	public static boolean isLockedOut(ShadowType objectType) {
		return isLockedOut(objectType.getActivation());
	}

	public static boolean isLockedOut(ActivationType activation) {
		if (activation == null) {
			return false;
		}
		return activation.getLockoutStatus() == LockoutStatusType.LOCKED;
	}

	public static ActivationType createDisabled() {
		ActivationType activationType = new ActivationType();
		activationType.setAdministrativeStatus(ActivationStatusType.DISABLED);
		return activationType;
	}
}
