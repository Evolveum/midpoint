/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Map.Entry;

/**
 * @author mederly
 *
 */
public class TemplateUtil {

    // very primitive implementation, for now
    // TODO implement some escaping of control characters
    public static String replace(String template, @NotNull Map<String, String> replacements) {
        if (template == null) {
            return null;
        }
        String rv = template;
        for (Entry<String, String> entry : replacements.entrySet()) {
            rv = rv.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return rv;
    }
}
