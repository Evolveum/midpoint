/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.notifications.api.OperationStatus;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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

    default @Nullable CaseCorrelationContextType getCorrelationContext() {
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

    /**
     * @return The object attached to the case, whatever that means for the particular case.
     * Null if the object cannot be retrieved.
     */
    @Nullable ObjectType getObject();

    /**
     * @return The name of the {@link #getObject()}. Falls back to OID if the object cannot be retrieved.
     */
    @Nullable PolyStringType getObjectName();

    /**
     * @return The target object attached to the case, whatever that means for the particular case.
     * Null if the object cannot be retrieved.
     */
    @Nullable ObjectType getTarget();

    /**
     * @return The name of the {@link #getTarget()}. Falls back to OID if the object cannot be retrieved.
     */
    @Nullable PolyStringType getTargetName();

    /**
     * @return The (real) value of given path in the pre-focus in a correlation case.
     * Returns the collection if there are more matching values.
     */
    @Nullable Object getFocusValue(@NotNull String pathString);

    /**
     * @return The (real) values of given path in the pre-focus in a correlation case.
     * Returns the collection if there are more matching values.
     */
    @NotNull Collection<?> getFocusValues(@NotNull String pathString);
}
