/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas.ad;

import java.io.IOException;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.smart.impl.wellknownschemas.SystemMappingSuggestion;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaProvider;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaTestBase;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

public class UserActiveDirectoryMappingProviderTest extends WellKnownSchemaTestBase {

    protected UserActiveDirectoryMappingProviderTest() throws SchemaException, IOException, SAXException {
    }

    @Test
    void shadowContainsCn_outboundMappingsAreSuggested_suggestedScriptShouldBeCorrect()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final WellKnownSchemaProvider mappingProvider = new UserActiveDirectoryMappingProvider();
        final List<SystemMappingSuggestion> systemMappingSuggestions = mappingProvider.suggestOutboundMappings(
                List.of(shadowWithAttribute("distinguishedName", "cn=Alice Baker,ou=users,dc=example,dc=com")));
        final ExpressionType expression = getExpression(systemMappingSuggestions);
        final String output = evaluateExpression(expression, "fullName", "Johny English");

        Assert.assertEquals(output, "cn=Johny English,ou=users,dc=example,dc=com");
    }

    @Test
    void shadowContainsUpn_outboundMappingsAreSuggested_suggestedScriptShouldBeCorrect()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final WellKnownSchemaProvider mappingProvider = new UserActiveDirectoryMappingProvider();
        final List<SystemMappingSuggestion> systemMappingSuggestions = mappingProvider.suggestOutboundMappings(
                List.of(shadowWithAttribute("userPrincipalName", "alice.baker@example.com")));
        final ExpressionType expression = getExpression(systemMappingSuggestions);
        final String output = evaluateExpression(expression, "name", "jenglish");

        Assert.assertEquals(output, "jenglish@example.com");
    }
}