/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util.template;

import org.apache.commons.text.StringSubstitutor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Utility methods for string substitution needs in midPoint. Uses Apache Commons Text StringSubstitutor.
 * (Of course, you can - and perhaps should - call StringSubstitutor directly in the code. Here we provide
 * some commonly-used methods that require custom configuration of the substitutor.)
 */
public class StringSubstitutorUtil {

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
        return new StringSubstitutor(replacements, "{", "}").replace(template);
    }
}
