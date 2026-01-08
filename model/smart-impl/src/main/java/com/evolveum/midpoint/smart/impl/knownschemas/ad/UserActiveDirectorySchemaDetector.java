/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.knownschemas.ad;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.smart.impl.knownschemas.KnownSchemaDetector;
import com.evolveum.midpoint.smart.impl.knownschemas.KnownSchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserActiveDirectorySchemaDetector implements KnownSchemaDetector {

    private static final Set<String> AD_CONNECTOR_TYPES = Set.of(
            "com.evolveum.polygon.connector.ldap.ad.AdLdapConnector"
    );

    private static final Set<String> AD_USER_ATTRIBUTES_LOWERCASE = Set.of(
            "samaccountname",
            "userprincipalname",
            "cn",
            "sn",
            "givenname",
            "mail"
    );

    @Override
    public Optional<KnownSchemaType> detectSchemaType(ResourceType resource, ResourceObjectTypeDefinition typeDefinition) {
        var objectClassDef = typeDefinition.getObjectClassDefinition();
        String objectClassName = objectClassDef.getTypeName().getLocalPart();

        if (!"user".equalsIgnoreCase(objectClassName)) {
            return Optional.empty();
        }

        var connectorRef = resource.getConnectorRef();
        if (connectorRef != null && connectorRef.getOid() != null) {
            String connectorType = connectorRef.getType() != null ? connectorRef.getType().getLocalPart() : "";

            if (AD_CONNECTOR_TYPES.stream().anyMatch(type -> connectorType.contains(type))) {
                return Optional.of(KnownSchemaType.AD_USER);
            }
        }

        Set<String> attributeNames = typeDefinition.getAttributeDefinitions().stream()
                .map(def -> def.getItemName().getLocalPart().toLowerCase())
                .collect(Collectors.toSet());

        if (attributeNames.containsAll(AD_USER_ATTRIBUTES_LOWERCASE)) {
            return Optional.of(KnownSchemaType.AD_USER);
        }

        return Optional.empty();
    }

    @Override
    public KnownSchemaType getSupportedSchemaType() {
        return KnownSchemaType.AD_USER;
    }
}
