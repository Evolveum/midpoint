/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.AccessDecision;

/**
 * @author Radovan Semancik
 *
 */
public class ExpressionEvaluatorProfile implements Serializable {

    private final QName type;
    private AccessDecision decision;
    private final List<ScriptExpressionProfile> scriptProfiles = new ArrayList<>();

    public ExpressionEvaluatorProfile(QName type) {
        this.type = type;
    }

    public QName getType() {
        return type;
    }

    public AccessDecision getDecision() {
        return decision;
    }

    public void setDecision(AccessDecision decision) {
        this.decision = decision;
    }

    public void add(ScriptExpressionProfile scriptProfile) {
        scriptProfiles.add(scriptProfile);
    }

    public ScriptExpressionProfile getScriptExpressionProfile(String language) {
        for (ScriptExpressionProfile scriptProfile : scriptProfiles) {
            if (language.equals(scriptProfile.getLanguage())) {
                return scriptProfile;
            }
        }
        return null;
    }
}
