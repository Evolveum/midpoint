/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PhaseSelector {

    @Nullable private final AuthorizationPhaseType phase;
    private final boolean strict;

    private PhaseSelector(@Nullable AuthorizationPhaseType phase, boolean strict) {
        this.phase = phase;
        this.strict = strict;
    }

    /** Matches authorizations with given phase or no phase. */
    static PhaseSelector nonStrict(@NotNull AuthorizationPhaseType phase) {
        return new PhaseSelector(phase, false);
    }

    /** Matches authorizations with given phase only. (Ones with no phase specified are not matched.) */
    static PhaseSelector strict(@NotNull AuthorizationPhaseType phase) {
        return new PhaseSelector(phase, true);
    }

    /** Matches only those authorizations that apply to both phases (their phase is not specified). */
    static PhaseSelector both() {
        return new PhaseSelector(null, true);
    }

    public boolean matches(@Nullable AuthorizationPhaseType autzPhase) {
        if (strict) {
            return autzPhase == phase;
        } else {
            return autzPhase == phase || autzPhase == null || phase == null;
        }
    }

    @Override
    public String toString() {
        if (strict) {
            if (phase != null) {
                return "phase = " + phase + " (strictly)";
            } else {
                return "both phases (strictly)";
            }
        } else {
            if (phase != null) {
                return "phase = " + phase + " or both phases";
            } else {
                return "any phase";
            }
        }
    }
}
