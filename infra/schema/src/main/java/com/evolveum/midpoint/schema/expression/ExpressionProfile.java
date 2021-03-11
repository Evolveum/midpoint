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
import com.evolveum.midpoint.util.QNameUtil;

/**
 * NOTE: This is pretty much throw-away implementation. Just the interface is important now.
 *
 * @author Radovan Semancik
 *
 */
public class ExpressionProfile implements Serializable { // TODO: DebugDumpable

    private final String identifier;
    private AccessDecision decision;
    private final List<ExpressionEvaluatorProfile> evaluatorProfiles = new ArrayList<>();

    public ExpressionProfile(String identifier) {
        super();
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public AccessDecision getDecision() {
        return decision;
    }

    public void setDecision(AccessDecision defaultDecision) {
        this.decision = defaultDecision;
    }

    public void add(ExpressionEvaluatorProfile evaluatorProfile) {
        evaluatorProfiles.add(evaluatorProfile);
    }

    public ExpressionEvaluatorProfile getEvaluatorProfile(QName type) {
        for (ExpressionEvaluatorProfile evaluatorProfile : evaluatorProfiles) {
            if (QNameUtil.match(evaluatorProfile.getType(), type)) {
                return evaluatorProfile;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "ExpressionProfile(" + identifier + ")";
    }
}
