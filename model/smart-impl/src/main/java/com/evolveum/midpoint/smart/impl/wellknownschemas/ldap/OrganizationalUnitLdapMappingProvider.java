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
public class OrganizationalUnitLdapMappingProvider implements WellKnownSchemaProvider {

    private static final Trace LOGGER = TraceManager.getTrace(OrganizationalUnitLdapMappingProvider.class);

    @Override
    public WellKnownSchemaType getSupportedSchemaType() {
        return WellKnownSchemaType.LDAP_ORGANIZATIONAL_UNIT;
    }

    @Override
    public Map<ItemPath, ItemPath> suggestSchemaMatches() {
        Map<ItemPath, ItemPath> matches = new HashMap<>();
        matches.put(ItemPath.create("ou"), OrgType.F_NAME);
        matches.put(ItemPath.create("description"), OrgType.F_DESCRIPTION);
        return matches;
    }

    @Override
    public List<SystemMappingSuggestion> suggestInboundMappings() {
        List<SystemMappingSuggestion> mappings = new ArrayList<>();
        return mappings;
    }

    @Override
    public List<SystemMappingSuggestion> suggestOutboundMappings(@Nullable List<ShadowType> sampleShadows) {
        List<SystemMappingSuggestion> mappings = new ArrayList<>();

        String suffix = extractSuffixFromSamples(sampleShadows);

        if (suffix != null) {
            String script = "basic.composeDnWithSuffix('ou', name, '%s')".formatted(suffix);
            String description = "Compose DN: ou=<name>,%s".formatted(suffix);
            mappings.add(SystemMappingSuggestion.createScriptSuggestion("dn", OrgType.F_NAME, script, description, MappingStrengthType.STRONG));
            mappings.add(SystemMappingSuggestion.createAsIsSuggestion("ou", OrgType.F_NAME, MappingStrengthType.WEAK));
        } else {
            mappings.add(SystemMappingSuggestion.createAsIsSuggestion("ou", OrgType.F_NAME));
        }
        return mappings;
    }

    private String extractSuffixFromSamples(List<ShadowType> sampleShadows) {
        if (sampleShadows == null || sampleShadows.isEmpty()) {
            return null;
        }

        for (ShadowType shadow : sampleShadows) {
            String suffix = extractOuSuffixFromShadow(shadow);
            if (suffix != null) {
                return suffix;
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

            String parentSuffix = ldapName.getPrefix(ldapName.size() - 1).toString();
            return (parentSuffix != null && !parentSuffix.isEmpty()) ? parentSuffix : null;
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
