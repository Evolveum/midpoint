/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import org.jetbrains.annotations.NotNull;

/**
 * Provides functionality common to all evaluations.
 */
abstract class AbstractEvaluation<AH extends AssignmentHolderType> {

    /**
     * Context of the evaluation.
     */
    @NotNull final EvaluationContext<AH> ctx;

    /**
     * Segment that is being evaluated.
     */
    @NotNull final AssignmentPathSegmentImpl segment;

    /**
     * This is to ensure the evaluation is carried out only once.
     */
    private boolean evaluated;

    AbstractEvaluation(@NotNull AssignmentPathSegmentImpl segment, @NotNull EvaluationContext<AH> ctx) {
        this.segment = segment;
        this.ctx = ctx;
    }

    void checkIfAlreadyEvaluated() {
        if (evaluated) {
            throw new IllegalStateException("Already evaluated");
        } else {
            evaluated = true;
        }
    }
}
