/*
 * Copyright (c) 2014-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 */
public class PhasedConstraints implements DebugDumpable {

    private final ItemSecurityConstraintsImpl requestConstraints = new ItemSecurityConstraintsImpl();
    private final ItemSecurityConstraintsImpl execConstraints = new ItemSecurityConstraintsImpl();

    public @NotNull ItemSecurityConstraintsImpl get(@NotNull AuthorizationPhaseType phase) {
        switch (phase) {
            case REQUEST:
                return requestConstraints;
            case EXECUTION:
                return execConstraints;
            default:
                throw new AssertionError(phase);
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(PhasedConstraints.class, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "request", requestConstraints, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "execution", execConstraints, indent+1);
        return sb.toString();
    }
}
