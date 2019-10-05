/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.expr.triggerSetter;

import org.jetbrains.annotations.NotNull;

/**
 *  Information on the trigger created for a given object.
 */
class CreatedTrigger {

    /**
     * OID of the holder. It is sometimes resolved only when the trigger is being created.
     */
    @NotNull private final String holderOid;

    /**
     * This is the fire time of the trigger created. Of course there might exist other triggers as well: created on another node,
     * or created by a mechanism other than OptimizingTriggerCreator. We don't care. We have just one goal: to avoid redundant
     * triggers being added when they come in a series on a single node.
     */
    private final long fireTime;

    CreatedTrigger(@NotNull String holderOid, long fireTime) {
        this.holderOid = holderOid;
        this.fireTime = fireTime;
    }

    @NotNull
    String getHolderOid() {
        return holderOid;
    }

    long getFireTime() {
        return fireTime;
    }

    @Override
    public String toString() {
        return "CreatedTrigger{" +
                "holderOid='" + holderOid + '\'' +
                ", fireTime=" + fireTime +
                '}';
    }
}
