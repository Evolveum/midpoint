/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingProfileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingRootType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 *  Experimental.
 */
class GlobalTracingOverride {
    @NotNull final Collection<TracingRootType> roots;
    @NotNull final TracingProfileType profile;

    GlobalTracingOverride(@NotNull Collection<TracingRootType> roots, @NotNull TracingProfileType profile) {
        this.roots = roots;
        this.profile = profile;
    }
}
