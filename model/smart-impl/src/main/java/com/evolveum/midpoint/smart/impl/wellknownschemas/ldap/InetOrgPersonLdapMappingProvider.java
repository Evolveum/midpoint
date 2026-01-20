/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas.ldap;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaProvider;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InetOrgPersonLdapMappingProvider implements WellKnownSchemaProvider {

    @Override
    public WellKnownSchemaType getSupportedSchemaType() {
        return com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaType.LDAP_INETORGPERSON;
    }

    @Override
    public Map<ItemPath, ItemPath> suggestSchemaMatches() {
        Map<ItemPath, ItemPath> matches = new HashMap<>();
        matches.put(ItemPath.create("uid"), UserType.F_NAME);
        matches.put(ItemPath.create("cn"), UserType.F_FULL_NAME);
        matches.put(ItemPath.create("givenName"), UserType.F_GIVEN_NAME);
        matches.put(ItemPath.create("sn"), UserType.F_FAMILY_NAME);
        matches.put(ItemPath.create("mail"), UserType.F_EMAIL_ADDRESS);
        matches.put(ItemPath.create("telephoneNumber"), UserType.F_TELEPHONE_NUMBER);
        return matches;
    }

    @Override
    public List<AttributeMappingsSuggestionType> suggestInboundMappings() {
        List<AttributeMappingsSuggestionType> mappings = new ArrayList<>();
        mappings.add(WellKnownSchemaProvider.createInboundMapping("uid", UserType.F_NAME, "LDAP uid to midPoint name", null));
        mappings.add(WellKnownSchemaProvider.createInboundMapping("cn", UserType.F_FULL_NAME, "LDAP cn (common name) to midPoint fullName", null));
        mappings.add(WellKnownSchemaProvider.createInboundMapping("givenName", UserType.F_GIVEN_NAME, "LDAP givenName to midPoint givenName", null));
        mappings.add(WellKnownSchemaProvider.createInboundMapping("sn", UserType.F_FAMILY_NAME, "LDAP sn (surname) to midPoint familyName", null));
        mappings.add(WellKnownSchemaProvider.createInboundMapping("mail", UserType.F_EMAIL_ADDRESS, "LDAP mail to midPoint emailAddress", null));
        mappings.add(WellKnownSchemaProvider.createInboundMapping("telephoneNumber", UserType.F_TELEPHONE_NUMBER, "LDAP telephoneNumber to midPoint telephoneNumber", null));
        return mappings;
    }

    @Override
    public List<AttributeMappingsSuggestionType> suggestOutboundMappings() {
        List<AttributeMappingsSuggestionType> mappings = new ArrayList<>();
        mappings.add(WellKnownSchemaProvider.createOutboundMapping("uid", UserType.F_NAME, "midPoint name to LDAP uid", null));
        mappings.add(WellKnownSchemaProvider.createOutboundMapping("cn", UserType.F_FULL_NAME, "midPoint fullName to LDAP cn (common name)", null));
        mappings.add(WellKnownSchemaProvider.createOutboundMapping("givenName", UserType.F_GIVEN_NAME, "midPoint givenName to LDAP givenName", null));
        mappings.add(WellKnownSchemaProvider.createOutboundMapping("sn", UserType.F_FAMILY_NAME, "midPoint familyName to LDAP sn (surname)", null));
        mappings.add(WellKnownSchemaProvider.createOutboundMapping("mail", UserType.F_EMAIL_ADDRESS, "midPoint emailAddress to LDAP mail", null));
        mappings.add(WellKnownSchemaProvider.createOutboundMapping("telephoneNumber", UserType.F_TELEPHONE_NUMBER, "midPoint telephoneNumber to LDAP telephoneNumber", null));
        return mappings;
    }
}
