/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas.scim;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaDetector;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class UserScimSchemaDetector implements WellKnownSchemaDetector {

    private static final Set<String> SCIM_CONNECTOR_TYPES = Set.of(
            "com.evolveum.polygon.connector.scim.ScimConnector",
            "ScimConnector"
    );

    private static final Set<String> SCIM_USER_ATTRIBUTES_LOWERCASE = Set.of(
            "username",
            "displayname",
            "givenname",
            "familyname",
            "emailaddress",
            "phonenumber"
    );

    @Override
    public Optional<WellKnownSchemaType> detectSchemaType(ResourceType resource, ResourceObjectTypeDefinition typeDefinition) {
        var objectClassDef = typeDefinition.getObjectClassDefinition();
        String objectClassName = objectClassDef.getTypeName().getLocalPart();

        if (!"user".equalsIgnoreCase(objectClassName)) {
            return Optional.empty();
        }

        var connectorRef = resource.getConnectorRef();
        if (connectorRef != null && connectorRef.getOid() != null) {
            String connectorType = connectorRef.getType() != null ? connectorRef.getType().getLocalPart() : "";

            if (SCIM_CONNECTOR_TYPES.stream().anyMatch(type -> connectorType.contains(type))) {
                return Optional.of(WellKnownSchemaType.SCIM_2_0_USER);
            }
        }

        Set<String> attributeNames = typeDefinition.getAttributeDefinitions().stream()
                .map(def -> def.getItemName().getLocalPart().toLowerCase())
                .collect(Collectors.toSet());

        if (attributeNames.containsAll(SCIM_USER_ATTRIBUTES_LOWERCASE)) {
            return Optional.of(com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaType.SCIM_2_0_USER);
        }

        return Optional.empty();
    }
}
