/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.notifications.api.OperationStatus;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ManualProvisioningContextType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;

/**
 * Event related to case management: either case-level or work-item-level.
 */
public interface CaseManagementEvent extends Event {

    @NotNull CaseType getCase();

    @Deprecated
    default String getProcessInstanceName1() {
        return getCaseName();
    }

    default String getCaseName() {
        return getOrig(getCase().getName());
    }

    OperationStatus getOperationStatus();

    ChangeType getChangeType();

    boolean isResultKnown();

    boolean isApproved();

    boolean isRejected();

    @Nullable ApprovalContextType getApprovalContext();

    default @Nullable ManualProvisioningContextType getManualProvisioningContext() {
        return getCase().getManualProvisioningContext();
    }

    default @Nullable CorrelationContextType getCorrelationContext() {
        return getCase().getCorrelationContext();
    }

    default boolean doesUseStages() {
        return getApprovalContext() != null;
    }

    /** Returns true if this event is related to an approval case. */
    default boolean isApproval() {
        return CaseTypeUtil.isApprovalCase(getCase());
    }

    @Deprecated
    default boolean isApprovalCase() {
        return isApproval();
    }

    /** Returns true if this event is related to a manual provisioning case. */
    default boolean isManualProvisioning() {
        return CaseTypeUtil.isManualProvisioningCase(getCase());
    }

    @Deprecated
    default boolean isManualResourceCase() {
        return isManualProvisioning();
    }

    /** Returns true if this event is related to a correlation case. */
    default boolean isCorrelation() {
        return CaseTypeUtil.isCorrelationCase(getCase());
    }
}
