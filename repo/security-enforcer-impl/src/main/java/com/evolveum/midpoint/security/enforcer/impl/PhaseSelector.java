/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Selects authorizations according to their specified phase. */
public class PhaseSelector {

    /** Phase value to select on. */
    @Nullable private final AuthorizationPhaseType phase;

    /** If `true`, the phase value (including `null`) must match exactly. */
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

    public @NotNull String getSymbol() {
        StringBuilder sb = new StringBuilder();
        if (strict) {
            sb.append('!');
        } else {
            sb.append('.'); // not very nice (assumes the use in "PART.xx" id)
        }
        if (phase == null) {
            sb.append('b');
        } else {
            sb.append(
                    switch (phase) {
                        case REQUEST -> 'r';
                        case EXECUTION -> 'e';
                    }
            );
        }
        return sb.toString();
    }
}
