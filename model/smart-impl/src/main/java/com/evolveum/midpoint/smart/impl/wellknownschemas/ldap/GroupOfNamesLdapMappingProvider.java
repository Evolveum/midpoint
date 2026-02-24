/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas.ldap;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.smart.impl.wellknownschemas.SystemMappingSuggestion;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaProvider;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaType;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GroupOfNamesLdapMappingProvider implements WellKnownSchemaProvider {

    private static final Trace LOGGER = TraceManager.getTrace(GroupOfNamesLdapMappingProvider.class);

    @Override
    public WellKnownSchemaType getSupportedSchemaType() {
        return WellKnownSchemaType.LDAP_GROUP_OF_NAMES;
    }

    @Override
    public Map<ItemPath, ItemPath> suggestSchemaMatches() {
        Map<ItemPath, ItemPath> matches = new HashMap<>();
        matches.put(ItemPath.create("description"), RoleType.F_DESCRIPTION);
        return matches;
    }

    @Override
    public List<SystemMappingSuggestion> suggestInboundMappings() {
        List<SystemMappingSuggestion> mappings = new ArrayList<>();
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("cn", AbstractRoleType.F_IDENTIFIER));
        mappings.add(SystemMappingSuggestion.createScriptSuggestion(
                "cn",
                RoleType.F_NAME,
                "'ldap:' + cn",
                "Inbound: group-sync-methodology name with prefix (ldap:<cn>)",
                MappingStrengthType.STRONG));
        return mappings;
    }

    @Override
    public List<SystemMappingSuggestion> suggestOutboundMappings(@Nullable List<ShadowType> sampleShadows) {
        List<SystemMappingSuggestion> mappings = new ArrayList<>();
        String ouSuffix = extractOuSuffixFromSamples(sampleShadows);
        if (ouSuffix != null) {
            mappings.add(SystemMappingSuggestion.createScriptSuggestion(
                    "dn",
                    AbstractRoleType.F_IDENTIFIER,
                    "basic.composeDnWithSuffix('cn', identifier, '%s')".formatted(ouSuffix),
                    "Compose DN: cn=<identifier>,%s".formatted(ouSuffix),
                    MappingStrengthType.STRONG));
        }
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("cn", AbstractRoleType.F_IDENTIFIER, MappingStrengthType.WEAK));
        return mappings;
    }

    private String extractOuSuffixFromSamples(List<ShadowType> sampleShadows) {
        if (sampleShadows == null || sampleShadows.isEmpty()) {
            return null;
        }

        for (ShadowType shadow : sampleShadows) {
            String ouSuffix = extractOuSuffixFromShadow(shadow);
            if (ouSuffix != null) {
                return ouSuffix;
            }
        }
        return null;
    }

    private String extractOuSuffixFromShadow(ShadowType shadow) {
        try {
            String dnValue = extractDnValue(shadow);
            if (dnValue == null) {
                return null;
            }

            LdapName ldapName = new LdapName(dnValue);
            if (ldapName.size() < 2) {
                return null;
            }

            String firstRdn = ldapName.getRdn(ldapName.size() - 1).toString();
            if (!firstRdn.toLowerCase().startsWith("cn=")) {
                return null;
            }

            for (int i = ldapName.size(); i > 0; i--) {
                String prefix = ldapName.getPrefix(i).toString();
                if (prefix.toLowerCase().startsWith("ou=") || prefix.toLowerCase().startsWith("o=")) {
                    return prefix;
                }
            }
        } catch (InvalidNameException e) {
            LOGGER.debug("Invalid LDAP DN format in sample shadow: ", e);
        }
        return null;
    }

    private String extractDnValue(ShadowType shadow) {
        if (shadow == null || shadow.getAttributes() == null) {
            return null;
        }

        var dnItem = shadow.getAttributes().asPrismContainerValue().findItem(ItemPath.create("dn"));
        if (dnItem == null || dnItem.getRealValues().isEmpty()) {
            return null;
        }

        String dnValue = String.valueOf(dnItem.getRealValues().iterator().next());
        return (dnValue != null && !dnValue.isEmpty()) ? dnValue : null;
    }
}
