/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util.task.work;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDistributionDefinitionType;

import org.jetbrains.annotations.NotNull;

public class ActivityDefinitionUtil {

    public static @NotNull ActivityDistributionDefinitionType findOrCreateDistribution(ActivityDefinitionType activity) {
        if (activity.getDistribution() != null) {
            return activity.getDistribution();
        } else {
            return activity.beginDistribution();
        }
    }
}
