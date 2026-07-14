/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas.ldap;

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

public class GroupOfNamesLdapMappingProviderTest extends WellKnownSchemaTestBase {

    protected GroupOfNamesLdapMappingProviderTest() throws SchemaException, IOException, SAXException {
    }

    @Test
    void shadowContainsOuSuffix_outboundMappingsAreSuggested_suggestedScriptShouldBeCorrect()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final WellKnownSchemaProvider mappingProvider = new GroupOfNamesLdapMappingProvider();
        final List<SystemMappingSuggestion> systemMappingSuggestions = mappingProvider.suggestOutboundMappings(
                List.of(shadowWithAttribute("dn", "cn=app:customer-conversion:specs,ou=appgroups,dc=example,dc=com")));
        final ExpressionType expression = getExpression(systemMappingSuggestions);
        final String output = evaluateExpression(expression, "identifier", "app:customer-conversion:developers");

        Assert.assertEquals(output, "cn=app:customer-conversion:developers,ou=appgroups,dc=example,dc=com");
    }

    @Test
    void resourceNameIsProvided_inboundMappingsAreSuggested_suggestedScriptShouldBeCorrect()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final WellKnownSchemaProvider mappingProvider = new GroupOfNamesLdapMappingProvider();
        final List<SystemMappingSuggestion> systemMappingSuggestions = mappingProvider.suggestInboundMappings("ldap");
        final ExpressionType expression = getExpression(systemMappingSuggestions);
        final String output = evaluateExpression(expression, "input", "app:customer-conversions:developers");

        Assert.assertEquals(output, "ldap-app:customer-conversions:developers");
    }

    @Test
    void shadowContainsOuSuffix_outboundMappingsAreSuggested_dnAndCnScriptsShouldUseIterationToken() throws SchemaException {
        final WellKnownSchemaProvider mappingProvider = new GroupOfNamesLdapMappingProvider();
        final List<SystemMappingSuggestion> systemMappingSuggestions = mappingProvider.suggestOutboundMappings(
                List.of(shadowWithAttribute("dn", "cn=app:customer-conversion:specs,ou=appgroups,dc=example,dc=com")));

        final String dnScript = getScriptCode(getExpression(systemMappingSuggestions, "dn"));
        final String cnScript = getScriptCode(getExpression(systemMappingSuggestions, "cn"));

        Assert.assertTrue(dnScript.contains("iterationToken"),
                "dn mapped from identifier should use iterationToken, but was: " + dnScript);
        Assert.assertTrue(cnScript.contains("iterationToken"),
                "cn mapped from identifier should use iterationToken, but was: " + cnScript);
    }
}
