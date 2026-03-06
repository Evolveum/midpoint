/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
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
public class InetOrgPersonLdapMappingProvider implements WellKnownSchemaProvider {

    private static final Trace LOGGER = TraceManager.getTrace(InetOrgPersonLdapMappingProvider.class);

    private record DnStructure(String rdnAttribute, String ouSuffix) {}

    @Override
    public WellKnownSchemaType getSupportedSchemaType() {
        return WellKnownSchemaType.LDAP_INETORGPERSON;
    }

    @Override
    public Map<ItemPath, ItemPath> suggestSchemaMatches() {
        Map<ItemPath, ItemPath> matches = new HashMap<>();
        matches.put(ItemPath.create("uid"), UserType.F_NAME);
        matches.put(ItemPath.create("cn"), UserType.F_FULL_NAME);
        matches.put(ItemPath.create("givenName"), UserType.F_GIVEN_NAME);
        matches.put(ItemPath.create("sn"), UserType.F_FAMILY_NAME);
        matches.put(ItemPath.create("description"), UserType.F_DESCRIPTION);
        matches.put(ItemPath.create("employeeNumber"), UserType.F_PERSONAL_NUMBER);
        matches.put(ItemPath.create("jpegPhoto"), UserType.F_JPEG_PHOTO);
        matches.put(ItemPath.create("mail"), UserType.F_EMAIL_ADDRESS);
        matches.put(ItemPath.create("l"), UserType.F_LOCALITY);
        matches.put(ItemPath.create("telephoneNumber"), UserType.F_TELEPHONE_NUMBER);
        matches.put(ItemPath.create("ou"), UserType.F_ORGANIZATIONAL_UNIT);
        matches.put(ItemPath.create("o"), UserType.F_ORGANIZATION);
        matches.put(ItemPath.create("preferredLanguage"), UserType.F_PREFERRED_LANGUAGE);
        matches.put(ItemPath.create("title"), UserType.F_TITLE);
        return matches;
    }

    @Override
    public List<SystemMappingSuggestion> suggestInboundMappings() {
        List<SystemMappingSuggestion> mappings = new ArrayList<>();
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("uid", UserType.F_NAME));
        return mappings;
    }

    @Override
    public List<SystemMappingSuggestion> suggestOutboundMappings(@Nullable List<ShadowType> sampleShadows) {
        List<SystemMappingSuggestion> mappings = new ArrayList<>();

        DnStructure dnStructure = extractDnStructureFromSamples(sampleShadows);
        String rdn = dnStructure != null ? dnStructure.rdnAttribute().toLowerCase() : null;

        if ("uid".equals(rdn)) {
            mappings.add(createDnScriptSuggestion("uid", "name", UserType.F_NAME, dnStructure.ouSuffix()));
        } else if ("cn".equals(rdn)) {
            mappings.add(createDnScriptSuggestion("cn", "fullName", UserType.F_FULL_NAME, dnStructure.ouSuffix()));
        }

        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("uid", UserType.F_NAME,
                "uid".equals(rdn) ? MappingStrengthType.WEAK : MappingStrengthType.STRONG));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("cn", UserType.F_FULL_NAME,
                "cn".equals(rdn) ? MappingStrengthType.WEAK : MappingStrengthType.STRONG));

        return mappings;
    }

    private SystemMappingSuggestion createDnScriptSuggestion(
            String rdnAttr, String sourceVar, ItemPath sourcePath, String ouSuffix) {
        String script = "basic.composeDnWithSuffix('%s', %s, '%s')".formatted(rdnAttr, sourceVar, ouSuffix);
        String description = "Compose DN: %s=<%s>,%s".formatted(rdnAttr, sourceVar, ouSuffix);
        return SystemMappingSuggestion.createScriptSuggestion("dn", sourcePath, script, description,
                MappingStrengthType.STRONG);
    }

    private DnStructure extractDnStructureFromSamples(List<ShadowType> sampleShadows) {
        if (sampleShadows == null || sampleShadows.isEmpty()) {
            return null;
        }

        for (ShadowType shadow : sampleShadows) {
            DnStructure structure = parseDnFromShadow(shadow);
            if (structure != null) {
                return structure;
            }
        }
        return null;
    }

    private DnStructure parseDnFromShadow(ShadowType shadow) {
        try {
            String dnValue = extractDnValue(shadow);
            if (dnValue == null) {
                return null;
            }

            LdapName ldapName = new LdapName(dnValue);
            if (ldapName.size() < 2) {
                return null;
            }

            String rdnAttribute = extractRdnAttribute(ldapName);
            if (rdnAttribute == null) {
                return null;
            }

            String ouSuffix = findOuSuffix(ldapName);
            return ouSuffix != null ? new DnStructure(rdnAttribute, ouSuffix) : null;

        } catch (InvalidNameException e) {
            LOGGER.debug("Invalid LDAP DN format in sample shadow: ", e);
            return null;
        }
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

    private String extractRdnAttribute(LdapName ldapName) {
        String firstRdn = ldapName.getRdn(ldapName.size() - 1).toString().toLowerCase();
        if (firstRdn.startsWith("uid=")) {
            return "uid";
        } else if (firstRdn.startsWith("cn=")) {
            return "cn";
        }
        return null;
    }

    private String findOuSuffix(LdapName ldapName) {
        for (int i = ldapName.size(); i > 0; i--) {
            String prefix = ldapName.getPrefix(i).toString();
            if (prefix.toLowerCase().startsWith("ou=") || prefix.toLowerCase().startsWith("o=")) {
                return prefix;
            }
        }
        return null;
    }
}
