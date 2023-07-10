/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.util.exception.*;

/**
 * @author Radovan Semancik
 */
public interface ScriptEvaluator {

    /**
     * Evaluates given script in given context. Everything is wrapped into {@link ScriptExpressionEvaluationContext} object.
     */
    @NotNull <V extends PrismValue> List<V> evaluate(@NotNull ScriptExpressionEvaluationContext context)
            throws ExpressionEvaluationException, ObjectNotFoundException, ExpressionSyntaxException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    /**
     * Returns human readable name of the language that this evaluator supports
     */
    String getLanguageName();

    /**
     * Returns (canonical) URL of the language that this evaluator can handle
     */
    @NotNull String getLanguageUrl();

    /**
     * Can indicate that script evaluator is not initialized, e.g. optional script evaluators
     * (Python) or depending on the JDK platform (JavaScript/ECMAScript).
     */
    default boolean isInitialized() {
        return true;
    }
}
