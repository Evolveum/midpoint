/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyType;

public class ActivityPolicyUtils {

    public static String createIdentifier(ActivityPath path, ActivityPolicyType policy) {
        return path.toString() + ":" + policy.getId();
    }
}
