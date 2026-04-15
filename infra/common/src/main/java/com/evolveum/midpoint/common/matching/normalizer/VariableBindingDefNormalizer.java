/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.matching.normalizer;

import com.evolveum.midpoint.prism.normalization.Normalizer;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.regex.Pattern;

public class VariableBindingDefNormalizer implements Normalizer<VariableBindingDefinitionType> {

    @Override
    public @NotNull VariableBindingDefinitionType normalize(VariableBindingDefinitionType original) throws SchemaException {
        return original;
    }

    @Override
    public boolean match(VariableBindingDefinitionType a, VariableBindingDefinitionType b) throws SchemaException {
        String varA = normalizeValue(getPathValue(a));
        String varB = normalizeValue(getPathValue(b));

        if (varA == null || varB == null) {
            return false;
        }

        return varA.equals(varB);
    }

    @Override
    public boolean matchRegex(VariableBindingDefinitionType a, String regex) {
        String varA = normalizeValue(getPathValue(a));
        return varA != null && Pattern.matches(regex, varA);
    }

    /** Extracts path value as String safely. */
    private String getPathValue(VariableBindingDefinitionType variable) {
        if (variable == null || variable.getPath() == null) {
            return null;
        }
        return variable.getPath().toString();
    }

    /** Normalizes string (trims and lowercases), returns null if empty. */
    private String normalizeValue(String value) {
        if (value == null) {
            return null;
        }
        value = value.trim().toLowerCase();
        return value.isEmpty() ? null : value;
    }

    @Override
    public @NotNull QName getName() {
        return VariableBindingDefinitionType.COMPLEX_TYPE;
    }

    @Override
    public boolean isIdentity() {
        return false;
    }
}
