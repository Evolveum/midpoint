/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectOperationPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValidationIssueSeverityType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ObjectOperationPolicyTypeUtil {

    /** Returns the `delete` policy severity, or `null` if there are no restrictions. */
    public static @Nullable ValidationIssueSeverityType getDeletionRestrictionSeverity(@NotNull ObjectOperationPolicyType policy) {
        // Current implementation indicates that the policy is computed in full.
        // But to make things more robust (e.g. until it's documented) let us be careful.
        OperationPolicyConfigurationType delete = policy.getDelete();
        if (delete != null && Boolean.FALSE.equals(delete.isEnabled())) {
            // TODO what is the default severity?
            return Objects.requireNonNullElse(delete.getSeverity(), ValidationIssueSeverityType.ERROR);
        } else {
            return null; // operation is allowed
        }
    }
}
