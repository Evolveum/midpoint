/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ObjectOperationPolicyTypeUtil {

    public static final ItemName PATH_ADD = ObjectOperationPolicyType.F_ADD;
    public static final ItemName PATH_MODIFY = ObjectOperationPolicyType.F_MODIFY;
    public static final ItemName PATH_DELETE = ObjectOperationPolicyType.F_DELETE;
    public static final ItemPath PATH_SYNC_INBOUND = ItemPath.create(
            ObjectOperationPolicyType.F_SYNCHRONIZE, SynchronizeOperationPolicyConfigurationType.F_INBOUND);
    public static final ItemPath PATH_SYNC_OUTBOUND = ItemPath.create(
            ObjectOperationPolicyType.F_SYNCHRONIZE, SynchronizeOperationPolicyConfigurationType.F_OUTBOUND);
    public static final ItemPath PATH_MEMBERSHIP_SYNC_INBOUND = ItemPath.create(
            ObjectOperationPolicyType.F_SYNCHRONIZE,
            SynchronizeOperationPolicyConfigurationType.F_MEMBERSHIP,
            SynchronizeMembershipOperationPolicyConfigurationType.F_INBOUND);
    public static final ItemPath PATH_MEMBERSHIP_SYNC_OUTBOUND = ItemPath.create(
            ObjectOperationPolicyType.F_SYNCHRONIZE,
            SynchronizeOperationPolicyConfigurationType.F_MEMBERSHIP,
            SynchronizeMembershipOperationPolicyConfigurationType.F_OUTBOUND);
    public static final ItemPath PATH_MEMBERSHIP_TOLERANCE = ItemPath.create(
            ObjectOperationPolicyType.F_SYNCHRONIZE,
            SynchronizeOperationPolicyConfigurationType.F_MEMBERSHIP,
            SynchronizeMembershipOperationPolicyConfigurationType.F_TOLERANT);

    /** Returns the `delete` policy severity, or `null` if there are no restrictions. */
    public static @Nullable OperationPolicyViolationSeverityType getDeletionRestrictionSeverity(
            @NotNull ObjectOperationPolicyType policy) {
        OperationPolicyConfigurationType delete = policy.getDelete();
        if (delete != null && Boolean.FALSE.equals(delete.isEnabled())) {
            return Objects.requireNonNullElse(delete.getSeverity(), OperationPolicyViolationSeverityType.ERROR);
        } else {
            return null; // operation is allowed
        }
    }

    public static boolean isAddDisabled(@NotNull ObjectOperationPolicyType policy) {
        return isDisabled(policy, PATH_ADD);
    }

    public static boolean isModifyDisabled(@NotNull ObjectOperationPolicyType policy) {
        return isDisabled(policy, PATH_MODIFY);
    }

    public static boolean isDeleteDisabled(@NotNull ObjectOperationPolicyType policy) {
        return isDisabled(policy, PATH_DELETE);
    }

    /** Returns {@code true} if the sync inbound is completely disabled. */
    public static boolean isSyncInboundDisabled(@NotNull ObjectOperationPolicyType policy) {
        return getSyncInboundPolicyEnabled(policy) == SyncInboundOperationPolicyEnabledType.FALSE;
    }

    public static boolean areSyncInboundMappingsDisabled(@NotNull ObjectOperationPolicyType policy) {
        var status = getSyncInboundPolicyEnabled(policy);
        // Note this is the same as "status != TRUE", but more explicit
        return status == SyncInboundOperationPolicyEnabledType.FALSE
                || status == SyncInboundOperationPolicyEnabledType.EXCEPT_FOR_MAPPINGS;
    }

    public static boolean isSyncOutboundDisabled(@NotNull ObjectOperationPolicyType policy) {
        return isDisabled(policy, PATH_SYNC_OUTBOUND);
    }

    public static boolean isMembershipSyncInboundDisabled(@NotNull ObjectOperationPolicyType policy) {
        return isDisabled(policy, PATH_MEMBERSHIP_SYNC_INBOUND);
    }

    public static boolean isMembershipSyncOutboundDisabled(@NotNull ObjectOperationPolicyType policy) {
        return isDisabled(policy, PATH_MEMBERSHIP_SYNC_OUTBOUND);
    }

    private static boolean isDisabled(@NotNull ObjectOperationPolicyType policy, @NotNull ItemPath path) {
        var operationPolicy = (OperationPolicyConfigurationType) getOperationPolicy(policy, path);
        return operationPolicy != null && Boolean.FALSE.equals(operationPolicy.isEnabled());
    }

    private static @NotNull SyncInboundOperationPolicyEnabledType getSyncInboundPolicyEnabled(
            @NotNull ObjectOperationPolicyType policy) {
        var operationPolicy = (SyncInboundOperationPolicyConfigurationType) getOperationPolicy(policy, PATH_SYNC_INBOUND);
        return Objects.requireNonNullElse(
                operationPolicy != null ? operationPolicy.getEnabled() : null,
                SyncInboundOperationPolicyEnabledType.TRUE);
    }

    private static AbstractOperationPolicyConfigurationType getOperationPolicy(
            @NotNull ObjectOperationPolicyType objectPolicy, @NotNull ItemPath path) {
        //noinspection unchecked
        var item = (PrismContainer<? extends AbstractOperationPolicyConfigurationType>)
                objectPolicy.asPrismContainerValue().findItem(path);
        return item != null && item.hasAnyValue() ? item.getRealValue(AbstractOperationPolicyConfigurationType.class) : null;
    }

    public static Boolean getToleranceOverride(@NotNull ObjectOperationPolicyType objectPolicy) {
        //noinspection unchecked
        PrismProperty<Boolean> item = objectPolicy.asPrismContainerValue().findProperty(PATH_MEMBERSHIP_TOLERANCE);
        return item != null && item.hasAnyValue() ? item.getRealValue(Boolean.class) : null;
    }
}
