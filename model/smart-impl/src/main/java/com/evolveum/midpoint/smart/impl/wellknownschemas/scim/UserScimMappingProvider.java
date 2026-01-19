/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas.scim;

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
public class UserScimMappingProvider implements WellKnownSchemaProvider {

    @Override
    public WellKnownSchemaType getSupportedSchemaType() {
        return com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaType.SCIM_2_0_USER;
    }

    @Override
    public Map<ItemPath, ItemPath> suggestSchemaMatches() {
        Map<ItemPath, ItemPath> matches = new HashMap<>();
        matches.put(ItemPath.create("userName"), UserType.F_NAME);
        matches.put(ItemPath.create("displayName"), UserType.F_FULL_NAME);
        matches.put(ItemPath.create("givenName"), UserType.F_GIVEN_NAME);
        matches.put(ItemPath.create("familyName"), UserType.F_FAMILY_NAME);
        matches.put(ItemPath.create("emailAddress"), UserType.F_EMAIL_ADDRESS);
        matches.put(ItemPath.create("phoneNumber"), UserType.F_TELEPHONE_NUMBER);
        return matches;
    }

    @Override
    public List<AttributeMappingsSuggestionType> suggestInboundMappings() {
        List<AttributeMappingsSuggestionType> mappings = new ArrayList<>();
        mappings.add(WellKnownSchemaProvider.createInboundMapping("userName", UserType.F_NAME, "SCIM userName to midPoint name", null));
        mappings.add(WellKnownSchemaProvider.createInboundMapping("displayName", UserType.F_FULL_NAME, "SCIM displayName to midPoint fullName", null));
        mappings.add(WellKnownSchemaProvider.createInboundMapping("givenName", UserType.F_GIVEN_NAME, "SCIM givenName to midPoint givenName", null));
        mappings.add(WellKnownSchemaProvider.createInboundMapping("familyName", UserType.F_FAMILY_NAME, "SCIM familyName to midPoint familyName", null));
        mappings.add(WellKnownSchemaProvider.createInboundMapping("emailAddress", UserType.F_EMAIL_ADDRESS, "SCIM emailAddress to midPoint emailAddress", null));
        mappings.add(WellKnownSchemaProvider.createInboundMapping("phoneNumber", UserType.F_TELEPHONE_NUMBER, "SCIM phoneNumber to midPoint telephoneNumber", null));
        return mappings;
    }

    @Override
    public List<AttributeMappingsSuggestionType> suggestOutboundMappings() {
        List<AttributeMappingsSuggestionType> mappings = new ArrayList<>();
        mappings.add(WellKnownSchemaProvider.createOutboundMapping("userName", UserType.F_NAME, "midPoint name to SCIM userName", null));
        mappings.add(WellKnownSchemaProvider.createOutboundMapping("displayName", UserType.F_FULL_NAME, "midPoint fullName to SCIM displayName", null));
        mappings.add(WellKnownSchemaProvider.createOutboundMapping("givenName", UserType.F_GIVEN_NAME, "midPoint givenName to SCIM givenName", null));
        mappings.add(WellKnownSchemaProvider.createOutboundMapping("familyName", UserType.F_FAMILY_NAME, "midPoint familyName to SCIM familyName", null));
        mappings.add(WellKnownSchemaProvider.createOutboundMapping("emailAddress", UserType.F_EMAIL_ADDRESS, "midPoint emailAddress to SCIM emailAddress", null));
        mappings.add(WellKnownSchemaProvider.createOutboundMapping("phoneNumber", UserType.F_TELEPHONE_NUMBER, "midPoint telephoneNumber to SCIM phoneNumber", null));
        return mappings;
    }
}
