/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyType;

public class ActivityPolicyUtils {

    public static String createIdentifier(String ownerObjectOid, ActivityPolicyType policy) {
        return ownerObjectOid + ":" + policy.getId();
    }
}
