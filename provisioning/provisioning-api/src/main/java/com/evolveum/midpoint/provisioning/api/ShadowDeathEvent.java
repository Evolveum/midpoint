/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

/**
 * Emitted when a shadow is converted from live to dead and then eventually to deleted.
 */
public class ShadowDeathEvent implements ProvisioningEvent {

    /**
     * OID of the shadow in question. For deleted shadows this is the OID before deletion.
     */
    @NotNull private final String oid;

    /**
     * New liveness state. Only {@link ShadowLivenessState#DEAD} or {@link ShadowLivenessState#DELETED} values are applicable.
     */
    @NotNull private final ShadowLivenessState newState;

    private ShadowDeathEvent(@NotNull String oid, @NotNull ShadowLivenessState newState) {
        this.oid = oid;
        this.newState = newState;
    }

    public static ShadowDeathEvent deleted(String oid) {
        return new ShadowDeathEvent(oid, ShadowLivenessState.DELETED);
    }

    public static ShadowDeathEvent dead(String oid) {
        return new ShadowDeathEvent(oid, ShadowLivenessState.DEAD);
    }

    public @NotNull String getOid() {
        return oid;
    }

    public @NotNull ShadowLivenessState getNewState() {
        return newState;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, this.getClass().getSimpleName(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "oid", oid, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "newState", String.valueOf(newState), indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "ShadowDeathEvent{" +
                "oid='" + oid + '\'' +
                ", newState=" + newState +
                '}';
    }
}
