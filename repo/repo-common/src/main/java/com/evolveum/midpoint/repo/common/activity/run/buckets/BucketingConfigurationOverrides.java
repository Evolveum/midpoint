/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets;

import com.google.common.annotations.VisibleForTesting;

@VisibleForTesting
public class BucketingConfigurationOverrides {

    private static Long freeBucketWaitIntervalOverride;

    public static void setFreeBucketWaitIntervalOverride(Long value) {
        freeBucketWaitIntervalOverride = value;
    }

    static Long getFreeBucketWaitIntervalOverride() {
        return freeBucketWaitIntervalOverride;
    }
}
