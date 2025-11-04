/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.Duration;
import java.util.Date;

public class ComputationUtil {

    public static Integer add(@Nullable Integer value1, @Nullable Integer value2) {
        if (value1 == null) {
            return value2;
        }
        if (value2 == null) {
            return value1;
        }
        return value1 + value2;
    }

    public static Long durationToMillis(Duration duration) {
        return duration != null ? duration.getTimeInMillis(new Date(0)) : null;
    }
}
