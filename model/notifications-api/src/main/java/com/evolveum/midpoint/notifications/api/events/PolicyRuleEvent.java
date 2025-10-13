/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import org.jetbrains.annotations.NotNull;

/**
 * Any event that is triggered by the 'notify' policy rule action.
 */
public interface PolicyRuleEvent extends Event {

    @NotNull EvaluatedPolicyRule getPolicyRule();

    String getRuleName();
}
