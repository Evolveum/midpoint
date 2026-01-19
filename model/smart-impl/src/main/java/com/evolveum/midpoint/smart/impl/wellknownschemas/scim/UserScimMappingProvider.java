/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas.scim;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.smart.impl.wellknownschemas.SystemMappingSuggestion;
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
    public List<SystemMappingSuggestion> suggestInboundMappings() {
        List<SystemMappingSuggestion> mappings = new ArrayList<>();
        mappings.add(WellKnownSchemaProvider.createSystemMappingSuggestion("userName", UserType.F_NAME, null, null));
        mappings.add(WellKnownSchemaProvider.createSystemMappingSuggestion("displayName", UserType.F_FULL_NAME, null, null));
        mappings.add(WellKnownSchemaProvider.createSystemMappingSuggestion("givenName", UserType.F_GIVEN_NAME, null, null));
        mappings.add(WellKnownSchemaProvider.createSystemMappingSuggestion("familyName", UserType.F_FAMILY_NAME, null, null));
        mappings.add(WellKnownSchemaProvider.createSystemMappingSuggestion("emailAddress", UserType.F_EMAIL_ADDRESS, null, null));
        mappings.add(WellKnownSchemaProvider.createSystemMappingSuggestion("phoneNumber", UserType.F_TELEPHONE_NUMBER, null, null));
        return mappings;
    }

    @Override
    public List<SystemMappingSuggestion> suggestOutboundMappings() {
        List<SystemMappingSuggestion> mappings = new ArrayList<>();
        mappings.add(WellKnownSchemaProvider.createSystemMappingSuggestion("userName", UserType.F_NAME, null, null));
        mappings.add(WellKnownSchemaProvider.createSystemMappingSuggestion("displayName", UserType.F_FULL_NAME, null, null));
        mappings.add(WellKnownSchemaProvider.createSystemMappingSuggestion("givenName", UserType.F_GIVEN_NAME, null, null));
        mappings.add(WellKnownSchemaProvider.createSystemMappingSuggestion("familyName", UserType.F_FAMILY_NAME, null, null));
        mappings.add(WellKnownSchemaProvider.createSystemMappingSuggestion("emailAddress", UserType.F_EMAIL_ADDRESS, null, null));
        mappings.add(WellKnownSchemaProvider.createSystemMappingSuggestion("phoneNumber", UserType.F_TELEPHONE_NUMBER, null, null));
        return mappings;
    }
}
