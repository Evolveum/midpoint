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

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TimeIntervalStatusType;

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
	
	public ActivationStatusType getEffectiveStatus(ActivationType activationType) {
		return getEffectiveStatus(activationType, getValidityStatus(activationType));
	}
	
	public ActivationStatusType getEffectiveStatus(ActivationType activationType, TimeIntervalStatusType validityStatus) {
		if (activationType == null) {
			return ActivationStatusType.DISABLED;
		}
		ActivationStatusType administrativeStatus = activationType.getAdministrativeStatus();
		if (administrativeStatus != null) {
			// Explicit administrative status overrides everything 
			return administrativeStatus;
		}
		if (activationType.isEnabled() != null) {
			// DEPRECATED legacy property
			if (activationType.isEnabled()) {
				return ActivationStatusType.ENABLED;
			} else {
				return ActivationStatusType.DISABLED;
			}
		}
		if (validityStatus == null) {
			// No administrative status, no validity. Defaults to disabled.
			return ActivationStatusType.DISABLED;
		}
		switch (validityStatus) {
			case AFTER:
			case BEFORE:
				return ActivationStatusType.DISABLED;
			case IN:
				return ActivationStatusType.ENABLED;
		}
		// This should not happen
		return null;
	}
	
	public TimeIntervalStatusType getValidityStatus(ActivationType activationType) {
		return getValidityStatus(activationType, clock.currentTimeXMLGregorianCalendar());
	}
	
	public TimeIntervalStatusType getValidityStatus(ActivationType activationType, XMLGregorianCalendar referenceTime) {
		XMLGregorianCalendar validFrom = activationType.getValidFrom();
		XMLGregorianCalendar validTo = activationType.getValidTo();
		if (validFrom == null && validTo == null) {
			return null;
		}
		TimeIntervalStatusType status = TimeIntervalStatusType.IN;
		if (validFrom != null &&  (referenceTime == null || referenceTime.compare(validFrom) ==  DatatypeConstants.LESSER)) {
			status = TimeIntervalStatusType.BEFORE;
		}
		if (validTo != null && referenceTime.compare(validTo) ==  DatatypeConstants.GREATER) {
			status = TimeIntervalStatusType.AFTER;
		}
		return status;
	}
	
	public void computeEffective(ActivationType activationType) {
		computeEffective(activationType, clock.currentTimeXMLGregorianCalendar());
	}
	
	public void computeEffective(ActivationType activationType, XMLGregorianCalendar referenceTime) {
		ActivationStatusType effectiveStatus = null;
		ActivationStatusType administrativeStatus = activationType.getAdministrativeStatus();
		if (administrativeStatus != null) {
			// Explicit administrative status overrides everything 
			effectiveStatus = administrativeStatus;
		} else if (activationType.isEnabled() != null) {
			// DEPRECATED legacy property
			if (activationType.isEnabled()) {
				effectiveStatus = ActivationStatusType.ENABLED;
			} else {
				effectiveStatus = ActivationStatusType.DISABLED;
			}
		}
		TimeIntervalStatusType validityStatus = getValidityStatus(activationType);
		if (effectiveStatus == null) {
			if (validityStatus == null) {
				// No administrative status, no validity. Defaults to disabled.
				effectiveStatus = ActivationStatusType.DISABLED;
			} else {
				switch (validityStatus) {
					case AFTER:
					case BEFORE:
						effectiveStatus = ActivationStatusType.DISABLED;
						break;
					case IN:
						effectiveStatus = ActivationStatusType.ENABLED;
						break;
				}
			}
		}
		activationType.setEffectiveStatus(effectiveStatus);
		activationType.setValidityStatus(validityStatus);
	}

	public boolean isActive(ActivationType activationType) {
		if (activationType == null) {
			return false;
		}
		ActivationStatusType effectiveStatus = activationType.getEffectiveStatus();
		if (effectiveStatus == null) {
			computeEffective(activationType);
			effectiveStatus = activationType.getEffectiveStatus();
		}
		if (effectiveStatus == null) {
			return false;
		}
		return effectiveStatus == ActivationStatusType.ENABLED;
	}
	
}
