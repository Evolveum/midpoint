/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas.ad;

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
public class UserActiveDirectoryMappingProvider implements WellKnownSchemaProvider {

    @Override
    public WellKnownSchemaType getSupportedSchemaType() {
        return com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaType.AD_USER;
    }

    @Override
    public Map<ItemPath, ItemPath> suggestSchemaMatches() {
        Map<ItemPath, ItemPath> matches = new HashMap<>();
        matches.put(ItemPath.create("sAMAccountName"), UserType.F_NAME);
        matches.put(ItemPath.create("cn"), UserType.F_FULL_NAME);
        matches.put(ItemPath.create("givenName"), UserType.F_GIVEN_NAME);
        matches.put(ItemPath.create("sn"), UserType.F_FAMILY_NAME);
        matches.put(ItemPath.create("mail"), UserType.F_EMAIL_ADDRESS);
        return matches;
    }

    @Override
    public List<AttributeMappingsSuggestionType> suggestInboundMappings() {
        List<AttributeMappingsSuggestionType> mappings = new ArrayList<>();
        mappings.add(WellKnownSchemaProvider.createInboundMapping("sAMAccountName", UserType.F_NAME, "AD sAMAccountName to midPoint name", null));
        mappings.add(WellKnownSchemaProvider.createInboundMapping("cn", UserType.F_FULL_NAME, "AD cn (common name) to midPoint fullName", null));
        mappings.add(WellKnownSchemaProvider.createInboundMapping("givenName", UserType.F_GIVEN_NAME, "AD givenName to midPoint givenName", null));
        mappings.add(WellKnownSchemaProvider.createInboundMapping("sn", UserType.F_FAMILY_NAME, "AD sn (surname) to midPoint familyName", null));
        mappings.add(WellKnownSchemaProvider.createInboundMapping("mail", UserType.F_EMAIL_ADDRESS, "AD mail to midPoint emailAddress", null));
        return mappings;
    }

    @Override
    public List<AttributeMappingsSuggestionType> suggestOutboundMappings() {
        List<AttributeMappingsSuggestionType> mappings = new ArrayList<>();
        mappings.add(WellKnownSchemaProvider.createOutboundMapping("sAMAccountName", UserType.F_NAME, "midPoint name to AD sAMAccountName", null));
        mappings.add(WellKnownSchemaProvider.createOutboundMapping("cn", UserType.F_FULL_NAME, "midPoint fullName to AD cn (common name)", null));
        mappings.add(WellKnownSchemaProvider.createOutboundMapping("givenName", UserType.F_GIVEN_NAME, "midPoint givenName to AD givenName", null));
        mappings.add(WellKnownSchemaProvider.createOutboundMapping("sn", UserType.F_FAMILY_NAME, "midPoint familyName to AD sn (surname)", null));
        mappings.add(WellKnownSchemaProvider.createOutboundMapping("mail", UserType.F_EMAIL_ADDRESS, "midPoint emailAddress to AD mail", null));
        return mappings;
    }
}
