/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas.ad;

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
public class UserActiveDirectoryMappingProvider implements WellKnownSchemaProvider {

    private static final Trace LOGGER = TraceManager.getTrace(UserActiveDirectoryMappingProvider.class);

    @Override
    public WellKnownSchemaType getSupportedSchemaType() {
        return WellKnownSchemaType.AD_USER;
    }

    @Override
    public Map<ItemPath, ItemPath> suggestSchemaMatches() {
        Map<ItemPath, ItemPath> matches = new HashMap<>();
        matches.put(ItemPath.create("sAMAccountName"), UserType.F_NAME);
        matches.put(ItemPath.create("cn"), UserType.F_FULL_NAME);
        matches.put(ItemPath.create("givenName"), UserType.F_GIVEN_NAME);
        matches.put(ItemPath.create("sn"), UserType.F_FAMILY_NAME);
        matches.put(ItemPath.create("mail"), UserType.F_EMAIL_ADDRESS);
        return matches;
    }

    @Override
    public List<SystemMappingSuggestion> suggestInboundMappings() {
        List<SystemMappingSuggestion> mappings = new ArrayList<>();
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("sAMAccountName", UserType.F_NAME));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("cn", UserType.F_FULL_NAME));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("givenName", UserType.F_GIVEN_NAME));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("sn", UserType.F_FAMILY_NAME));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("mail", UserType.F_EMAIL_ADDRESS));
        return mappings;
    }

    @Override
    public List<SystemMappingSuggestion> suggestOutboundMappings(@Nullable List<ShadowType> sampleShadows) {
        List<SystemMappingSuggestion> mappings = new ArrayList<>();
        var dnMapping = createDnMapping(sampleShadows);
        if (dnMapping != null) {
            mappings.add(dnMapping);
        }
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("sAMAccountName", UserType.F_NAME));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("cn", UserType.F_FULL_NAME));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("givenName", UserType.F_GIVEN_NAME));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("sn", UserType.F_FAMILY_NAME));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("mail", UserType.F_EMAIL_ADDRESS));
        return mappings;
    }

    private SystemMappingSuggestion createDnMapping(@Nullable List<ShadowType> sampleShadows) {
        String ouSuffix = extractOuSuffixFromSamples(sampleShadows);
        if (ouSuffix == null) {
            return null;
        }

        String script = "basic.composeDnWithSuffix('cn', fullName, '%s')".formatted(ouSuffix);
        String description = "Compose DN: cn=<fullName>,%s".formatted(ouSuffix);
        return SystemMappingSuggestion.createScriptSuggestion("distinguishedName", UserType.F_FULL_NAME, script, description);
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
            LOGGER.debug("Invalid DN format in AD shadow: ", e);
        }
        return null;
    }

    private String extractDnValue(ShadowType shadow) {
        if (shadow == null || shadow.getAttributes() == null) {
            return null;
        }

        var dnItem = shadow.getAttributes().asPrismContainerValue().findItem(ItemPath.create("distinguishedName"));
        if (dnItem == null || dnItem.getRealValues().isEmpty()) {
            return null;
        }

        String dnValue = String.valueOf(dnItem.getRealValues().iterator().next());
        return (dnValue != null && !dnValue.isEmpty()) ? dnValue : null;
    }
}
