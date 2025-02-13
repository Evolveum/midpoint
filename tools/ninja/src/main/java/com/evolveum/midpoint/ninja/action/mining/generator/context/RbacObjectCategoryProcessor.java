/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining.generator.context;

import static com.evolveum.midpoint.ninja.action.mining.generator.context.RbacGeneratorUtils.*;
import static com.evolveum.midpoint.ninja.action.mining.generator.context.RbacObjectCategoryProcessor.Category.SALES;
import static com.evolveum.midpoint.ninja.action.mining.generator.context.RbacObjectCategoryProcessor.Category.SECURITY_OFFICERS;

import java.util.Random;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.ninja.action.mining.generator.GeneratorOptions;
import com.evolveum.midpoint.ninja.action.mining.generator.object.InitialBusinessRole;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class RbacObjectCategoryProcessor {
    public enum Category {
        REGULR("Regular User"),
        SEMI_REGULAR("Semi Regular User"),
        IRREGULAR("Irregular User"),
        MANAGERS("Manager"),
        SALES("Sales"),
        SECURITY_OFFICERS("Security Officer"),
        CONTRACTORS("Contractor");

        String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }

    public static Category getRandomCategory(Category category) {
        Random random = new Random();
        Category[] values = Category.values();
        Category randomCategory = category;
        while (randomCategory == category) {
            randomCategory = values[random.nextInt(values.length)];
        }
        return randomCategory;
    }

    public static Category getRandomCategory(Category category, Category category2) {
        Random random = new Random();
        Category[] values = Category.values();
        Category randomCategory = category;
        while (randomCategory == category || randomCategory == category2) {
            randomCategory = values[random.nextInt(values.length)];
        }
        return randomCategory;
    }

    public static void generateRbacData(
            @NotNull RepositoryService repository,
            @NotNull Category category,
            @NotNull Log log,
            @NotNull GeneratorOptions generatorOptions,
            int total, Set<String> names, OperationResult result) {
        switch (category) {
            case REGULR -> {
                new RbacObjectCategoryBuilder.RegularUserType(generatorOptions)
                        .buildAndImportObjects(log, repository, generatorOptions, total, names, result);

                resolveOutliers(Category.REGULR, repository, log, generatorOptions, total, result);
            }

            case SEMI_REGULAR -> {
                new RbacObjectCategoryBuilder.SemiRegularUserType(generatorOptions)
                        .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
                resolveOutliers(Category.SEMI_REGULAR, repository, log, generatorOptions, total, result);
            }
            case IRREGULAR -> {
                new RbacObjectCategoryBuilder.IrregularUserType(generatorOptions)
                        .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
                resolveOutliers(Category.IRREGULAR, repository, log, generatorOptions, total, result);
            }
            case MANAGERS -> {
                new RbacObjectCategoryBuilder.ManagerUserType(generatorOptions)
                        .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
                resolveOutliers(Category.MANAGERS, repository, log, generatorOptions, total, result);
            }
            case SALES -> {
                new RbacObjectCategoryBuilder.SalesUserType(generatorOptions)
                        .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
                resolveOutliers(SALES, repository, log, generatorOptions, total, result);
            }
            case SECURITY_OFFICERS -> {
                new RbacObjectCategoryBuilder.SecurityOfficer(generatorOptions)
                        .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
                resolveOutliers(SECURITY_OFFICERS, repository, log, generatorOptions, total, result);
            }
            case CONTRACTORS -> {
                new RbacObjectCategoryBuilder.Contractor(generatorOptions)
                        .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
                resolveOutliers(Category.CONTRACTORS, repository, log, generatorOptions, total, result);
            }
        }
    }

    private static void resolveOutliers(
            @NotNull RbacObjectCategoryProcessor.Category category,
            @NotNull RepositoryService repository,
            @NotNull Log log,
            @NotNull GeneratorOptions generatorOptions,
            int total,
            OperationResult result) {
        int outlierProbability = generatorOptions.getOutlierProbability();
        if (generatorOptions.getOutlierProbability() != 0.0) {
            int partition = (int) ((double) total / 100) * outlierProbability;

            int outlierZombieProbability = generatorOptions.getOutlierZombieProbability();
            int outlierMatuzalemProbability = generatorOptions.getOutlierMatuzalemProbability();
            int outlierJumperProbability = generatorOptions.getOutlierJumperProbability();
            int outlierMaskProbability = generatorOptions.getOutlierMaskProbability();

            if (outlierMatuzalemProbability != 0) {
                outlierMatuzalemProbability = outlierMatuzalemProbability
                        + outlierZombieProbability;
            }

            if (outlierJumperProbability != 0) {
                outlierJumperProbability = outlierJumperProbability
                        + outlierMatuzalemProbability;
            }

            if (outlierMaskProbability != 0) {
                outlierMaskProbability = outlierMaskProbability
                        + outlierJumperProbability;
            }

            for (int i = 0; i < partition; i++) {
                int probabilityPoint = getProbabilityPoint();

                if (outlierZombieProbability != 0
                        && probabilityPoint <= outlierZombieProbability) {

                    new RbacObjectCategoryBuilder.OutlierZombie(category)
                            .buildAndImport(log, repository, generatorOptions, i, result);

                } else if (outlierMatuzalemProbability != 0
                        && probabilityPoint <= outlierMatuzalemProbability) {

                    new RbacObjectCategoryBuilder.OutlierMatuzalem(category)
                            .buildAndImport(log, repository, generatorOptions, i, result);

                } else if (outlierJumperProbability != 0
                        && probabilityPoint <= outlierJumperProbability) {

                    new RbacObjectCategoryBuilder.OutlierJumper(category)
                            .buildAndImport(log, repository, generatorOptions, i, result);

                } else if (outlierMaskProbability != 0
                        && probabilityPoint <= outlierMaskProbability) {

                    new RbacObjectCategoryBuilder.OutlierMask(category)
                            .buildAndImport(log, repository, generatorOptions, i, result);
                }

            }

            boolean candidate = isCandidate(generatorOptions.getOutlierProbability());
            if (candidate) {
                new RbacObjectCategoryBuilder.OutlierMask(category)
                        .buildAndImport(log, repository, generatorOptions, total, result);
            }

            candidate = isCandidate(generatorOptions.getOutlierProbability());
            if (candidate) {
                new RbacObjectCategoryBuilder.OutlierMatuzalem(category)
                        .buildAndImport(log, repository, generatorOptions, total, result);
            }

            candidate = isCandidate(generatorOptions.getOutlierProbability());
            if (candidate) {
                new RbacObjectCategoryBuilder.OutlierJumper(category)
                        .buildAndImport(log, repository, generatorOptions, total, result);
            }

            candidate = isCandidate(generatorOptions.getOutlierProbability());
            if (candidate) {
                new RbacObjectCategoryBuilder.OutlierZombie(category)
                        .buildAndImport(log, repository, generatorOptions, total, result);
            }
        }
    }

    public static void generateRbacObject(
            @NotNull UserType user,
            @NotNull Category category,
            @NotNull GeneratorOptions generatorOptions) {
        switch (category) {
            case REGULR -> new
                    RbacObjectCategoryBuilder.RegularUserType(generatorOptions)
                    .build(user);
            case SEMI_REGULAR -> new
                    RbacObjectCategoryBuilder.SemiRegularUserType(generatorOptions)
                    .build(user);
            case IRREGULAR -> new
                    RbacObjectCategoryBuilder.IrregularUserType(generatorOptions)
                    .build(user);
            case MANAGERS -> new
                    RbacObjectCategoryBuilder.ManagerUserType(generatorOptions)
                    .build(user);
            case SALES -> new
                    RbacObjectCategoryBuilder.SalesUserType(generatorOptions)
                    .build(user);
            case SECURITY_OFFICERS -> new
                    RbacObjectCategoryBuilder.SecurityOfficer(generatorOptions)
                    .build(user);
            case CONTRACTORS -> new
                    RbacObjectCategoryBuilder.Contractor(generatorOptions)
                    .build(user);
        }
    }

    public static @Nullable String retrieveOrgUnit(@NotNull GeneratorOptions generatorOptions, @NotNull Category category) {
        switch (category) {
            case REGULR -> {
                return new
                        RbacObjectCategoryBuilder.RegularUserType(generatorOptions).getProfessionOrganizationOid();
            }
            case SEMI_REGULAR -> {
                return new
                        RbacObjectCategoryBuilder.SemiRegularUserType(generatorOptions).getProfessionOrganizationOid();
            }
            case IRREGULAR -> {
                return new
                        RbacObjectCategoryBuilder.IrregularUserType(generatorOptions).getProfessionOrganizationOid();
            }
            case MANAGERS -> {
                return new
                        RbacObjectCategoryBuilder.ManagerUserType(generatorOptions).getProfessionOrganizationOid();
            }
            case SALES -> {
                return new
                        RbacObjectCategoryBuilder.SalesUserType(generatorOptions).getProfessionOrganizationOid();
            }
            case SECURITY_OFFICERS -> {
                return new
                        RbacObjectCategoryBuilder.SecurityOfficer(generatorOptions).getProfessionOrganizationOid();
            }
            case CONTRACTORS -> {
                return new
                        RbacObjectCategoryBuilder.Contractor(generatorOptions).getProfessionOrganizationOid();
            }
        }
        return null;
    }

    public static void assignPrimaryAccessByCategory(
            @NotNull UserType user,
            @NotNull Category category,
            @NotNull GeneratorOptions generatorOptions) {
        switch (category) {
            case REGULR -> {
                InitialBusinessRole primaryRole = new RbacObjectCategoryBuilder.RegularUserType(generatorOptions)
                        .getPrimaryRole();
                user.getAssignment().add(createRoleAssignment(primaryRole.getOidValue()));
            }
            case SEMI_REGULAR -> {
                InitialBusinessRole primaryRole = new RbacObjectCategoryBuilder.SemiRegularUserType(generatorOptions)
                        .getPrimaryRole();
                user.getAssignment().add(createRoleAssignment(primaryRole.getOidValue()));
            }
            case IRREGULAR -> {
                InitialBusinessRole primaryRole = new RbacObjectCategoryBuilder.IrregularUserType(generatorOptions)
                        .getPrimaryRole();
                user.getAssignment().add(createRoleAssignment(primaryRole.getOidValue()));
            }
            case MANAGERS -> {
                InitialBusinessRole primaryRole = new RbacObjectCategoryBuilder.ManagerUserType(generatorOptions)
                        .getPrimaryRole();
                user.getAssignment().add(createRoleAssignment(primaryRole.getOidValue()));
            }
            case SALES -> {
                InitialBusinessRole primaryRole = new RbacObjectCategoryBuilder.SalesUserType(generatorOptions)
                        .getPrimaryRole();
                user.getAssignment().add(createRoleAssignment(primaryRole.getOidValue()));
            }
            case SECURITY_OFFICERS -> {
                InitialBusinessRole primaryRole = new RbacObjectCategoryBuilder.SecurityOfficer(generatorOptions)
                        .getPrimaryRole();
                user.getAssignment().add(createRoleAssignment(primaryRole.getOidValue()));
            }
            case CONTRACTORS -> {
                InitialBusinessRole primaryRole = new RbacObjectCategoryBuilder.Contractor(generatorOptions)
                        .getPrimaryRole();
                user.getAssignment().add(createRoleAssignment(primaryRole.getOidValue()));
            }
        }
    }

}
