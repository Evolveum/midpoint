/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.provisioning.impl.shadows.PendingOperations;
import com.evolveum.midpoint.schema.util.PendingOperationTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * Determines the shadow lifecycle state according to https://docs.evolveum.com/midpoint/reference/resources/shadow/dead/.
 *
 * (Some of the necessary functionality is exported for use in a different context.)
 */
public class ShadowLifecycleStateDeterminer {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowLifecycleStateDeterminer.class);

    // NOTE: detection of quantum states (gestation, corpse) might not be precise. E.g. the shadow may already be
    // tombstone because it is not in the snapshot. But as long as the pending operation is in grace we will still
    // detect it as corpse. But that should not cause any big problems.
    public static @NotNull ShadowLifecycleStateType determineShadowState(
            @NotNull ProvisioningContext ctx, @NotNull ShadowType shadow) {
        return determineShadowStateInternal(ctx, shadow, getClock().currentTimeXMLGregorianCalendar());
   }

    private static Clock getClock() {
        return CommonBeans.get().clock;
    }

    /**
     * Determines the shadow lifecycle state according to https://docs.evolveum.com/midpoint/reference/resources/shadow/dead/.
     *
     * @param ctx Used to know the grace period. In emergency situations it can be null.
     */
    private static @NotNull ShadowLifecycleStateType determineShadowStateInternal(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType shadow,
            @NotNull XMLGregorianCalendar now) {
        ChangeTypeType pendingLifecycleOperation = findPendingLifecycleOperationInGracePeriod(ctx, shadow, now);
        // after the life (dead)
        if (ShadowUtil.isDead(shadow)) {
            if (pendingLifecycleOperation == ChangeTypeType.DELETE) {
                return ShadowLifecycleStateType.CORPSE;
            } else {
                return ShadowLifecycleStateType.TOMBSTONE;
            }
        }
        // before the life (not existing yet)
        if (!ShadowUtil.isExists(shadow)) {
            if (hasPendingOrExecutingAdd(shadow)) {
                return ShadowLifecycleStateType.CONCEIVED;
            } else {
                return ShadowLifecycleStateType.PROPOSED;
            }
        }
        // during the life (existing and not dead)
        if (pendingLifecycleOperation == ChangeTypeType.DELETE) {
            return ShadowLifecycleStateType.REAPING;
        } else if (pendingLifecycleOperation == ChangeTypeType.ADD) {
            return ShadowLifecycleStateType.GESTATING;
        } else {
            return ShadowLifecycleStateType.LIVE;
        }
    }

    /** Determines and updates the shadow state. */
    static void updateShadowState(ProvisioningContext ctx, ShadowType shadow) {
        ShadowLifecycleStateType state = determineShadowState(ctx, shadow);
        shadow.setShadowLifecycleState(state);
        LOGGER.trace("shadow state is {}", state);
    }

    /**
     * Returns {@link ChangeTypeType#ADD}, {@link ChangeTypeType#DELETE}, or null.
     *
     * Used also outside of this class.
     */
    public static ChangeTypeType findPendingLifecycleOperationInGracePeriod(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType shadow,
            @NotNull XMLGregorianCalendar now) {
        var pendingOperations = PendingOperations.of(shadow.getPendingOperation());
        if (pendingOperations.isEmpty()) {
            return null;
        }
        Duration gracePeriod = ctx.getGracePeriod();
        ChangeTypeType found = null;
        for (var pendingOperation : pendingOperations) {
            if (!pendingOperation.isAdd() && !pendingOperation.isDelete()) {
                continue; // only ADD/DELETE are lifecycle operations
            }
            if (pendingOperation.isCompletedAndOverPeriod(now, gracePeriod)) {
                continue;
            }
            if (pendingOperation.isDelete()) {
                return ChangeTypeType.DELETE; // DELETE always wins
            } else {
                found = ChangeTypeType.ADD; // If there is an ADD then let's check for delete.
            }
        }
        return found;
    }

    private static boolean hasPendingOrExecutingAdd(@NotNull ShadowType shadow) {
        return shadow.getPendingOperation().stream()
                .anyMatch(p ->
                        PendingOperationTypeUtil.isAdd(p)
                                && PendingOperationTypeUtil.isPendingOrExecuting(p));
    }
}
