/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
