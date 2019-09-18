/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 *
 */
public class NextRecompute {
    @NotNull public final XMLGregorianCalendar nextRecomputeTime;
    @Nullable public final String triggerOriginDescription;

    NextRecompute(@NotNull XMLGregorianCalendar nextRecomputeTime, @Nullable String triggerOriginDescription) {
        this.nextRecomputeTime = nextRecomputeTime;
        this.triggerOriginDescription = triggerOriginDescription;
    }
}
