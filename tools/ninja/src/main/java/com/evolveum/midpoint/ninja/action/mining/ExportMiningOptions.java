/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import com.evolveum.midpoint.common.RoleMiningExportUtils;
import com.evolveum.midpoint.ninja.action.BasicExportOptions;
import com.evolveum.midpoint.ninja.util.ItemPathConverter;
import com.evolveum.midpoint.prism.path.ItemPath;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "exportMining")
public class ExportMiningOptions extends BaseMiningOptions implements BasicExportOptions {

    private static final String DELIMITER = ",";
    public static final String P_OUTPUT = "-o";
    public static final String P_OUTPUT_LONG = "--output";
    public static final String P_OVERWRITE = "-O";
    public static final String P_OVERWRITE_LONG = "--overwrite";
    public static final String P_PREFIX_APPLICATION = "-arp";
    public static final String P_PREFIX_APPLICATION_LONG = "--application-role-prefix";
    public static final String P_PREFIX_BUSINESS = "-brp";
    public static final String P_PREFIX_BUSINESS_LONG = "--business-role-prefix";
    public static final String P_SUFFIX_APPLICATION = "-ars";
    public static final String P_SUFFIX_APPLICATION_LONG = "--application-role-suffix";
    public static final String P_SUFFIX_BUSINESS = "-brs";
    public static final String P_SUFFIX_BUSINESS_LONG = "--business-role-suffix";
    public static final String P_ORG = "-do";
    public static final String P_ORG_LONG = "--disable-org";
    public static final String P_ATTRIBUTE = "-da";
    public static final String P_ATTRIBUTE_LONG = "--disable-attribute";
    public static final String P_EXCLUDE_ATTRIBUTES_USER_LONG = "--exclude-user-attribute";
    public static final String P_EXCLUDE_ATTRIBUTES_ROLE_LONG = "--exclude-role-attribute";
    public static final String P_EXCLUDE_ATTRIBUTES_ORG_LONG = "--exclude-org-attribute";
    public static final String P_NAME_OPTIONS = "-nm";
    public static final String P_NAME_OPTIONS_LONG = "--name-mode";
    public static final String P_ARCHETYPE_OID_APPLICATION_LONG = "--application-role-archetype-oid";
    public static final String P_ARCHETYPE_OID_BUSINESS_LONG = "--business-role-archetype-oid";
    public static final String P_SECURITY_LEVEL = "-s";
    public static final String P_SECURITY_LEVEL_LONG = "--security";

    @Parameter(names = { P_SECURITY_LEVEL, P_SECURITY_LEVEL_LONG }, descriptionKey = "export.security.level")
    private RoleMiningExportUtils.SecurityMode securityMode = RoleMiningExportUtils.SecurityMode.ADVANCED;

    @Parameter(names = { P_SUFFIX_APPLICATION, P_SUFFIX_APPLICATION_LONG }, descriptionKey = "export.application.role.suffix")
    private String applicationRoleSuffix;

    @Parameter(names = { P_SUFFIX_BUSINESS, P_SUFFIX_BUSINESS_LONG }, descriptionKey = "export.business.role.suffix")
    private String businessRoleSuffix;

    @Parameter(names = { P_OUTPUT, P_OUTPUT_LONG }, descriptionKey = "export.output")
    private File output;

    @Parameter(names = { P_OVERWRITE, P_OVERWRITE_LONG }, descriptionKey = "export.overwrite")
    private boolean overwrite;

    @Parameter(names = { P_PREFIX_BUSINESS, P_PREFIX_BUSINESS_LONG }, descriptionKey = "export.business.role.prefix")
    private String businessRolePrefix;

    @Parameter(names = { P_PREFIX_APPLICATION, P_PREFIX_APPLICATION_LONG }, descriptionKey = "export.application.role.prefix")
    private String applicationRolePrefix;

    @Parameter(names = { P_ORG, P_ORG_LONG }, descriptionKey = "export.prevent.org")
    private boolean disableOrg = false;

    @Parameter(names = { P_ATTRIBUTE, P_ATTRIBUTE_LONG }, descriptionKey = "export.prevent.attribute")
    private boolean disableAttribute = false;

    @Parameter(names = { P_NAME_OPTIONS, P_NAME_OPTIONS_LONG }, descriptionKey = "export.name.options")
    private RoleMiningExportUtils.NameMode nameMode = RoleMiningExportUtils.NameMode.SEQUENTIAL;

    @Parameter(names = { P_ARCHETYPE_OID_APPLICATION_LONG },
            descriptionKey = "export.application.role.archetype.oid")
    private String applicationRoleArchetypeOid = "00000000-0000-0000-0000-000000000328";

    @Parameter(names = { P_ARCHETYPE_OID_BUSINESS_LONG },
            descriptionKey = "export.business.role.archetype.oid")
    private String businessRoleArchetypeOid = "00000000-0000-0000-0000-000000000321";

    @Parameter(names = { P_EXCLUDE_ATTRIBUTES_USER_LONG }, descriptionKey = "export.exclude.attributes.user",
            validateWith = ItemPathConverter.class, converter = ItemPathConverter.class)
    private List<ItemPath> excludedAttributesUser = new ArrayList<>();

    @Parameter(names = { P_EXCLUDE_ATTRIBUTES_ROLE_LONG }, descriptionKey = "export.exclude.attributes.role",
            validateWith = ItemPathConverter.class, converter = ItemPathConverter.class)
    private List<ItemPath> excludedAttributesRole = new ArrayList<>();

    @Parameter(names = { P_EXCLUDE_ATTRIBUTES_ORG_LONG }, descriptionKey = "export.exclude.attributes.org",
            validateWith = ItemPathConverter.class, converter = ItemPathConverter.class)
    private List<ItemPath> excludedAttributesOrg = new ArrayList<>();

    public RoleMiningExportUtils.SecurityMode getSecurityLevel() {
        return securityMode;
    }

    public boolean isIncludeOrg() {
        return !disableOrg;
    }

    public boolean isIncludeAttributes() {
        return !disableAttribute;
    }

    public String getApplicationRoleArchetypeOid() {
        return applicationRoleArchetypeOid;
    }

    public String getBusinessRoleArchetypeOid() {
        return businessRoleArchetypeOid;
    }

    public RoleMiningExportUtils.NameMode getNameMode() {
        return nameMode;
    }

    public File getOutput() {
        return output;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public List<String> getApplicationRolePrefix() {
        if (applicationRolePrefix == null || applicationRolePrefix.isEmpty()) {
            return new ArrayList<>();
        }
        String[] separatePrefixes = applicationRolePrefix.split(DELIMITER);
        return new ArrayList<>(Arrays.asList(separatePrefixes));
    }

    public List<String> getBusinessRolePrefix() {
        if (businessRolePrefix == null || businessRolePrefix.isEmpty()) {
            return new ArrayList<>();
        }
        String[] separatePrefixes = businessRolePrefix.split(DELIMITER);
        return new ArrayList<>(Arrays.asList(separatePrefixes));
    }

    public List<String> getApplicationRoleSuffix() {
        if (applicationRoleSuffix == null || applicationRoleSuffix.isEmpty()) {
            return new ArrayList<>();
        }
        String[] separateSuffixes = applicationRoleSuffix.split(DELIMITER);
        return new ArrayList<>(Arrays.asList(separateSuffixes));
    }

    public List<String> getBusinessRoleSuffix() {
        if (businessRoleSuffix == null || businessRoleSuffix.isEmpty()) {
            return new ArrayList<>();
        }
        String[] separateSuffixes = businessRoleSuffix.split(DELIMITER);
        return new ArrayList<>(Arrays.asList(separateSuffixes));
    }

    private List<String> itemPathsToStrings(List<ItemPath> itemPaths) {
        return itemPaths.stream().map(ItemPath::toString).toList();
    }

    public List<String> getExcludedAttributesUser() {
        return itemPathsToStrings(excludedAttributesUser);
    }

    public List<String> getExcludedAttributesRole() {
        return itemPathsToStrings(excludedAttributesRole);
    }

    public List<String> getExcludedAttributesOrg() {
        return itemPathsToStrings(excludedAttributesOrg);
    }
}
