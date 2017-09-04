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
