/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.List;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;

/**
 * TODO DOC
 */
public interface ActivityPolicyConstraintEvaluator<
        C extends AbstractPolicyConstraintType,
        T extends EvaluatedActivityPolicyRuleTrigger<C>> {

    List<T> evaluate(C constraint, ActivityPolicyRuleEvaluationContext context, OperationResult result);
}
