/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.reactor;

public enum ActionState {
    NOT_READY(false, false, false),
    APPLICABLE(true, false, false),
    APPLIED(false, true, false),
    FAILED(false, false, true);

    private ActionState(boolean satisfied, boolean applied, boolean failed) {
        this.satisfied = satisfied;
        this.applied = applied;
        this.failed = failed;
    }

    private final boolean satisfied;
    private final boolean applied;
    private final boolean failed;

    boolean canApply() {
        return satisfied;
    }

    boolean applied() {
        return applied;
    }

    boolean failed() {
        return failed;
    }

}
