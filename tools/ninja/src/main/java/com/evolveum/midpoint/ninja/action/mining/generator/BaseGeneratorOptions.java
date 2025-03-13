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

    public static final String P_RANDOM_ROLE_MULTIPLIER = "-rrm";
    public static final String P_RANDOM_ROLE_MULTIPLIER_LONG = "--random-role-multiplier";

    public static final String P_MIN_RANDOM_MULTIPLIER = "-mrm";
    public static final String P_MIN_RANDOM_MULTIPLIER_LONG = "--min-random-multiplier";

    public static final String P_MAX_RANDOM_MULTIPLIER = "-xrm";
    public static final String P_MAX_RANDOM_MULTIPLIER_LONG = "--max-random-multiplier";

    public static final String P_MAX_MEMBERSHIP_SPECIAL_ROLES = "-msr";
    public static final String P_MAX_MEMBERSHIP_SPECIAL_ROLES_LONG = "--max-membership-special-roles";

    public static final String P_IS_SPECIAL_LOW_MEMBERSHIP_ROLE_ENABLED = "-slmr";
    public static final String P_IS_SPECIAL_LOW_MEMBERSHIP_ROLE_ENABLED_LONG = "--special-low-membership-role-enabled";

    public static final String P_SPECIAL_LOW_MEMBERSHIP_ROLE_COUNT = "-slmrc";
    public static final String P_SPECIAL_LOW_MEMBERSHIP_ROLE_COUNT_LONG = "--special-low-membership-role-count";

    public static final String P_USERS_COUNT = "-uc";
    public static final String P_USERS_COUNT_LONG = "--users-count";

    public static final String P_NAME_CSV_FILE = "-np";
    public static final String P_NAME_CSV_FILE_LONG = "--name-csv-file";

    public static final String P_ARCHETYPE_ROLE = "-ar";
    public static final String P_ARCHETYPE_ROLE_LONG = "--archetype-role-enabled";

    public static final String P_ARCHETYPE_USER = "-au";
    public static final String P_ARCHETYPE_USER_LONG = "--archetype-user-enabled";

    public static final String P_USER_DIVISION = "-ud";
    public static final String P_USER_DIVISION_LONG = "--user-division";

    public static final String P_INCLUDE_AUX = "-ia";
    public static final String P_INCLUDE_AUX_LONG = "--include-aux";

    public static final String P_FORGET_NOISE = "-fn";
    public static final String P_FORGET_NOISE_LONG = "--forget-noise";

    public static final String P_ADDITION_NOISE = "-an";
    public static final String P_ADDITION_NOISE_LONG = "--addition-noise";

    public static final String P_PLANKTON_DISABLE = "-pd";
    public static final String P_PLANKTON_DISABLE_LONG = "--plankton-disable";

    public static final String P_OUTLIER_MATUZALEM = "-oe";
    public static final String P_OUTLIER_MATUZALEM_LONG = "--outlier-matuzalem";

    public static final String P_OUTLIER_JUMPER = "-oj";
    public static final String P_OUTLIER_JUMPER_LONG = "--outlier-jumper";

    public static final String P_OUTLIER_MASK = "-om";
    public static final String P_OUTLIER_MASD_LONG = "--outlier-mask";

    public static final String P_OUTLIER_ZOMBIE = "-oz";
    public static final String P_OUTLIER_ZOMBIE_LONG = "--outlier-zombie";

    public static final String P_OUTLIER_PROBABILITY = "-op";
    public static final String P_OUTLIER_PROBABILITY_LONG = "--outlier-probability";

    @Parameter(names = { P_OUTLIER_PROBABILITY, P_OUTLIER_PROBABILITY_LONG }, descriptionKey = "baseGeneratorOptions.outlierProbability")
    private int outlierProbability = 0;

    @Parameter(names = { P_OUTLIER_ZOMBIE, P_OUTLIER_ZOMBIE_LONG }, descriptionKey = "baseGeneratorOptions.outlierZombie")
    private int outlierZombieProbability = 0;

    @Parameter(names = { P_OUTLIER_MASK, P_OUTLIER_MASD_LONG }, descriptionKey = "baseGeneratorOptions.outlierMask")
    private int outlierMaskProbability = 0;

    @Parameter(names = { P_OUTLIER_JUMPER, P_OUTLIER_JUMPER_LONG }, descriptionKey = "baseGeneratorOptions.outlierJumper")
    private int outlierJumperProbability = 0;

    @Parameter(names = { P_OUTLIER_MATUZALEM, P_OUTLIER_MATUZALEM_LONG }, descriptionKey = "baseGeneratorOptions.outlierMatuzalem")
    private int outlierMatuzalemProbability = 0;

    @Parameter(names = { P_PLANKTON_DISABLE, P_PLANKTON_DISABLE_LONG }, descriptionKey = "baseGeneratorOptions.planktonDisable")
    private boolean isPlanktonDisable = false;

    @Parameter(names = { P_FORGET_NOISE, P_FORGET_NOISE_LONG }, descriptionKey = "baseGeneratorOptions.forget.noise")
    private int forgetNoise = 0;

    @Parameter(names = { P_ADDITION_NOISE, P_ADDITION_NOISE_LONG }, descriptionKey = "baseGeneratorOptions.addition.noise")
    private int additionNoise = 0;

    @Parameter(names = { P_INCLUDE_AUX, P_INCLUDE_AUX_LONG }, descriptionKey = "baseGeneratorOptions.includeAux")
    private boolean isAuxInclude = false;

    @Parameter(names = { P_USER_DIVISION, P_USER_DIVISION_LONG }, descriptionKey = "baseGeneratorOptions.userDivision")
    private String division = "30:20:20:10:10:5:5";

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

    @Parameter(names = { P_RANDOM_ROLE_MULTIPLIER, P_RANDOM_ROLE_MULTIPLIER_LONG }, descriptionKey = "baseGeneratorOptions.randomMultipliers")
    private boolean randomRoleMultiplier = false;

    @Parameter(names = { P_USERS_COUNT, P_USERS_COUNT_LONG }, descriptionKey = "baseGeneratorOptions.usersCount")
    private int usersCount = 100;

    @Parameter(names = { P_MIN_RANDOM_MULTIPLIER, P_MIN_RANDOM_MULTIPLIER_LONG }, descriptionKey = "baseGeneratorOptions.minRandomMultiplier")
    private int minRandomMultiplier = 3;

    @Parameter(names = { P_MAX_RANDOM_MULTIPLIER, P_MAX_RANDOM_MULTIPLIER_LONG }, descriptionKey = "baseGeneratorOptions.maxRandomMultiplier")
    private int maxRandomMultiplier = 7;

    @Parameter(names = { P_MAX_MEMBERSHIP_SPECIAL_ROLES, P_MAX_MEMBERSHIP_SPECIAL_ROLES_LONG }, descriptionKey = "baseGeneratorOptions.maxMembershipSpecialRoles")
    private int maxMembershipSpecialRoles = 5;

    @Parameter(names = { P_IS_SPECIAL_LOW_MEMBERSHIP_ROLE_ENABLED, P_IS_SPECIAL_LOW_MEMBERSHIP_ROLE_ENABLED_LONG }, descriptionKey = "baseGeneratorOptions.specialLowMembershipRoleEnabled")
    private boolean isSpecialLowMembershipRoleEnabled = false;

    @Parameter(names = { P_SPECIAL_LOW_MEMBERSHIP_ROLE_COUNT, P_SPECIAL_LOW_MEMBERSHIP_ROLE_COUNT_LONG }, descriptionKey = "baseGeneratorOptions.specialLowMembershipRoleCount")
    private int specialLowMembershipRoleCount = 200;

    public int getSpecialLowMembershipRoleCount() {
        return specialLowMembershipRoleCount;
    }

    public boolean isSpecialLowMembershipRoleEnabled() {
        return isSpecialLowMembershipRoleEnabled;
    }

    public int getMaxMembershipSpecialRoles() {
        return maxMembershipSpecialRoles;
    }

    public int getMinRandomMultiplier() {
        return minRandomMultiplier;
    }

    public int getMaxRandomMultiplier() {
        return maxRandomMultiplier;
    }

    public boolean isRandomRoleMultiplier() {
        return randomRoleMultiplier;
    }

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

    public String getDivision() {
        return division;
    }

    public boolean isAuxInclude() {
        return isAuxInclude;
    }

    public int getForgetNoise() {
        return forgetNoise;
    }

    public int getAdditionNoise() {
        return additionNoise;
    }

    public boolean isPlanktonDisable() {
        return isPlanktonDisable;
    }

    public int getOutlierZombieProbability() {
        return outlierZombieProbability;
    }

    public int getOutlierMaskProbability() {
        return outlierMaskProbability;
    }

    public int getOutlierJumperProbability() {
        return outlierJumperProbability;
    }

    public int getOutlierMatuzalemProbability() {
        return outlierMatuzalemProbability;
    }

    public int getOutlierProbability() {
        return outlierProbability;
    }

}
