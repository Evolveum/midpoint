/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.AccessDecision;

/**
 * @author Radovan Semancik
 *
 */
public class ExpressionEvaluatorProfile {

    private final QName type;
    private AccessDecision decision;
    private final List<ScriptExpressionProfile> scritpProfiles = new ArrayList<>();

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
        scritpProfiles.add(scriptProfile);
    }

    public ScriptExpressionProfile getScriptExpressionProfile(String language) {
        for(ScriptExpressionProfile scritpProfile : scritpProfiles) {
            if (language.equals(scritpProfile.getLanguage())) {
                return scritpProfile;
            }
        }
        return null;
    }


}
