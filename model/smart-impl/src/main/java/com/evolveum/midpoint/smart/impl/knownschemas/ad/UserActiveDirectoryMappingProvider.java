/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.knownschemas.ad;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.smart.impl.knownschemas.KnownSchemaMappingProvider;
import com.evolveum.midpoint.smart.impl.knownschemas.KnownSchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class UserActiveDirectoryMappingProvider implements KnownSchemaMappingProvider {

    @Override
    public KnownSchemaType getSupportedSchemaType() {
        return KnownSchemaType.AD_USER;
    }

    @Override
    public Map<ItemPath, ItemPath> getSchemaMatches() {
        Map<ItemPath, ItemPath> matches = new LinkedHashMap<>();
        matches.put(ItemPath.create("sAMAccountName"), UserType.F_NAME);
        matches.put(ItemPath.create("cn"), UserType.F_FULL_NAME);
        matches.put(ItemPath.create("givenName"), UserType.F_GIVEN_NAME);
        matches.put(ItemPath.create("sn"), UserType.F_FAMILY_NAME);
        matches.put(ItemPath.create("mail"), UserType.F_EMAIL_ADDRESS);
        return matches;
    }

    @Override
    public List<AttributeMappingsSuggestionType> getInboundMappings() {
        List<AttributeMappingsSuggestionType> mappings = new ArrayList<>();
        mappings.add(createInboundMapping("sAMAccountName", UserType.F_NAME, "AD sAMAccountName to midPoint name", null));
        mappings.add(createInboundMapping("cn", UserType.F_FULL_NAME, "AD cn (common name) to midPoint fullName", null));
        mappings.add(createInboundMapping("givenName", UserType.F_GIVEN_NAME, "AD givenName to midPoint givenName", null));
        mappings.add(createInboundMapping("sn", UserType.F_FAMILY_NAME, "AD sn (surname) to midPoint familyName", null));
        mappings.add(createInboundMapping("mail", UserType.F_EMAIL_ADDRESS, "AD mail to midPoint emailAddress", null));
        return mappings;
    }

    @Override
    public List<AttributeMappingsSuggestionType> getOutboundMappings() {
        List<AttributeMappingsSuggestionType> mappings = new ArrayList<>();
        mappings.add(createOutboundMapping("sAMAccountName", UserType.F_NAME, "midPoint name to AD sAMAccountName", null));
        mappings.add(createOutboundMapping("cn", UserType.F_FULL_NAME, "midPoint fullName to AD cn (common name)", null));
        mappings.add(createOutboundMapping("givenName", UserType.F_GIVEN_NAME, "midPoint givenName to AD givenName", null));
        mappings.add(createOutboundMapping("sn", UserType.F_FAMILY_NAME, "midPoint familyName to AD sn (surname)", null));
        mappings.add(createOutboundMapping("mail", UserType.F_EMAIL_ADDRESS, "midPoint emailAddress to AD mail", null));
        return mappings;
    }

    private AttributeMappingsSuggestionType createInboundMapping(
            String shadowAttrName,
            ItemPath focusPropertyPath,
            String description,
            @Nullable ExpressionType expression) {
        var inboundMapping = new InboundMappingType()
                .name(shadowAttrName + "-into-" + focusPropertyPath.lastName())
                .description(description)
                .strength(MappingStrengthType.STRONG)
                .expression(expression)
                .target(new VariableBindingDefinitionType().path(focusPropertyPath.toBean()));

        return new AttributeMappingsSuggestionType()
                .expectedQuality(null)
                .definition(new ResourceAttributeDefinitionType()
                        .inbound(inboundMapping));
    }

    private AttributeMappingsSuggestionType createOutboundMapping(
            String shadowAttrName,
            ItemPath focusPropertyPath,
            String description,
            @Nullable ExpressionType expression) {
        var outboundMapping = new OutboundMappingType()
                .name(focusPropertyPath.lastName() + "-to-" + shadowAttrName)
                .description(description)
                .strength(MappingStrengthType.STRONG)
                .expression(expression)
                .source(new VariableBindingDefinitionType().path(focusPropertyPath.toBean()));

        return new AttributeMappingsSuggestionType()
                .expectedQuality(null)
                .definition(new ResourceAttributeDefinitionType()
                        .outbound(outboundMapping));
    }

}
