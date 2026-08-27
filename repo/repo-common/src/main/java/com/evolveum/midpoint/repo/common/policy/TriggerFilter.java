/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.policy;

import java.util.function.Predicate;

/** Just a predicate on triggers. */
public interface TriggerFilter extends Predicate<EvaluatedPolicyRuleTrigger<?>> {
}
