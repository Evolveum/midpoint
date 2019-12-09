/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.template;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Represents a state in template resolution process.
 */
class TemplateResolution {

    private static final char SCOPE_DELIMITER = ':';
    private static final char EXPRESSION_START_CHARACTER = '$';

    // Configuration
    @NotNull private final String template;
    @NotNull private final ReferenceResolver resolver;
    private final boolean useExpressionStartCharacter;
    private final boolean errorOnUnresolved;

    // State
    private StringBuilder result = new StringBuilder();
    private int position = 0;
    private int errorReportingSegmentStart;

    TemplateResolution(@NotNull String template, @NotNull ReferenceResolver resolver, boolean useExpressionStartCharacter,
            boolean errorOnUnresolved) {
        this.template = template;
        this.resolver = resolver;
        this.useExpressionStartCharacter = useExpressionStartCharacter;
        this.errorOnUnresolved = errorOnUnresolved;
    }

    String resolve() {
        while (position < template.length()) {
            char current = template.charAt(position);
            if (position < template.length() - 1) {
                char next = template.charAt(position + 1);
                if (current == '\\') {
                    result.append(next);
                    position++;
                } else if (useExpressionStartCharacter && current == EXPRESSION_START_CHARACTER && next != '{') {
                    resolveSimpleReference();
                } else if (useExpressionStartCharacter && current == EXPRESSION_START_CHARACTER ||
                        !useExpressionStartCharacter && current == '{') {
                    resolveComplexReference();
                } else {
                    result.append(current);
                    position++;
                }
            } else {
                // whatever the last character is, let's output it
                result.append(current);
                position++;
            }
        }
        return result.toString();
    }

    private void resolveSimpleReference() {
        int start = ++position;
        while (position < template.length() && isReferenceChar(template.charAt(position))) {
            position++;
        }
        if (position > start) {
            String reference = template.substring(start, position);
            resolveAndAppend(null, reference, emptyList());
        } else {
            result.append(EXPRESSION_START_CHARACTER);
        }
    }

    /**
     * Resolves something like ${scope:ref(parameters)} (with scope and parameters being optional).
     *
     * parameters = parameter1,parameter2,...,parameterN
     * where each parameter is either a plain string (except for comma and ')') or quoted string - in single or double quotes
     */
    private void resolveComplexReference() {
        errorReportingSegmentStart = position;
        if (useExpressionStartCharacter) {
            position += 2;
        } else {
            position += 1;
        }
        StringBuilder currentSegment = new StringBuilder();
        String scope = null;
        String reference = null;
        List<String> parameters = new ArrayList<>();
        for (; position < template.length(); position++) {
            char current = template.charAt(position);
            if (current == '\\') {
                position++;
                if (position == template.length()) {
                    currentSegment.append(current);         // What else? It will end up wrong anyway, as we should close with '}'
                } else {
                    currentSegment.append(template.charAt(position));
                }
            } else if (scope == null && current == SCOPE_DELIMITER) {
                scope = currentSegment.toString().trim();
                currentSegment.setLength(0);
            } else if (current == '(' || current == '}') {
                reference = currentSegment.toString().trim();
                if (current == '(') {
                    position++;
                    parseParameters(parameters);
                }
                break;
            } else {
                currentSegment.append(current);
            }
        }
        skipWhitespaces();
        if (position < template.length() && template.charAt(position) == '}') {
            resolveAndAppend(scope, reference, parameters);
            position++;
        } else {
            throw new IllegalArgumentException("Unfinished reference: " + template.substring(errorReportingSegmentStart));
        }
    }

    private void parseParameters(List<String> parameters) {
        while (position < template.length()) {
            char current = template.charAt(position);
            if (Character.isWhitespace(current)) {
                position++;
            } else if (current == ')') {
                position++;
                break;
            } else if (current == '\'' || current == '\"') {
                //noinspection UnnecessaryLocalVariable
                char border = current;
                position++;
                StringBuilder parameter = new StringBuilder();
                while (position < template.length()) {
                    char c = template.charAt(position++);
                    if (c == border) {
                        parameters.add(parameter.toString());
                        skipWhitespaces();
                        if (position < template.length()) {
                            char separator = template.charAt(position++);
                            if (separator == ')') {
                                return;
                            } else if (separator == ',') {
                                break;
                            } else {
                                throw new IllegalArgumentException("Unexpected content after parameter: " + template.substring(errorReportingSegmentStart, position));
                            }
                        }
                    } else {
                        parameter.append(c);
                    }
                }
            } else {
                StringBuilder parameter = new StringBuilder();
                while (position < template.length()) {
                    char c = template.charAt(position++);
                    if (c == ',' || c == ')') {
                        parameters.add(parameter.toString().trim());
                        if (c == ')') {
                            return;
                        } else {
                            break;
                        }
                    } else {
                        parameter.append(c);
                    }
                }
            }
        }
    }

    private void skipWhitespaces() {
        while (position < template.length() && Character.isWhitespace(template.charAt(position))) {
            position++;
        }
    }

    private boolean isReferenceChar(char c) {
        return Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-';
    }

    private void resolveAndAppend(String scope, String reference, @NotNull List<String> parameters) {
        String resolved = resolver.resolve(scope, reference, parameters);
        if (resolved != null) {
            result.append(resolved);
        } else if (errorOnUnresolved) {
            throw new IllegalStateException("Reference '" + reference + "' in scope '" + scope + "' couldn't be resolved");
        } else {
            // silently ignore
        }
    }
}
