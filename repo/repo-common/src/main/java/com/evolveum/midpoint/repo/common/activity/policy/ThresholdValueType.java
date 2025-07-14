/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import org.jetbrains.annotations.NotNull;

public enum ThresholdValueType {

    /**
     * The threshold value is tested against a policy trigger counter.
     */
    COUNTER(new IntegerThresholdEvaluator()),

    /**
     * The threshold value is tested against a custom integer value.
     * For example, execution attempts. Execution attempts are not counted
     * via policy trigger counter, but rather by custom value stored in
     * the activity state.
     */
    INTEGER(new IntegerThresholdEvaluator()),

    /**
     * The threshold value is tested against a custom duration value.
     * For example, the execution time.
     */
    DURATION(new DurationThresholdEvaluator());

    private final ThresholdEvaluator evaluator;

    ThresholdValueType(ThresholdEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @NotNull
    public ThresholdEvaluator getEvaluator() {
        return evaluator;
    }
}
