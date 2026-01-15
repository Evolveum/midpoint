/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.knownschemas.scim;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.smart.impl.knownschemas.KnownSchemaMappingProvider;
import com.evolveum.midpoint.smart.impl.knownschemas.KnownSchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class UserScimMappingProvider implements KnownSchemaMappingProvider {

    @Override
    public KnownSchemaType getSupportedSchemaType() {
        return KnownSchemaType.SCIM_2_0_USER;
    }

    @Override
    public Map<ItemPath, ItemPath> getSchemaMatches() {
        Map<ItemPath, ItemPath> matches = new LinkedHashMap<>();
        matches.put(ItemPath.create("userName"), UserType.F_NAME);
        matches.put(ItemPath.create("displayName"), UserType.F_FULL_NAME);
        matches.put(ItemPath.create("givenName"), UserType.F_GIVEN_NAME);
        matches.put(ItemPath.create("familyName"), UserType.F_FAMILY_NAME);
        matches.put(ItemPath.create("emailAddress"), UserType.F_EMAIL_ADDRESS);
        matches.put(ItemPath.create("phoneNumber"), UserType.F_TELEPHONE_NUMBER);
        return matches;
    }

    @Override
    public List<AttributeMappingsSuggestionType> getInboundMappings() {
        List<AttributeMappingsSuggestionType> mappings = new ArrayList<>();
        mappings.add(createInboundMapping("userName", UserType.F_NAME, "SCIM userName to midPoint name", null));
        mappings.add(createInboundMapping("displayName", UserType.F_FULL_NAME, "SCIM displayName to midPoint fullName", null));
        mappings.add(createInboundMapping("givenName", UserType.F_GIVEN_NAME, "SCIM givenName to midPoint givenName", null));
        mappings.add(createInboundMapping("familyName", UserType.F_FAMILY_NAME, "SCIM familyName to midPoint familyName", null));
        mappings.add(createInboundMapping("emailAddress", UserType.F_EMAIL_ADDRESS, "SCIM emailAddress to midPoint emailAddress", null));
        mappings.add(createInboundMapping("phoneNumber", UserType.F_TELEPHONE_NUMBER, "SCIM phoneNumber to midPoint telephoneNumber", null));
        return mappings;
    }

    @Override
    public List<AttributeMappingsSuggestionType> getOutboundMappings() {
        List<AttributeMappingsSuggestionType> mappings = new ArrayList<>();
        mappings.add(createOutboundMapping("userName", UserType.F_NAME, "midPoint name to SCIM userName", null));
        mappings.add(createOutboundMapping("displayName", UserType.F_FULL_NAME, "midPoint fullName to SCIM displayName", null));
        mappings.add(createOutboundMapping("givenName", UserType.F_GIVEN_NAME, "midPoint givenName to SCIM givenName", null));
        mappings.add(createOutboundMapping("familyName", UserType.F_FAMILY_NAME, "midPoint familyName to SCIM familyName", null));
        mappings.add(createOutboundMapping("emailAddress", UserType.F_EMAIL_ADDRESS, "midPoint emailAddress to SCIM emailAddress", null));
        mappings.add(createOutboundMapping("phoneNumber", UserType.F_TELEPHONE_NUMBER, "midPoint telephoneNumber to SCIM phoneNumber", null));
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
