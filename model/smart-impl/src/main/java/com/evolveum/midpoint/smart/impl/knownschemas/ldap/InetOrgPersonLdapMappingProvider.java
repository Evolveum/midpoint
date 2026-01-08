/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.knownschemas.ldap;

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
public class InetOrgPersonLdapMappingProvider implements KnownSchemaMappingProvider {

    @Override
    public KnownSchemaType getSupportedSchemaType() {
        return KnownSchemaType.LDAP_INETORGPERSON;
    }

    @Override
    public Map<ItemPath, ItemPath> getSchemaMatches() {
        Map<ItemPath, ItemPath> matches = new LinkedHashMap<>();
        matches.put(ItemPath.create("uid"), UserType.F_NAME);
        matches.put(ItemPath.create("cn"), UserType.F_FULL_NAME);
        matches.put(ItemPath.create("givenName"), UserType.F_GIVEN_NAME);
        matches.put(ItemPath.create("sn"), UserType.F_FAMILY_NAME);
        matches.put(ItemPath.create("mail"), UserType.F_EMAIL_ADDRESS);
        matches.put(ItemPath.create("telephoneNumber"), UserType.F_TELEPHONE_NUMBER);
        return matches;
    }

    @Override
    public List<InboundMappingType> getInboundMappings() {
        List<InboundMappingType> mappings = new ArrayList<>();
        mappings.add(createInboundMapping("uid", UserType.F_NAME, "LDAP uid to midPoint name", null));
        mappings.add(createInboundMapping("cn", UserType.F_FULL_NAME, "LDAP cn (common name) to midPoint fullName", null));
        mappings.add(createInboundMapping("givenName", UserType.F_GIVEN_NAME, "LDAP givenName to midPoint givenName", null));
        mappings.add(createInboundMapping("sn", UserType.F_FAMILY_NAME, "LDAP sn (surname) to midPoint familyName", null));
        mappings.add(createInboundMapping("mail", UserType.F_EMAIL_ADDRESS, "LDAP mail to midPoint emailAddress", null));
        mappings.add(createInboundMapping("telephoneNumber", UserType.F_TELEPHONE_NUMBER, "LDAP telephoneNumber to midPoint telephoneNumber", null));
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
