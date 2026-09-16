/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas.ad;

import java.io.IOException;
import java.util.List;

import com.evolveum.midpoint.util.exception.*;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.smart.impl.wellknownschemas.SystemMappingSuggestion;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaProvider;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaTestBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

public class OrganizationalUnitActiveDirectoryMappingProviderTest extends WellKnownSchemaTestBase {

    protected OrganizationalUnitActiveDirectoryMappingProviderTest() throws SchemaException, IOException, SAXException {
    }

    @Test
    void shadowContainsDistinguishedName_outboundMappingsAreSuggested_suggestedScriptShouldBeCorrect()
            throws CommonException {
        final WellKnownSchemaProvider mappingProvider = new OrganizationalUnitActiveDirectoryMappingProvider();
        final List<SystemMappingSuggestion> systemMappingSuggestions = mappingProvider.suggestOutboundMappings(
                List.of(shadowWithAttribute("distinguishedName", "ou=admins,dc=example,dc=com")));
        final ExpressionType expression = getExpression(systemMappingSuggestions);
        final String output = evaluateExpression(expression, "name", "admins");

        Assert.assertEquals(output, "ou=admins,dc=example,dc=com");
    }
}
