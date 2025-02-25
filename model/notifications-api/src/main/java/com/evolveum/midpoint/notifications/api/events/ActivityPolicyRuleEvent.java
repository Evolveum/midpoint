/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.repo.common.activity.policy.EvaluatedActivityPolicyRule;

public interface ActivityPolicyRuleEvent extends ActivityEvent {

    String getRuleName();

    EvaluatedActivityPolicyRule getPolicyRule();
}
