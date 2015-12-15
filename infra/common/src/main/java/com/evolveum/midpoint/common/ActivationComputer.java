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
package com.evolveum.midpoint.common;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;

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
	
	public ActivationStatusType getEffectiveStatus(ActivationType activationType, ActivationStatusType defaultStatus) {
		return getEffectiveStatus(activationType, getValidityStatus(activationType), defaultStatus);
	}
	
	public ActivationStatusType getEffectiveStatus(ActivationType activationType, TimeIntervalStatusType validityStatus, ActivationStatusType defaultStatus) {
		if (activationType == null) {
			return defaultStatus;
		}
		ActivationStatusType administrativeStatus = activationType.getAdministrativeStatus();
		if (administrativeStatus != null) {
			// Explicit administrative status overrides everything 
			return administrativeStatus;
		}
		if (validityStatus == null) {
			// No administrative status, no validity. Return default.
			return defaultStatus;
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
		}
		TimeIntervalStatusType validityStatus = getValidityStatus(activationType);
		if (effectiveStatus == null) {
			if (validityStatus == null) {
				// No administrative status, no validity. Defaults to enabled.
				effectiveStatus = ActivationStatusType.ENABLED;
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
			return true;
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
