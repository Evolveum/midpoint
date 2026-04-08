/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.repo.common.policy.GenericEvaluatedPolicyRule;

public interface ActivityPolicyRuleEvent extends ActivityEvent {

    String getRuleName();

    GenericEvaluatedPolicyRule getPolicyRule();
}
