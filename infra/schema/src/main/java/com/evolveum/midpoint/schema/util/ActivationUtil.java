/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author semancik
 *
 */
public class ActivationUtil {

    public static ActivationStatusType getAdministrativeStatus(FocusType focus) {
        return focus != null ? getAdministrativeStatus(focus.getActivation()) : null;
    }

    public static ActivationStatusType getAdministrativeStatus(ShadowType shadow) {
        return shadow != null ? getAdministrativeStatus(shadow.getActivation()) : null;
    }

    public static ActivationStatusType getAdministrativeStatus(PrismObject<ShadowType> shadow) {
        return getAdministrativeStatus(asObjectable(shadow));
    }

    private static ActivationStatusType getAdministrativeStatus(ActivationType activation) {
        return activation != null ? activation.getAdministrativeStatus() : null;
    }

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

    public static XMLGregorianCalendar getValidFrom(AssignmentType assignment) {
        var activation = assignment.getActivation();
        return activation != null ? activation.getValidFrom() : null;
    }

    public static XMLGregorianCalendar getValidFrom(ShadowType shadow) {
        var activation = shadow.getActivation();
        return activation != null ? activation.getValidFrom() : null;
    }

    public static XMLGregorianCalendar getValidTo(ShadowType shadow) {
        var activation = shadow.getActivation();
        return activation != null ? activation.getValidTo() : null;
    }

    public static LockoutStatusType getLockoutStatus(ShadowType objectType) {
        ActivationType activation = objectType.getActivation();
        return activation != null ? activation.getLockoutStatus() : null;
    }

    public static boolean isLockedOut(LockoutStatusType status) {
        return status == LockoutStatusType.LOCKED;
    }

    public static boolean isLockedOut(ActivationType activation) {
        return activation != null && activation.getLockoutStatus() == LockoutStatusType.LOCKED;
    }

    public static ActivationType createDisabled() {
        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.DISABLED);
        return activationType;
    }

    public static String getDisableReason(PrismObject<ShadowType> shadow) {
        return shadow != null ? getDisableReason(shadow.asObjectable().getActivation()) : null;
    }

    public static String getDisableReason(ActivationType activation) {
        return activation != null ? activation.getDisableReason() : null;
    }
}
