/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
