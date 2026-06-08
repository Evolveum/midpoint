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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

}
