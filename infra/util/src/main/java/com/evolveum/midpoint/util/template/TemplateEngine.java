/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util.template;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Generic template expansion engine.
 *
 * Beyond simple references like $ref it supports scoped and parameterized expressions in the form of ${scope:ref(parameters)}.
 * Allows the use of '\' as a general escaping character (e.g. for "$" sign, for brackets, colons, quotes, etc).
 */
public class TemplateEngine {

    /**
     * Resolver of the references.
     */
    private final ReferenceResolver resolver;

    /**
     * If true, expressions are marked as ${...}. This is the recommended way.
     * If false, legacy marking of {...} is used.
     */
    private final boolean useExpressionStartCharacter;

    /**
     * If a reference cannot be resolved, should we throw an error (true), or silently replace it with an empty string (false)?
     */
    private final boolean errorOnUnresolved;

    public TemplateEngine(ReferenceResolver resolver, boolean useExpressionStartCharacter, boolean errorOnUnresolved) {
        this.resolver = resolver;
        this.useExpressionStartCharacter = useExpressionStartCharacter;
        this.errorOnUnresolved = errorOnUnresolved;
    }

    /**
     * Generic templating function.
     *
     * @param template Template e.g. node-${builtin:seq(%04d)}
     * @return resolved template e.g. node-0003
     */
    public String expand(String template) {
        return new TemplateResolution(template, resolver, useExpressionStartCharacter, errorOnUnresolved).resolve();
    }

    /**
     * Simply expands ${propertyName} Java system properties.
     */
    public static String simpleExpandProperties(String template) {
        JavaPropertiesResolver resolver = new JavaPropertiesResolver(null, true);
        TemplateEngine engine = new TemplateEngine(resolver, true, false);
        return engine.expand(template);
    }

    /**
     * Evaluates a template against set of replacement mappings.
     * The string(s) to be matched are denoted by "{key}" sequence(s).
     *
     * This is a legacy method. We should use the "$" sign in the future.
     *
     * @param template Template e.g. "{masterTaskName} ({index})"
     * @param replacements Map of e.g. "masterTaskName" -> "Reconciliation", "index" -> "1"
     * @return resolved template, e.g. "Reconciliation (1)"
     */
    public static String simpleExpand(String template, @NotNull Map<String, String> replacements) {
        MapResolver resolver = new MapResolver(null, true, null, replacements);
        TemplateEngine engine = new TemplateEngine(resolver, false, false);
        return engine.expand(template);
    }
}
