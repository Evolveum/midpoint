/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.impl.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.smart.impl.mappings.MappingScriptTestBase;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class WellKnownSchemaTestBase extends MappingScriptTestBase {

    protected WellKnownSchemaTestBase() throws SchemaException, IOException, SAXException {
    }

    protected static ShadowType shadowWithAttribute(String name, String value) throws SchemaException {
        final ShadowAttributesType attributes = new ShadowAttributesType();
        final PrismContainerValue<?> prismAttributes = attributes.asPrismContainerValue();
        prismAttributes.findOrCreateProperty(new PrismPropertyDefinitionImpl<String>(QName.valueOf(name),
                        QName.valueOf("String")))
                .setRealValue(value);
        return new ShadowType().attributes(attributes);
    }

    protected static ExpressionType getExpression(List<SystemMappingSuggestion> systemMappingSuggestions) {
        return systemMappingSuggestions.stream()
                .map(SystemMappingSuggestion::expression)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(AssertionError::new);
    }

    /**
     * Returns the expression of the suggestion targeting the given shadow attribute local name.
     * Useful when a provider suggests scripts for multiple attributes (e.g. both "dn" and "cn").
     */
    protected static ExpressionType getExpression(List<SystemMappingSuggestion> systemMappingSuggestions, String shadowAttrLocalName) {
        return systemMappingSuggestions.stream()
                .filter(s -> s.expression() != null
                        && s.shadowAttributePath().lastName() != null
                        && shadowAttrLocalName.equals(s.shadowAttributePath().lastName().getLocalPart()))
                .map(SystemMappingSuggestion::expression)
                .findFirst()
                .orElseThrow(AssertionError::new);
    }

    /**
     * Returns the raw script code of the (single) script evaluator contained in the expression.
     */
    protected static String getScriptCode(ExpressionType expression) {
        return expression.getExpressionEvaluator().stream()
                .filter(e -> e.getValue() instanceof ScriptExpressionEvaluatorType)
                .map(e -> ((ScriptExpressionEvaluatorType) e.getValue()).getCode())
                .findFirst()
                .orElseThrow(AssertionError::new);
    }

}
