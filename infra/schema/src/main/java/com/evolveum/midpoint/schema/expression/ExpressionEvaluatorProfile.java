/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.expression;

import com.evolveum.midpoint.schema.AccessDecision;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Specifies limitations on the use of a particular expression evaluator (e.g. `script`, `path`, `value`, etc).
 *
 * There are two cases:
 *
 * . `script` evaluator - here we obey {@link #getScriptLanguageExpressionProfile(String)} to determine whether a particular
 * scripting language is allowed or not.
 * . all other evaluators - here we just use {@link #getDefaultDecision()} to determine whether the evaluator is allowed or not.
 */
public interface ExpressionEvaluatorProfile extends Serializable {

    /**
     * Returns a decision whether the evaluator is allowed or not.
     *
     * NOTE: Use only for non-script evaluators. For script evaluator, use {@link #getScriptLanguageExpressionProfile(String)}.
     */
    @NotNull AccessDecision getDefaultDecision();

    /** Returns the profile for a particular scripting language. */
    @NotNull ScriptLanguageExpressionProfile getScriptLanguageExpressionProfile(@NotNull String qualifiedLanguageUri);

    /**
     * Returns {@code true} if at least anything is allowed in the given language.
     * Returns {@code false} if the language is completely forbidden.
     *
     * Doesn't need to be 100% correct: there may be situations that a language is de-facto completely forbidden,
     * but it is not obvious at the first sight. We can return {@code true} in such cases. The important thing
     * is that we never return {@code false} if the language is not completely forbidden.
     *
     * Useful e.g. to display sensible list of scripting options in the GUI.
     *
     * WARNING: Do not use for actual access control. Take the return value only as a hint.
     */
    boolean isNotCompletelyForbidden();

    /** Nothing is allowed. */
    ExpressionEvaluatorProfile NONE = new EmptyImpl(AccessDecision.DENY);

    /** Everything is allowed. */
    ExpressionEvaluatorProfile FULL = new EmptyImpl(AccessDecision.ALLOW);

    static ExpressionEvaluatorProfile none() {
        return NONE;
    }

    static ExpressionEvaluatorProfile full() {
        return FULL;
    }

    /** Default object to use when no real profile is available. */
    class EmptyImpl implements ExpressionEvaluatorProfile {
        private final AccessDecision decision;

        EmptyImpl(AccessDecision decision) {
            this.decision = decision;
        }

        @Override
        public @NotNull AccessDecision getDefaultDecision() {
            return decision;
        }

        @Override
        public @NotNull ScriptLanguageExpressionProfile getScriptLanguageExpressionProfile(@NotNull String qualifiedLanguageUri) {
            return ScriptLanguageExpressionProfile.forDecision(decision);
        }

        @Override
        public boolean isNotCompletelyForbidden() {
            return decision == AccessDecision.ALLOW;
        }
    }

    static @NotNull ExpressionEvaluatorProfile forDecision(@NotNull AccessDecision decision) {
        return switch (decision) {
            case ALLOW -> full();
            case DEFAULT, DENY -> none();
        };
    }
}
