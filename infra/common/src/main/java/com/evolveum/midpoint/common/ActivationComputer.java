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
package com.evolveum.midpoint.common;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TimeIntervalStatus;

/**
 * @author semancik
 *
 */
public class ActivationComputer {
	
	private Clock clock;

	public ActivationComputer() {
		super();
	}

	
	public ActivationComputer(Clock clock) {
		super();
		this.clock = clock;
	}


	public Clock getClock() {
		return clock;
	}


	public void setClock(Clock clock) {
		this.clock = clock;
	}
	
	public ActivationStatus getEffectiveStatus(ActivationType activationType) {
		ActivationStatus administrativeStatus = activationType.getAdministrativeStatus();
		if (administrativeStatus != null) {
			// Explicit administrative status overrides everything 
			return administrativeStatus;
		}
		if (activationType.isEnabled() != null) {
			// DEPRECATED legacy property
			if (activationType.isEnabled()) {
				return ActivationStatus.ENABLED;
			} else {
				return ActivationStatus.DISABLED;
			}
		}
		TimeIntervalStatus validityStatus = getValidityStatus(activationType);
		if (validityStatus == null) {
			// No administrative status, no validity. Defaults to disabled.
			return ActivationStatus.DISABLED;
		}
		switch (validityStatus) {
			case AFTER:
			case BEFORE:
				return ActivationStatus.DISABLED;
			case IN:
				return ActivationStatus.ENABLED;
		}
		// This should not happen
		return null;
	}
	
	public TimeIntervalStatus getValidityStatus(ActivationType activationType) {
		XMLGregorianCalendar validFrom = activationType.getValidFrom();
		XMLGregorianCalendar validTo = activationType.getValidTo();
		if (validFrom == null && validTo == null) {
			return null;
		}
		TimeIntervalStatus status = TimeIntervalStatus.IN;
		if (validFrom != null && clock.isFuture(validFrom)) {
			status = TimeIntervalStatus.BEFORE;
		}
		if (validTo != null && clock.isPast(validTo)) {
			status = TimeIntervalStatus.AFTER;
		}
		return status;
	}
	
	public void computeEffective(ActivationType activationType) {
		ActivationStatus effectiveStatus = null;
		ActivationStatus administrativeStatus = activationType.getAdministrativeStatus();
		if (administrativeStatus != null) {
			// Explicit administrative status overrides everything 
			effectiveStatus = administrativeStatus;
		} else if (activationType.isEnabled() != null) {
			// DEPRECATED legacy property
			if (activationType.isEnabled()) {
				effectiveStatus = ActivationStatus.ENABLED;
			} else {
				effectiveStatus = ActivationStatus.DISABLED;
			}
		}
		TimeIntervalStatus validityStatus = getValidityStatus(activationType);
		if (effectiveStatus == null) {
			if (validityStatus == null) {
				// No administrative status, no validity. Defaults to disabled.
				effectiveStatus = ActivationStatus.DISABLED;
			} else {
				switch (validityStatus) {
					case AFTER:
					case BEFORE:
						effectiveStatus = ActivationStatus.DISABLED;
						break;
					case IN:
						effectiveStatus = ActivationStatus.ENABLED;
						break;
				}
			}
		}
		activationType.setEffectiveStatus(effectiveStatus);
		activationType.setValidityStatus(validityStatus);
	}

}
