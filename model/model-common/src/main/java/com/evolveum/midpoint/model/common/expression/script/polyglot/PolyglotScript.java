package com.evolveum.midpoint.model.common.expression.script.polyglot;

import java.util.Map;

/**
 * Represents a script that can be executed within a polyglot environment.
 *
 * Implementations of this interface allow for the evaluation of scripts with provided variable bindings.
 */
public interface PolyglotScript {
    /**
     * Evaluates the script within the execution context using the provided variable bindings.
     *
     * The variables provided will be accessible within the script during its execution.
     *
     * @param variables a map of variable names to their corresponding values to be bound within the script's context
     * @return the result of the script execution, which could be any object depending on the script's logic and
     * return value
     */
    Object evaluate(Map<String, Object> variables);
}
