/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
