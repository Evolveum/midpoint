/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas.ldap;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaDetector;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class InetOrgPersonLdapSchemaDetector implements WellKnownSchemaDetector {

    private static final Set<String> LDAP_CONNECTOR_TYPES = Set.of(
            "org.identityconnectors.ldap.LdapConnector",
            "LdapConnector",
            "com.evolveum.polygon.connector.ldap.LdapConnector"
    );

    private static final Set<String> LDAP_INETORGPERSON_ATTRIBUTES_LOWERCASE = Set.of(
            "uid",
            "cn",
            "sn",
            "givenname",
            "mail",
            "telephonenumber"
    );

    @Override
    public Optional<WellKnownSchemaType> detectSchemaType(ResourceType resource, ResourceObjectTypeDefinition typeDefinition) {
        var objectClassDef = typeDefinition.getObjectClassDefinition();
        String objectClassName = objectClassDef.getTypeName().getLocalPart();

        if (!"inetorgperson".equalsIgnoreCase(objectClassName)) {
            return Optional.empty();
        }

        var connectorRef = resource.getConnectorRef();
        if (connectorRef != null && connectorRef.getOid() != null) {
            String connectorType = connectorRef.getType() != null ? connectorRef.getType().getLocalPart() : "";

            if (LDAP_CONNECTOR_TYPES.stream().anyMatch(type -> connectorType.contains(type))) {
                return Optional.of(WellKnownSchemaType.LDAP_INETORGPERSON);
            }
        }

        Set<String> attributeNames = typeDefinition.getAttributeDefinitions().stream()
                .map(def -> def.getItemName().getLocalPart().toLowerCase())
                .collect(Collectors.toSet());

        if (attributeNames.containsAll(LDAP_INETORGPERSON_ATTRIBUTES_LOWERCASE)) {
            return Optional.of(com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaType.LDAP_INETORGPERSON);
        }

        return Optional.empty();
    }
}
