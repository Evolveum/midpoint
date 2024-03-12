/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining.generator;

import com.beust.jcommander.Parameter;

/**
 * The BaseGeneratorOptions class provides configuration options for data generation operation, such as importing,
 * transforming, specifying the number of users, and enabling archetype roles and users.
 * <p>
 * Part of RBAC Testing Data, which provides testing data for role mining and other RBAC-related processes.
 */
public class BaseGeneratorOptions {

    public static final String P_IMPORT = "-i";
    public static final String P_IMPORT_LONG = "--import";
    public static final String P_TRANSFORM = "-t";
    public static final String P_TRANSFORM_LONG = "--transform";

    public static final String P_ROLE_MULTIPLIER = "-rm";
    public static final String P_ROLE_MULTIPLIER_LONG = "--role-multiplier";

    public static final String P_USERS_COUNT = "-uc";
    public static final String P_USERS_COUNT_LONG = "--users-count";

    public static final String P_NAME_CSV_FILE = "-np";
    public static final String P_NAME_CSV_FILE_LONG = "--name-csv-file";

    public static final String P_ARCHETYPE_ROLE = "-ar";
    public static final String P_ARCHETYPE_ROLE_LONG = "--archetype-role-enabled";

    public static final String P_ARCHETYPE_USER = "-au";
    public static final String P_ARCHETYPE_USER_LONG = "--archetype-user-enabled";

    @Parameter(names = { P_ARCHETYPE_ROLE, P_ARCHETYPE_ROLE_LONG }, descriptionKey = "baseGeneratorOptions.archetypeRole")
    private boolean isArchetypeRoleEnable = false;
    @Parameter(names = { P_ARCHETYPE_USER, P_ARCHETYPE_USER_LONG }, descriptionKey = "baseGeneratorOptions.archetypeUser")
    private boolean isArchetypeUserEnable = false;

    @Parameter(names = { P_IMPORT, P_IMPORT_LONG }, descriptionKey = "baseGeneratorOptions.import")
    private boolean isImport = false;
    @Parameter(names = { P_NAME_CSV_FILE, P_NAME_CSV_FILE_LONG }, descriptionKey = "baseGeneratorOptions.nameCsvFile")
    private String csvPath;
    @Parameter(names = { P_TRANSFORM, P_TRANSFORM_LONG }, descriptionKey = "baseGeneratorOptions.transform")
    private boolean isTransform = false;

    @Parameter(names = { P_ROLE_MULTIPLIER, P_ROLE_MULTIPLIER_LONG }, descriptionKey = "baseGeneratorOptions.roleMultiplier")
    private int roleMultiplier = 0;

    @Parameter(names = { P_USERS_COUNT, P_USERS_COUNT_LONG }, descriptionKey = "baseGeneratorOptions.usersCount")
    private int usersCount = 100;

    public boolean isTransform() {
        return isTransform;
    }

    public boolean isImport() {
        return isImport;
    }

    public int getRoleMultiplier() {
        return roleMultiplier;
    }

    public int getUsersCount() {
        if (usersCount < 10) {
            return 10;
        }
        return usersCount;
    }

    public String getCsvPath() {
        return csvPath;
    }

    public boolean isArchetypeRoleEnable() {
        return isArchetypeRoleEnable;
    }

    public boolean isArchetypeUserEnable() {
        return isArchetypeUserEnable;
    }

}
