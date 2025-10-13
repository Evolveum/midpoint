/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
