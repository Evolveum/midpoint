/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.List;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;

public interface ActivityPolicyConstraintEvaluator<
        C extends AbstractPolicyConstraintType,
        T extends EvaluatedActivityPolicyRuleTrigger<C>> {

    List<T> evaluate(C constraint, ActivityPolicyRuleEvaluationContext context, OperationResult result);
}
