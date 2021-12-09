/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.api;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtherPrivilegesLimitationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * TODO better name ;)
 *
 * @author mederly
 */
public class DelegatorWithOtherPrivilegesLimitations implements DebugDumpable {

    @NotNull private final UserType delegator;
    @NotNull private final List<OtherPrivilegesLimitationType> limitations;

    public DelegatorWithOtherPrivilegesLimitations(@NotNull UserType delegator,
            @NotNull List<OtherPrivilegesLimitationType> limitations) {
        this.delegator = delegator;
        this.limitations = limitations;
    }

    @NotNull
    public UserType getDelegator() {
        return delegator;
    }

    @NotNull
    public List<OtherPrivilegesLimitationType> getLimitations() {
        return limitations;
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
