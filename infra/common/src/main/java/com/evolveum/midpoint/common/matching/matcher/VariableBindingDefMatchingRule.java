/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.common.matching.matcher;

import com.evolveum.midpoint.common.matching.normalizer.VariableBindingDefNormalizer;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.normalization.Normalizer;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

public class VariableBindingDefMatchingRule implements MatchingRule<VariableBindingDefinitionType> {

    @Override
    public QName getName() {
        return PrismConstants.VARIABLE_BINDING_DEF_MATCHING_RULE_NAME;
    }

    @Override
    public boolean supports(@NotNull QName xsdType) {
        return xsdType.equals(VariableBindingDefinitionType.COMPLEX_TYPE);
    }

    @Override
    public VariableBindingDefinitionType normalize(VariableBindingDefinitionType original) throws SchemaException {
        return MatchingRule.super.normalize(original);
    }

    @Override
    public @NotNull Normalizer<VariableBindingDefinitionType> getNormalizer() {
        return new VariableBindingDefNormalizer();
    }

    @Override
    public boolean match(VariableBindingDefinitionType a, VariableBindingDefinitionType b) throws SchemaException {
        return MatchingRule.super.match(a, b);
    }

    @Override
    public String toString() {
        return "VariableBindingDefMatchingRule{}";
    }
}
