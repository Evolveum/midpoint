/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.LifecycleUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;

import org.jetbrains.annotations.NotNull;

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

    public ActivationStatusType getEffectiveStatus(String lifecycleStatus, ActivationType activationType, LifecycleStateModelType stateModel) {
        return getEffectiveStatus(lifecycleStatus, activationType, getValidityStatus(activationType), stateModel);
    }

    public ActivationStatusType getEffectiveStatus(
            String lifecycleStatus,
            ActivationType activationType,
            TimeIntervalStatusType validityStatus,
            LifecycleStateModelType stateModel) {
        ActivationStatusType forcedLifecycleActivationStatus = getForcedLifecycleActivationStatus(lifecycleStatus, stateModel);
        if (forcedLifecycleActivationStatus != null) {
            return forcedLifecycleActivationStatus;
        }

        if (activationType == null) {
            return ActivationStatusType.ENABLED;
        }
        ActivationStatusType administrativeStatus = activationType.getAdministrativeStatus();
        if (administrativeStatus != null) {
            // Explicit administrative status overrides everything
            return administrativeStatus;
        }
        if (validityStatus == null) {
            // No administrative status, no validity. Return default.
            return ActivationStatusType.ENABLED;
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
        if (activationType == null || referenceTime == null) {
            return null;
        }
        XMLGregorianCalendar validFrom = activationType.getValidFrom();
        XMLGregorianCalendar validTo = activationType.getValidTo();
        if (validFrom == null && validTo == null) {
            return null;
        } else if (validTo != null && referenceTime.compare(validTo) == DatatypeConstants.GREATER) {
            return TimeIntervalStatusType.AFTER;
        } else if (validFrom != null && referenceTime.compare(validFrom) == DatatypeConstants.LESSER) {
            return TimeIntervalStatusType.BEFORE;
        } else {
            return TimeIntervalStatusType.IN;
        }
    }

    public void computeEffective(String lifecycleStatus, ActivationType activationType, LifecycleStateModelType stateModel) {
        computeEffective(lifecycleStatus, activationType, clock.currentTimeXMLGregorianCalendar(), stateModel);
    }

    public void computeEffective(String lifecycleStatus, ActivationType activationType, XMLGregorianCalendar referenceTime, LifecycleStateModelType stateModel) {
        ActivationStatusType effectiveStatus = getForcedLifecycleActivationStatus(lifecycleStatus, stateModel);

        ActivationStatusType administrativeStatus = activationType.getAdministrativeStatus();
        if (effectiveStatus == null && administrativeStatus != null) {
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

    public boolean lifecycleHasActiveAssignments(
            String lifecycleStatus, LifecycleStateModelType stateModel, @NotNull TaskExecutionMode taskExecutionMode) {
        LifecycleStateType stateDefinition = LifecycleUtil.findStateDefinition(stateModel, lifecycleStatus);
        if (stateDefinition == null) {
            return defaultLifecycleHasActiveAssignments(lifecycleStatus, stateModel, taskExecutionMode);
        }
        Boolean activeAssignments = stateDefinition.isActiveAssignments();
        if (activeAssignments == null) {
            return defaultLifecycleHasActiveAssignments(lifecycleStatus, stateModel, taskExecutionMode);
        }
        return activeAssignments; // TODO do we want to have the same property for all task execution modes?
    }

    private boolean defaultLifecycleHasActiveAssignments(
            String lifecycleStatus, LifecycleStateModelType stateModel, @NotNull TaskExecutionMode taskExecutionMode) {
        if (!taskExecutionMode.isProductionConfiguration() && SchemaConstants.LIFECYCLE_PROPOSED.equals(lifecycleStatus)) {
            return true; // FIXME TEMPORARY IMPLEMENTATION
        }

        ActivationStatusType forcedLifecycleActivationStatus = getForcedLifecycleActivationStatus(lifecycleStatus, stateModel);
        if (forcedLifecycleActivationStatus == null) {
            return true;
        }
        switch (forcedLifecycleActivationStatus) {
            case ENABLED:
                return true;
            case DISABLED:
            case ARCHIVED:
                return false;
            default:
                throw new IllegalStateException("Unknown forced activation "+forcedLifecycleActivationStatus);
        }
    }


    private ActivationStatusType getForcedLifecycleActivationStatus(String lifecycleStatus, LifecycleStateModelType stateModel) {
        LifecycleStateType stateDefinition = LifecycleUtil.findStateDefinition(stateModel, lifecycleStatus);
        if (stateDefinition == null) {
            return getHardcodedForcedLifecycleActivationStatus(lifecycleStatus);
        }
        return stateDefinition.getForcedActivationStatus();
    }


    private ActivationStatusType getHardcodedForcedLifecycleActivationStatus(String lifecycleStatus) {
        if (lifecycleStatus == null
                || lifecycleStatus.equals(SchemaConstants.LIFECYCLE_ACTIVE)
                || lifecycleStatus.equals(SchemaConstants.LIFECYCLE_DEPRECATED)) {
            return null;
        } else if (lifecycleStatus.equals(SchemaConstants.LIFECYCLE_ARCHIVED)) {
            return ActivationStatusType.ARCHIVED;
        } else {
            return ActivationStatusType.DISABLED;
        }
    }
}
