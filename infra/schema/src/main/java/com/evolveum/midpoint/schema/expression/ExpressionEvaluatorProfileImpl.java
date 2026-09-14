/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.expression;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.AccessDecision;

import com.evolveum.midpoint.schema.SchemaConstantsGenerated;

import com.evolveum.midpoint.util.QNameUtil;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

/**
 * Specifies limitations on the use of a particular expression evaluator (e.g. `script`, `path`, `value`, etc).
 *
 * @author Radovan Semancik
 */
public class ExpressionEvaluatorProfileImpl implements ExpressionEvaluatorProfile {

    /**
     * Type of the expression evaluator, given by the name of XML element for the evaluator bean,
     * e.g. {@link SchemaConstantsGenerated#C_SCRIPT}. Must be qualified.
     */
    @NotNull private final QName type;

    /** @see ExpressionEvaluatorProfile#getDefaultDecision() */
    @NotNull private final AccessDecision defaultDecision;

    /** Scripting language profiles, keyed by [full] language URI. Currently applicable only for `script` evaluator. */
    @NotNull private final Map<String, ScriptLanguageExpressionProfile> scriptLanguageProfiles;

    public ExpressionEvaluatorProfileImpl(
            @NotNull QName type,
            @NotNull AccessDecision defaultDecision,
            @NotNull List<ScriptLanguageExpressionProfileImpl> scriptLanguageProfiles) {
        Preconditions.checkArgument(
                QNameUtil.isQualified(type),
                "Expression evaluator type must be qualified: %s", type);
        this.type = type;
        this.defaultDecision = defaultDecision;
        this.scriptLanguageProfiles =
                scriptLanguageProfiles.stream()
                        .collect(Collectors.toUnmodifiableMap(p -> p.getLanguage(), p -> p));
    }

    public @NotNull QName getType() {
        return type;
    }

    @Override
    public @NotNull AccessDecision getDefaultDecision() {
        return defaultDecision;
    }

    @Override
    public @NotNull ScriptLanguageExpressionProfile getScriptLanguageExpressionProfile(@NotNull String qualifiedLanguageUri) {
        return Objects.requireNonNullElseGet(
                scriptLanguageProfiles.get(qualifiedLanguageUri),
                () -> ScriptLanguageExpressionProfile.forDecision(defaultDecision));
    }
}
