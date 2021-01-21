/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

/**
 * An additional filter used to match objects (returned by the iterative search) with the objectclass/kind/intent
 * specification, which is typically given in the synchronization task.
 *
 * TODO consolidate with {@link SyncTaskHelper.TargetInfo}.
 */
public interface SynchronizationObjectsFilter {

    /**
     * @return True if we should process given shadow in this synchronization task.
     *
     * (Normally, we check object class, kind, and intent with regards to values provided by the task.)
     */
    boolean matches(@NotNull ShadowType shadow);
}
