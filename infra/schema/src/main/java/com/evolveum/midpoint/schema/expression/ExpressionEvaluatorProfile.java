/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.AccessDecision;

import com.evolveum.midpoint.schema.SchemaConstantsGenerated;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Specifies limitations on the use of a particular expression evaluator (e.g. script, path, value, etc).
 *
 * @author Radovan Semancik
 */
public class ExpressionEvaluatorProfile implements Serializable {

    /** Type of the expression evaluator, e.g. {@link SchemaConstantsGenerated#C_SCRIPT}. Beware, it may be unqualified. */
    @NotNull private final QName type;

    @NotNull private final AccessDecision decision;

    /** Scripting language profiles, keyed by [full] language URI. Currently applicable only for `script` evaluator. */
    @NotNull private final Map<String, ScriptExpressionProfile> scriptProfiles;

    public ExpressionEvaluatorProfile(
            @NotNull QName type,
            @NotNull AccessDecision decision,
            @NotNull List<ScriptExpressionProfile> scriptProfiles) {
        this.type = type;
        this.decision = decision;
        this.scriptProfiles =
                scriptProfiles.stream()
                        .collect(Collectors.toUnmodifiableMap(p -> p.getLanguage(), p -> p));
    }

    public @NotNull QName getType() {
        return type;
    }

    public @NotNull AccessDecision getDecision() {
        return decision;
    }

    public @Nullable ScriptExpressionProfile getScriptExpressionProfile(@NotNull String language) {
        return scriptProfiles.get(language);
    }
}
