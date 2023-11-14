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
import org.jetbrains.annotations.Nullable;

/**
 * @author semancik
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

    /** Never returns {@link ActivationStatusType#ARCHIVED}. */
    public ActivationStatusType getEffectiveStatus(
            String lifecycleStatus, ActivationType activationBean, LifecycleStateModelType stateModel) {
        return getEffectiveStatus(
                lifecycleStatus,
                activationBean,
                getValidityStatus(activationBean),
                stateModel);
    }

    /** Never returns {@link ActivationStatusType#ARCHIVED}. */
    public ActivationStatusType getEffectiveStatus(
            @Nullable String lifecycleStatus,
            @Nullable ActivationType activationBean,
            @Nullable TimeIntervalStatusType validityStatus,
            @Nullable LifecycleStateModelType stateModel) {

        ActivationStatusType forcedEffectiveStatus = getForcedLifecycleEffectiveActivationStatus(lifecycleStatus, stateModel);
        if (forcedEffectiveStatus != null) {
            return forcedEffectiveStatus;
        }
        if (activationBean == null) {
            return ActivationStatusType.ENABLED;
        }
        ActivationStatusType administrativeStatus = activationBean.getAdministrativeStatus();
        if (administrativeStatus != null) {
            // Explicit administrative status overrides everything; except that ARCHIVED is converted to DISABLED
            return archivedToDisabled(administrativeStatus);
        }
        if (validityStatus != null) {
            return validityToEffective(validityStatus);
        }
        // No administrative status, no validity. Return default.
        return ActivationStatusType.ENABLED;
    }

    @NotNull
    private static ActivationStatusType validityToEffective(@NotNull TimeIntervalStatusType validityStatus) {
        return switch (validityStatus) {
            case AFTER, BEFORE -> ActivationStatusType.DISABLED;
            case IN -> ActivationStatusType.ENABLED;
        };
    }

    public static ActivationStatusType archivedToDisabled(ActivationStatusType status) {
        return status != ActivationStatusType.ARCHIVED ? status : ActivationStatusType.DISABLED;
    }

    public TimeIntervalStatusType getValidityStatus(ActivationType activationBean) {
        return getValidityStatus(activationBean, clock.currentTimeXMLGregorianCalendar());
    }

    public TimeIntervalStatusType getValidityStatus(ActivationType activationBean, XMLGregorianCalendar referenceTime) {
        if (activationBean == null || referenceTime == null) {
            return null;
        }
        XMLGregorianCalendar validFrom = activationBean.getValidFrom();
        XMLGregorianCalendar validTo = activationBean.getValidTo();
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

    public void setValidityAndEffectiveStatus(
            String lifecycleStatus, @NotNull ActivationType activationBean, LifecycleStateModelType stateModel) {

        TimeIntervalStatusType validityStatus = getValidityStatus(activationBean);
        activationBean.setValidityStatus(validityStatus);

        ActivationStatusType effectiveStatus = getEffectiveStatus(lifecycleStatus, activationBean, validityStatus, stateModel);
        activationBean.setEffectiveStatus(effectiveStatus);
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
        if (SchemaConstants.LIFECYCLE_SUSPENDED.equals(lifecycleStatus)) {
            return true; // FIXME TEMPORARY IMPLEMENTATION, need to do something smarter when we have full lifecycle model
        }

        ActivationStatusType forcedLifecycleActivationStatus =
                getForcedLifecycleEffectiveActivationStatus(lifecycleStatus, stateModel);
        if (forcedLifecycleActivationStatus == null) {
            return true;
        }
        return switch (forcedLifecycleActivationStatus) {
            case ENABLED -> true;
            case DISABLED -> false;
            case ARCHIVED -> throw new AssertionError();
        };
    }

    /** Never returns {@link ActivationStatusType#ARCHIVED}. */
    private ActivationStatusType getForcedLifecycleEffectiveActivationStatus(
            String lifecycleStatus, LifecycleStateModelType stateModel) {
        LifecycleStateType stateDefinition = LifecycleUtil.findStateDefinition(stateModel, lifecycleStatus);
        if (stateDefinition == null) {
            return getHardcodedForcedLifecycleActivationStatus(lifecycleStatus);
        }
        return archivedToDisabled(
                stateDefinition.getForcedActivationStatus());
    }

    private ActivationStatusType getHardcodedForcedLifecycleActivationStatus(String lifecycleStatus) {
        if (lifecycleStatus == null
                || lifecycleStatus.equals(SchemaConstants.LIFECYCLE_ACTIVE)
                || lifecycleStatus.equals(SchemaConstants.LIFECYCLE_DEPRECATED)) {
            return null;
        } else {
            return ActivationStatusType.DISABLED;
        }
    }
}
