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
        matches.put(ItemPath.create("mail"), UserType.F_EMAIL_ADDRESS);
        matches.put(ItemPath.create("telephoneNumber"), UserType.F_TELEPHONE_NUMBER);
        return matches;
    }

    @Override
    public List<SystemMappingSuggestion> suggestInboundMappings() {
        List<SystemMappingSuggestion> mappings = new ArrayList<>();
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("uid", UserType.F_NAME));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("cn", UserType.F_FULL_NAME));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("givenName", UserType.F_GIVEN_NAME));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("sn", UserType.F_FAMILY_NAME));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("mail", UserType.F_EMAIL_ADDRESS));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("telephoneNumber", UserType.F_TELEPHONE_NUMBER));
        return mappings;
    }

    @Override
    public List<SystemMappingSuggestion> suggestOutboundMappings(@Nullable List<ShadowType> sampleShadows) {
        List<SystemMappingSuggestion> mappings = new ArrayList<>();
        var dnMapping = createDnMapping(sampleShadows);
        if (dnMapping != null) {
            mappings.add(dnMapping);
        }
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("uid", UserType.F_NAME));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("cn", UserType.F_FULL_NAME));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("givenName", UserType.F_GIVEN_NAME));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("sn", UserType.F_FAMILY_NAME));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("mail", UserType.F_EMAIL_ADDRESS));
        mappings.add(SystemMappingSuggestion.createAsIsSuggestion("telephoneNumber", UserType.F_TELEPHONE_NUMBER));
        return mappings;
    }

    private SystemMappingSuggestion createDnMapping(@Nullable List<ShadowType> sampleShadows) {
        DnStructure dnStructure = extractDnStructureFromSamples(sampleShadows);
        if (dnStructure == null) {
            return null;
        }

        return switch (dnStructure.rdnAttribute().toLowerCase()) {
            case "uid" -> createDnScriptSuggestion("uid", "name", UserType.F_NAME, dnStructure.ouSuffix());
            case "cn" -> createDnScriptSuggestion("cn", "fullName", UserType.F_FULL_NAME, dnStructure.ouSuffix());
            default -> null;
        };
    }

    private SystemMappingSuggestion createDnScriptSuggestion(
            String rdnAttr, String sourceVar, ItemPath sourcePath, String ouSuffix) {
        String script = "basic.composeDnWithSuffix('%s', %s, '%s')".formatted(rdnAttr, sourceVar, ouSuffix);
        String description = "Compose DN: %s=<%s>,%s".formatted(rdnAttr, sourceVar, ouSuffix);
        return SystemMappingSuggestion.createScriptSuggestion("dn", sourcePath, script, description);
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
        String firstRdn = ldapName.getRdn(ldapName.size() - 1).toString();
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
