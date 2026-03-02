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
        matches.put(ItemPath.create("userPrincipalName"), UserType.F_NAME);
        matches.put(ItemPath.create("cn"), UserType.F_FULL_NAME);
        matches.put(ItemPath.create("givenName"), UserType.F_GIVEN_NAME);
        matches.put(ItemPath.create("sn"), UserType.F_FAMILY_NAME);
        matches.put(ItemPath.create("company"), UserType.F_ORGANIZATION);
        matches.put(ItemPath.create("department"), UserType.F_ORGANIZATIONAL_UNIT);
        matches.put(ItemPath.create("employeeNumber"), UserType.F_PERSONAL_NUMBER);
        matches.put(ItemPath.create("mail"), UserType.F_EMAIL_ADDRESS);
        matches.put(ItemPath.create("l"), UserType.F_LOCALITY);
        matches.put(ItemPath.create("telephoneNumber"), UserType.F_TELEPHONE_NUMBER);
        matches.put(ItemPath.create("title"), UserType.F_TITLE);
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
        String ouSuffix = extractOuSuffixFromSamples(sampleShadows);
        if (ouSuffix != null) {
            mappings.add(SystemMappingSuggestion.createScriptSuggestion(
                    "distinguishedName",
                    UserType.F_FULL_NAME,
                    "basic.composeDnWithSuffix('cn', fullName, '%s')".formatted(ouSuffix),
                    "Compose DN: cn=<fullName>,%s".formatted(ouSuffix),
                    MappingStrengthType.STRONG));
            mappings.add(SystemMappingSuggestion.createAsIsSuggestion("cn", UserType.F_FULL_NAME, MappingStrengthType.WEAK));
        } else {
            mappings.add(SystemMappingSuggestion.createAsIsSuggestion("cn", UserType.F_FULL_NAME));
        }
        String upnSuffix = extractUpnSuffixFromSamples(sampleShadows);
        if (upnSuffix != null) {
            mappings.add(SystemMappingSuggestion.createScriptSuggestion(
                    "userPrincipalName",
                    UserType.F_NAME,
                    "name + '%s'".formatted(upnSuffix),
                    "Compose UPN: <name>%s".formatted(upnSuffix),
                    MappingStrengthType.STRONG));
        }
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
            LOGGER.debug("Invalid DN format in AD shadow: ", e);
        }
        return null;
    }

    private String extractUpnSuffixFromSamples(List<ShadowType> sampleShadows) {
        if (sampleShadows == null || sampleShadows.isEmpty()) {
            return null;
        }
        for (ShadowType shadow : sampleShadows) {
            String upnSuffix = extractUpnSuffixFromShadow(shadow);
            if (upnSuffix != null) {
                return upnSuffix;
            }
        }
        return null;
    }

    private String extractUpnSuffixFromShadow(ShadowType shadow) {
        if (shadow == null || shadow.getAttributes() == null) {
            return null;
        }
        var upnItem = shadow.getAttributes().asPrismContainerValue().findItem(ItemPath.create("userPrincipalName"));
        if (upnItem == null || upnItem.getRealValues().isEmpty()) {
            return null;
        }
        String upnValue = String.valueOf(upnItem.getRealValues().iterator().next());
        if (upnValue == null || upnValue.isEmpty()) {
            return null;
        }
        int atIndex = upnValue.indexOf('@');
        if (atIndex < 0) {
            return null;
        }
        return upnValue.substring(atIndex);
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
