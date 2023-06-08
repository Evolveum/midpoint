/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.api;

import java.util.List;
import javax.xml.namespace.QName;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDeputyUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtherPrivilegesLimitationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * TODO better name for the class
 */
public class DelegatorWithOtherPrivilegesLimitations implements DebugDumpable {

    @NotNull private final UserType delegator;
    @NotNull private final List<OtherPrivilegesLimitationType> limitations;

    public DelegatorWithOtherPrivilegesLimitations(
            @NotNull UserType delegator,
            @NotNull List<OtherPrivilegesLimitationType> limitations) {
        this.delegator = delegator;
        this.limitations = limitations;
    }

    public @NotNull UserType getDelegator() {
        return delegator;
    }

    /**
     * Note that deprecated {@link OtherPrivilegesLimitationType#F_APPROVAL_WORK_ITEMS} are not present in the limitations.
     */
    @VisibleForTesting
    public @NotNull List<OtherPrivilegesLimitationType> getLimitations() {
        return limitations;
    }

    /**
     * We should not ask about deprecated {@link OtherPrivilegesLimitationType#F_APPROVAL_WORK_ITEMS};
     * it is not in the limitations anyway.
     */
    public boolean limitationsAllow(@NotNull QName limitationItemName) {
        Preconditions.checkArgument(!limitationItemName.equals(OtherPrivilegesLimitationType.F_APPROVAL_WORK_ITEMS));
        return SchemaDeputyUtil.limitationsAllow(limitations, limitationItemName);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, "DelegatorWithOtherPrivilegesLimitations", indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Delegator", ObjectTypeUtil.toShortString(delegator), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "Limitations", limitations, indent + 1);
        return sb.toString();
    }
}
