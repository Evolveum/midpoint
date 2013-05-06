/**
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;

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

}
