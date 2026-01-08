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
    public List<InboundMappingType> getInboundMappings() {
        List<InboundMappingType> mappings = new ArrayList<>();
        mappings.add(createInboundMapping("userName", UserType.F_NAME, "SCIM userName to midPoint name", null));
        mappings.add(createInboundMapping("displayName", UserType.F_FULL_NAME, "SCIM displayName to midPoint fullName", null));
        mappings.add(createInboundMapping("givenName", UserType.F_GIVEN_NAME, "SCIM givenName to midPoint givenName", null));
        mappings.add(createInboundMapping("familyName", UserType.F_FAMILY_NAME, "SCIM familyName to midPoint familyName", null));
        mappings.add(createInboundMapping("emailAddress", UserType.F_EMAIL_ADDRESS, "SCIM emailAddress to midPoint emailAddress", null));
        mappings.add(createInboundMapping("phoneNumber", UserType.F_TELEPHONE_NUMBER, "SCIM phoneNumber to midPoint telephoneNumber", null));
        return mappings;
    }

    private InboundMappingType createInboundMapping(
            String shadowAttrName,
            ItemPath focusPropertyPath,
            String description,
            @Nullable ExpressionType expression) {
        return new InboundMappingType()
                .name(shadowAttrName + "-into-" + focusPropertyPath.lastName())
                .description(description)
                .strength(MappingStrengthType.STRONG)
                .expression(expression)
                .target(new VariableBindingDefinitionType().path(focusPropertyPath.toBean()));
    }

}
