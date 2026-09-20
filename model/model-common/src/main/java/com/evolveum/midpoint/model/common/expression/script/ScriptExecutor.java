/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.util.exception.*;

/**
 * Executes scripts for specific scripting language, e.g. Groovy, JavaScript, Python, etc ({@link #getLanguageName()}).
 *
 * @author Radovan Semancik
 */
public interface ScriptExecutor {

    /**
     * Executes given script in given context. Everything is wrapped into {@link ScriptExecutionContext} object.
     */
    @NotNull <V extends PrismValue> List<V> execute(@NotNull ScriptExecutionContext context)
            throws ExpressionEvaluationException, ObjectNotFoundException, ExpressionSyntaxException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    /**
     * Returns human readable name of the language that this executor supports
     */
    String getLanguageName();

    /**
     * Returns (canonical) URL of the language that this executor supports
     */
    @NotNull String getLanguageUrl();

    /**
     * Can indicate that script executor is not initialized, e.g. for optional script executors
     * (Python) or depending on the JDK platform (JavaScript/ECMAScript).
     */
    default boolean isInitialized() {
        return true;
    }
}
