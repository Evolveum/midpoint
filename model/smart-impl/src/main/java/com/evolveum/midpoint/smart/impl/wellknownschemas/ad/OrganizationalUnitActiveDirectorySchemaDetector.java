/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas.ad;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaDetector;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OrganizationalUnitActiveDirectorySchemaDetector implements WellKnownSchemaDetector {

    private static final Set<String> AD_CONNECTOR_TYPES = Set.of(
            "com.evolveum.polygon.connector.ldap.ad.AdLdapConnector"
    );

    private static final Set<String> AD_OU_ATTRIBUTES_LOWERCASE = Set.of(
            "ou",
            "distinguishedname",
            "admindescription"
    );

    @Override
    public Optional<WellKnownSchemaType> detectSchemaType(ResourceType resource, ResourceObjectTypeDefinition typeDefinition) {
        var objectClassDef = typeDefinition.getObjectClassDefinition();
        String objectClassName = objectClassDef.getTypeName().getLocalPart();

        if (!"organizationalUnit".equalsIgnoreCase(objectClassName)) {
            return Optional.empty();
        }

        var connectorRef = resource.getConnectorRef();
        if (connectorRef != null && connectorRef.getOid() != null) {
            String connectorType = connectorRef.getType() != null ? connectorRef.getType().getLocalPart() : "";

            if (AD_CONNECTOR_TYPES.stream().anyMatch(type -> connectorType.contains(type))) {
                return Optional.of(WellKnownSchemaType.AD_ORGANIZATIONAL_UNIT);
            }
        }

        Set<String> attributeNames = typeDefinition.getAttributeDefinitions().stream()
                .map(def -> def.getItemName().getLocalPart().toLowerCase())
                .collect(Collectors.toSet());

        if (attributeNames.containsAll(AD_OU_ATTRIBUTES_LOWERCASE)) {
            return Optional.of(WellKnownSchemaType.AD_ORGANIZATIONAL_UNIT);
        }

        return Optional.empty();
    }
}
