/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Context of the evaluation of all policy rules related to either single evaluated assignment, or the object as a whole.
 * (It is called also "global context", although it is not really global.)
 */
class RulesEvaluationContext {

    /**
     * Rules that should be "recorded" by writing policy state to the corresponding item (object or assignment).
     */
    @NotNull final List<EvaluatedPolicyRule> rulesToRecord = new ArrayList<>();
}
