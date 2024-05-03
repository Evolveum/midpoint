/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining.generator.context;

import static com.evolveum.midpoint.ninja.action.mining.generator.context.ImportAction.getNameFromSet;
import static com.evolveum.midpoint.ninja.action.mining.generator.context.ImportAction.importUserAndResolveAuxRoles;
import static com.evolveum.midpoint.ninja.action.mining.generator.context.RbacGeneratorUtils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.action.mining.generator.GeneratorOptions;
import com.evolveum.midpoint.ninja.action.mining.generator.object.InitialObjectsDefinition;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

enum RbacObjectCategoryProcessor {
    REGULR("Regular User"),
    SEMI_REGULAR("Semi Regular User"),
    IRREGULAR("Irregular User"),
    MANAGERS("Manager"),
    SALES("Sales"),
    SECURITY_OFFICERS("Security Officer"),
    CONTRACTORS("Contractor");
    String displayName;

    RbacObjectCategoryProcessor(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public static RbacObjectCategoryProcessor getRandomCategory(RbacObjectCategoryProcessor category) {
        Random random = new Random();
        RbacObjectCategoryProcessor[] values = values();
        RbacObjectCategoryProcessor randomCategory = category;
        while (randomCategory == category) {
            randomCategory = values[random.nextInt(values.length)];
        }
        return randomCategory;
    }

    public static RbacObjectCategoryProcessor getRandomCategory(RbacObjectCategoryProcessor category, RbacObjectCategoryProcessor category2) {
        Random random = new Random();
        RbacObjectCategoryProcessor[] values = values();
        RbacObjectCategoryProcessor randomCategory = category;
        while (randomCategory == category || randomCategory == category2) {
            randomCategory = values[random.nextInt(values.length)];
        }
        return randomCategory;
    }

    public void generateRbacData(@NotNull RepositoryService repository,
            @NotNull Log log,
            @NotNull GeneratorOptions generatorOptions,
            int total, Set<String> names, OperationResult result) {
        switch (this) {
            case REGULR -> {
                new RegularUser()
                        .buildAndImportObjects(log, repository, generatorOptions, total, names, result);

                resolveOutliers(REGULR, repository, log, generatorOptions, total, names, result);
            }

            case SEMI_REGULAR -> {
                new SemiRegularUser(generatorOptions)
                        .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
                resolveOutliers(SEMI_REGULAR, repository, log, generatorOptions, total, names, result);
            }
            case IRREGULAR -> {
                new IrregularUser(generatorOptions)
                        .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
                resolveOutliers(IRREGULAR, repository, log, generatorOptions, total, names, result);
            }
            case MANAGERS -> {
                new ManagerUser(generatorOptions)
                        .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
                resolveOutliers(MANAGERS, repository, log, generatorOptions, total, names, result);
            }
            case SALES -> {
                new SalesUser(generatorOptions)
                        .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
                resolveOutliers(SALES, repository, log, generatorOptions, total, names, result);
            }
            case SECURITY_OFFICERS -> {
                new SecurityOfficer()
                        .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
                resolveOutliers(SECURITY_OFFICERS, repository, log, generatorOptions, total, names, result);
            }
            case CONTRACTORS -> {
                new Contractor(generatorOptions)
                        .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
                resolveOutliers(CONTRACTORS, repository, log, generatorOptions, total, names, result);
            }
        }
    }

    private static void resolveOutliers(
            @NotNull RbacObjectCategoryProcessor category,
            @NotNull RepositoryService repository,
            @NotNull Log log,
            @NotNull GeneratorOptions generatorOptions,
            int total,
            Set<String> names,
            OperationResult result) {
        if (generatorOptions.getOutlierProbability() != 0.0) {
            boolean candidate = isCandidate(generatorOptions.getOutlierProbability());
            if (candidate) {
                new OutlierMask(category)
                        .buildAndImport(log, repository, generatorOptions, total, result);
            }

            candidate = isCandidate(generatorOptions.getOutlierProbability());
            if (candidate) {
                new OutlierMatuzalem(category)
                        .buildAndImport(log, repository, generatorOptions, total, result);
            }

            candidate = isCandidate(generatorOptions.getOutlierProbability());
            if (candidate) {
                new OutlierJumper(category)
                        .buildAndImport(log, repository, generatorOptions, total, result);
            }

            candidate = isCandidate(generatorOptions.getOutlierProbability());
            if (candidate) {
                new OutlierZombie(category)
                        .buildAndImport(log, repository, generatorOptions, total, result);
            }
        }
    }

    public void generateRbacObject(
            @NotNull UserType user,
            boolean limited,
            boolean partialLimited,
            @NotNull GeneratorOptions generatorOptions) {
        switch (this) {
            case REGULR -> new
                    RegularUser()
                    .build(user);
            case SEMI_REGULAR -> new
                    SemiRegularUser(generatorOptions)
                    .build(user, limited);
            case IRREGULAR -> new
                    IrregularUser(generatorOptions)
                    .build(user);
            case MANAGERS -> new
                    ManagerUser(generatorOptions)
                    .build(user, limited);
            case SALES -> new
                    SalesUser(generatorOptions)
                    .build(user, limited, partialLimited);
            case SECURITY_OFFICERS -> new
                    SecurityOfficer()
                    .build(user);
            case CONTRACTORS -> new
                    Contractor(generatorOptions)
                    .build(user);
        }
    }

    public void assignAccessByCategory(@NotNull UserType user, @NotNull GeneratorOptions generatorOptions, boolean includePlancton) {
        switch (this) {
            case REGULR -> new RegularUser()
                    .assignAccess(user);
            case SEMI_REGULAR -> new SemiRegularUser(generatorOptions)
                    .assignAccess(user, includePlancton);
            case IRREGULAR -> new IrregularUser(generatorOptions)
                    .assignAccess(user, includePlancton);
            case MANAGERS -> new ManagerUser(generatorOptions)
                    .assignAccess(user, includePlancton);
            case SALES -> new SalesUser(generatorOptions)
                    .assignAccess(user, false, false);
            case SECURITY_OFFICERS -> new SecurityOfficer()
                    .assignAccess(user);
            case CONTRACTORS -> new Contractor(generatorOptions)
                    .assignAccess(user, includePlancton);
        }
    }

    /**
     * This class represents a Regular User in the system.
     * It contains methods to build a UserType object with attributes specific to a Regular User.
     */
    public static class RegularUser {

        String organizationOid = InitialObjectsDefinition.Organization.REGULAR.getOidValue();
        String birthEmployeeRole = InitialObjectsDefinition.BirthrightBusinessRole.EMPLOYEE.getOidValue();
        String archetypeOid = InitialObjectsDefinition.Archetypes.REGULAR_USER.getOidValue();

        /**
         * Default constructor for the RegularUser class.
         */
        public RegularUser() {
        }

        /**
         * This method builds a UserType object with attributes specific to a Regular User.
         * It assigns a name, locality, title, and assignments to the user.
         * It also sets up the archetype for the user.
         *
         * @param user The UserType object to be built.
         * @return The built UserType object.
         */
        public UserType build(@NotNull UserType user) {
            InitialObjectsDefinition.LocationInitialBusinessRole randomLocationBusinessRole = getRandomLocationBusinessRole();
            String locationBusinessRoleOidValue = randomLocationBusinessRole.getOidValue();

            PolyStringType locale = PolyStringType.fromOrig(randomLocationBusinessRole.getLocale());

            InitialObjectsDefinition.JobInitialBusinessRole randomJobBusinessRole = getRandomJobBusinessRole();
            String randomJobBusinessRoleOidValue = randomJobBusinessRole.getOidValue();

            String randomlyJobTitleStructure = getRandomlyJobTitles();

            user.setLocality(locale);
            user.setTitle(PolyStringType.fromOrig(randomlyJobTitleStructure));

            assignAccess(user, locationBusinessRoleOidValue, randomJobBusinessRoleOidValue);

            user.getAssignment().add(createOrgAssignment(organizationOid));

            setUpArchetypeUser(user, archetypeOid);

            return user;
        }

        private void assignAccess(@NotNull UserType user, String locationBusinessRoleOidValue, String randomJobBusinessRoleOidValue) {
            user.getAssignment().add(createRoleAssignment(birthEmployeeRole));
            user.getAssignment().add(createRoleAssignment(locationBusinessRoleOidValue));
            user.getAssignment().add(createRoleAssignment(randomJobBusinessRoleOidValue));
        }

        private void assignAccess(@NotNull UserType user) {
            InitialObjectsDefinition.LocationInitialBusinessRole randomLocationBusinessRole = getRandomLocationBusinessRole();
            String locationBusinessRoleOidValue = randomLocationBusinessRole.getOidValue();
            InitialObjectsDefinition.JobInitialBusinessRole randomJobBusinessRole = getRandomJobBusinessRole();
            String randomJobBusinessRoleOidValue = randomJobBusinessRole.getOidValue();

            assignAccess(user, locationBusinessRoleOidValue, randomJobBusinessRoleOidValue);
        }

        /**
         * This method is responsible for building and importing objects for Regular Users.
         * It logs the progress of the import operation and uses the build() method to construct each user.
         * After building a user, it imports the user and resolves auxiliary roles.
         *
         * @param log The logger used to log the progress of the import operation.
         * @param repository The repository service used to import the user and resolve auxiliary roles.
         * @param generatorOptions The options for the generator, used in the importUserAndResolveAuxRoles method.
         * @param total The total number of users to be imported.
         * @param names A set of names to be used for the users. A name is selected from this set for each user.
         * @param result The operation result, used in the importUserAndResolveAuxRoles method.
         */
        public void buildAndImportObjects(
                @NotNull Log log,
                @NotNull RepositoryService repository,
                @NotNull GeneratorOptions generatorOptions,
                int total, Set<String> names,
                @NotNull OperationResult result) {
            String displayName = REGULR.getDisplayName();
            log.info("Importing " + displayName + ": 0/{}", total);
            for (int i = 0; i < total; i++) {
                log.info("Importing " + displayName + ": {}/{}", i + 1, total);
                UserType user = new UserType();
                user.setName(getNameFromSet(PolyStringType.fromOrig(displayName + " User " + i), names));
                user = build(user);
                importUserAndResolveAuxRoles(user, repository, generatorOptions, result, log);
            }
        }
    }

    /**
     * This class represents a Semi Regular User in the system.
     * It contains methods to build a UserType object with attributes specific to a Semi Regular User.
     */
    public static class SemiRegularUser {
        String organizationOid = InitialObjectsDefinition.Organization.SEMI_REGULAR.getOidValue();
        String birthEmployeeRole = InitialObjectsDefinition.BirthrightBusinessRole.EMPLOYEE.getOidValue();
        String archetypeOid = InitialObjectsDefinition.Archetypes.SEMI_REGULAR_USER.getOidValue();
        GeneratorOptions generatorOptions;

        public SemiRegularUser(GeneratorOptions generatorOptions) {
            this.generatorOptions = generatorOptions;
        }

        /**
         * This method builds a UserType object with attributes specific to a Semi Regular User.
         * It assigns a name, locality, title, and assignments to the user.
         * It also sets up the archetype for the user.
         *
         * @param user The UserType object to be built.
         * @param limited A boolean value that determines if the user is limited.
         * @return The built UserType object.
         */
        public UserType build(@NotNull UserType user, boolean limited) {

            InitialObjectsDefinition.LocationInitialBusinessRole randomLocationBusinessRole = getRandomLocationBusinessRole();
            String locationBusinessRoleOidValue = randomLocationBusinessRole.getOidValue();
            PolyStringType locale = PolyStringType.fromOrig(randomLocationBusinessRole.getLocale());
            String randomlyJobTitleStructure = getRandomlyJobTitlesWithNone();
            List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> randomPlanktonRoles = getRandomPlanktonRoles(
                    0, generatorOptions);

            if (!limited) {
                user.setLocality(locale);
            }

            if (!randomlyJobTitleStructure.isEmpty()) {
                user.setTitle(PolyStringType.fromOrig(randomlyJobTitleStructure));
            }

            assignAccess(user, randomPlanktonRoles, locationBusinessRoleOidValue);

            user.getAssignment().add(createOrgAssignment(organizationOid));

            setUpArchetypeUser(user, archetypeOid);

            return user;
        }

        private void assignAccess(@NotNull UserType user,
                @NotNull List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> randomPlanktonRoles,
                String locationBusinessRoleOidValue) {
            for (InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole randomPlanktonRole : randomPlanktonRoles) {
                user.getAssignment().add(createRoleAssignment(randomPlanktonRole.getOidValue()));
            }
            user.getAssignment().add(createRoleAssignment(birthEmployeeRole));
            user.getAssignment().add(createRoleAssignment(locationBusinessRoleOidValue));
        }

        private void assignAccess(@NotNull UserType user, boolean includePlancton) {
            List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> randomPlanktonRoles = getRandomPlanktonRoles(
                    0, generatorOptions);
            InitialObjectsDefinition.LocationInitialBusinessRole randomLocationBusinessRole = getRandomLocationBusinessRole();
            String locationBusinessRoleOidValue = randomLocationBusinessRole.getOidValue();
            if (!includePlancton) {
                assignAccess(user, new ArrayList<>(), locationBusinessRoleOidValue);
                return;
            }
            assignAccess(user, randomPlanktonRoles, locationBusinessRoleOidValue);
        }

        /**
         * This method is responsible for building and importing objects for Semi Regular Users.
         * It logs the progress of the import operation and uses the build() method to construct each user.
         * After building a user, it imports the user and resolves auxiliary roles.
         *
         * @param log The logger used to log the progress of the import operation.
         * @param repository The repository service used to import the user and resolve auxiliary roles.
         * @param generatorOptions The options for the generator, used in the importUserAndResolveAuxRoles method.
         * @param total The total number of users to be imported.
         * @param names A set of names to be used for the users. A name is selected from this set for each user.
         * @param result The operation result, used in the importUserAndResolveAuxRoles method.
         */
        public void buildAndImportObjects(
                @NotNull Log log,
                @NotNull RepositoryService repository,
                @NotNull GeneratorOptions generatorOptions,
                int total, Set<String> names,
                @NotNull OperationResult result) {
            int ninetyPercent = (int) (total * 0.9);

            String displayName = SEMI_REGULAR.getDisplayName();
            log.info("Importing " + displayName + ": 0/{}", total);
            for (int i = 0; i < total; i++) {
                log.info("Importing " + displayName + ": {}/{}", i + 1, total);
                UserType user = new UserType();
                user.setName(getNameFromSet(PolyStringType.fromOrig(displayName + " User " + i), names));

                if (i < ninetyPercent) {
                    user = build(user, false);
                } else {
                    user = build(user, true);
                }

                importUserAndResolveAuxRoles(user, repository, generatorOptions, result, log);
            }
        }
    }

    /**
     * This class represents an Irregular User in the system.
     * It contains methods to build a UserType object with attributes specific to an Irregular User.
     */
    public static class IrregularUser {
        String organizationOid = InitialObjectsDefinition.Organization.IRREGULAR.getOidValue();
        String birthEmployeeRole = InitialObjectsDefinition.BirthrightBusinessRole.EMPLOYEE.getOidValue();
        String archetypeOid = InitialObjectsDefinition.Archetypes.IRREGULAR_USER.getOidValue();

        GeneratorOptions generatorOptions;

        public IrregularUser(GeneratorOptions generatorOptions) {
            this.generatorOptions = generatorOptions;
        }

        /**
         * This method builds a UserType object with attributes specific to an Irregular User.
         * It assigns a name, title, and assignments to the user.
         * It also sets up the archetype for the user.
         *
         * @param user The UserType object to be built.
         * @return The built UserType object.
         */
        public UserType build(@NotNull UserType user) {
            String randomlyJobTitleStructureWithNone = getRandomlyJobTitlesWithNone();
            List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> randomPlanktonRoles = getRandomPlanktonRoles(
                    7, generatorOptions);
            user.getAssignment().add(createOrgAssignment(organizationOid));

            if (!randomlyJobTitleStructureWithNone.isEmpty()) {
                user.setTitle(PolyStringType.fromOrig(randomlyJobTitleStructureWithNone));
            }

            assignAccess(user, randomPlanktonRoles);

            setUpArchetypeUser(user, archetypeOid);
            return user;
        }

        private void assignAccess(@NotNull UserType user,
                @NotNull List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> randomPlanktonRoles) {
            user.getAssignment().add(createRoleAssignment(birthEmployeeRole));
            for (InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole randomPlanktonRole : randomPlanktonRoles) {
                user.getAssignment().add(createRoleAssignment(randomPlanktonRole.getOidValue()));
            }
        }

        private void assignAccess(@NotNull UserType user, boolean includePlancton) {
            List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> randomPlanktonRoles = getRandomPlanktonRoles(
                    7, generatorOptions);
            if (!includePlancton) {
                assignAccess(user, new ArrayList<>());
                return;
            }
            assignAccess(user, randomPlanktonRoles);
        }

        /**
         * This method is responsible for building and importing objects for Irregular Users.
         * It logs the progress of the import operation and uses the build() method to construct each user.
         * After building a user, it imports the user and resolves auxiliary roles.
         *
         * @param log The logger used to log the progress of the import operation.
         * @param repository The repository service used to import the user and resolve auxiliary roles.
         * @param generatorOptions The options for the generator, used in the importUserAndResolveAuxRoles method.
         * @param total The total number of users to be imported.
         * @param names A set of names to be used for the users. A name is selected from this set for each user.
         * @param result The operation result, used in the importUserAndResolveAuxRoles method.
         */
        public void buildAndImportObjects(
                @NotNull Log log,
                @NotNull RepositoryService repository,
                @NotNull GeneratorOptions generatorOptions,
                int total, Set<String> names,
                @NotNull OperationResult result) {
            String displayName = IRREGULAR.getDisplayName();
            log.info("Importing " + displayName + ": 0/{}", total);
            for (int i = 0; i < total; i++) {
                log.info("Importing " + displayName + ": {}/{}", i + 1, total);
                UserType user = new UserType();
                user.setName(getNameFromSet(PolyStringType.fromOrig(displayName + " User " + i), names));
                user = build(user);
                importUserAndResolveAuxRoles(user, repository, generatorOptions, result, log);
            }
        }
    }

    /**
     * This class represents a Manager User in the system.
     * It contains methods to build a UserType object with attributes specific to a Manager User.
     */
    public static class ManagerUser {
        String organizationOid = InitialObjectsDefinition.Organization.MANAGERS.getOidValue();
        String birthEmployeeRole = InitialObjectsDefinition.BirthrightBusinessRole.EMPLOYEE.getOidValue();
        String archetypeOid = InitialObjectsDefinition.Archetypes.MANAGERS_USER.getOidValue();

        GeneratorOptions generatorOptions;

        /**
         * Default constructor for the ManagerUser class.
         */
        public ManagerUser(GeneratorOptions generatorOptions) {
            this.generatorOptions = generatorOptions;
        }

        /**
         * This method builds a UserType object with attributes specific to a Manager User.
         * It assigns a name, title, and assignments to the user.
         * It also sets up the archetype for the user.
         *
         * @param user The UserType object to be built.
         * @param limited A boolean value that determines if the user is limited.
         * @return The built UserType object.
         */
        public UserType build(@NotNull UserType user, boolean limited) {
            InitialObjectsDefinition.JobInitialBusinessRole managerRole = InitialObjectsDefinition.JobInitialBusinessRole.MANAGER;
            String jobTitle = "manager";

            InitialObjectsDefinition.LocationInitialBusinessRole randomLocationBusinessRole = getRandomLocationBusinessRole();
            String locationBusinessRoleOidValue = randomLocationBusinessRole.getOidValue();
            PolyStringType locale = PolyStringType.fromOrig(randomLocationBusinessRole.getLocale());
            List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> randomPlanktonRoles = getRandomPlanktonRoles(
                    0, generatorOptions);

            user.setTitle(PolyStringType.fromOrig(jobTitle));
            user.getAssignment().add(createOrgAssignment(organizationOid));

            if (!limited) {
                user.setLocality(locale);
            }

            assignAccess(user, managerRole, locationBusinessRoleOidValue, randomPlanktonRoles);

            setUpArchetypeUser(user, archetypeOid);

            return user;
        }

        private void assignAccess(@NotNull UserType user,
                InitialObjectsDefinition.@NotNull JobInitialBusinessRole managerRole,
                @NotNull String locationBusinessRoleOidValue,
                @NotNull List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> randomPlanktonRoles) {
            user.getAssignment().add(createRoleAssignment(birthEmployeeRole));
            user.getAssignment().add(createRoleAssignment(managerRole.getOidValue()));
            user.getAssignment().add(createRoleAssignment(locationBusinessRoleOidValue));
            for (InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole randomPlanktonRole : randomPlanktonRoles) {
                user.getAssignment().add(createRoleAssignment(randomPlanktonRole.getOidValue()));
            }
        }

        private void assignAccess(@NotNull UserType user, boolean includePlancton) {
            InitialObjectsDefinition.JobInitialBusinessRole managerRole = InitialObjectsDefinition.JobInitialBusinessRole.MANAGER;
            InitialObjectsDefinition.LocationInitialBusinessRole randomLocationBusinessRole = getRandomLocationBusinessRole();
            String locationBusinessRoleOidValue = randomLocationBusinessRole.getOidValue();
            List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> randomPlanktonRoles = getRandomPlanktonRoles(
                    0, generatorOptions);
            if (!includePlancton) {
                assignAccess(user, managerRole, locationBusinessRoleOidValue, new ArrayList<>());
                return;
            }
            assignAccess(user, managerRole, locationBusinessRoleOidValue, randomPlanktonRoles);
        }

        /**
         * This method is responsible for building and importing objects for Manager Users.
         * It logs the progress of the import operation and uses the build() method to construct each user.
         * After building a user, it imports the user and resolves auxiliary roles.
         *
         * @param log The logger used to log the progress of the import operation.
         * @param repository The repository service used to import the user and resolve auxiliary roles.
         * @param generatorOptions The options for the generator, used in the importUserAndResolveAuxRoles method.
         * @param total The total number of users to be imported.
         * @param names A set of names to be used for the users. A name is selected from this set for each user.
         * @param result The operation result, used in the importUserAndResolveAuxRoles method.
         */
        public void buildAndImportObjects(
                @NotNull Log log,
                @NotNull RepositoryService repository,
                @NotNull GeneratorOptions generatorOptions,
                int total, Set<String> names,
                @NotNull OperationResult result) {
            int ninetyPercent = (int) (total * 0.9);
            String displayName = MANAGERS.getDisplayName();
            log.info("Importing " + displayName + ": 0/{}", total);
            for (int i = 0; i < total; i++) {
                log.info("Importing " + displayName + ": {}/{}", i + 1, total);
                UserType user = new UserType();
                user.setName(getNameFromSet(PolyStringType.fromOrig(displayName + " User " + i), names));

                if (i < ninetyPercent) {
                    user = build(user, false);
                } else {
                    user = build(user, true);
                }

                importUserAndResolveAuxRoles(user, repository, generatorOptions, result, log);
            }
        }
    }

    /**
     * This class represents a Sales User in the system.
     * It contains methods to build a UserType object with attributes specific to a Sales User.
     */
    public static class SalesUser {
        String organizationOid = InitialObjectsDefinition.Organization.SALES.getOidValue();
        String birthEmployeeRole = InitialObjectsDefinition.BirthrightBusinessRole.EMPLOYEE.getOidValue();
        String archetypeOid = InitialObjectsDefinition.Archetypes.SALES_USER.getOidValue();

        GeneratorOptions generatorOptions;

        public SalesUser(GeneratorOptions generatorOptions) {
            this.generatorOptions = generatorOptions;
        }

        /**
         * This method builds a UserType object with attributes specific to a Sales User.
         * It assigns a name, title, and assignments to the user.
         * It also sets up the archetype for the user.
         *
         * @param user The UserType object to be built.
         * @param limited A boolean value that determines if the user is limited.
         * @param partialLimited A boolean value that determines if the user is partially limited.
         * @return The built UserType object.
         */
        public UserType build(@NotNull UserType user, boolean limited, boolean partialLimited) {
            InitialObjectsDefinition.JobInitialBusinessRole salesBr = InitialObjectsDefinition.JobInitialBusinessRole.SALES;
            InitialObjectsDefinition.LocationInitialBusinessRole locationNewYorkBr = InitialObjectsDefinition
                    .LocationInitialBusinessRole.LOCATION_NEW_YORK;
            String jobTitle = "salesperson";

            InitialObjectsDefinition.LocationInitialBusinessRole randomLocationBusinessRole = getRandomLocationBusinessRole();

            user.getAssignment().add(createOrgAssignment(organizationOid));

            assignAccess(user, limited, partialLimited, salesBr, locationNewYorkBr, randomLocationBusinessRole);

            if (!limited) {
                user.setTitle(PolyStringType.fromOrig(jobTitle));

                if (!partialLimited) {
                    user.setLocality(PolyStringType.fromOrig(locationNewYorkBr.getLocale()));
                } else {
                    user.setLocality(PolyStringType.fromOrig(randomLocationBusinessRole.getLocale()));
                }
            }

            setUpArchetypeUser(user, archetypeOid);

            return user;
        }

        private void assignAccess(@NotNull UserType user, boolean limited, boolean partialLimited,
                InitialObjectsDefinition.@NotNull JobInitialBusinessRole salesBr,
                InitialObjectsDefinition.LocationInitialBusinessRole locationNewYorkBr,
                InitialObjectsDefinition.LocationInitialBusinessRole randomLocationBusinessRole) {
            user.getAssignment().add(createRoleAssignment(salesBr.getOidValue()));
            user.getAssignment().add(createRoleAssignment(birthEmployeeRole));

            if (!limited) {
                if (!partialLimited) {
                    user.getAssignment().add(createRoleAssignment(locationNewYorkBr.getOidValue()));
                } else {
                    user.getAssignment().add(createRoleAssignment(randomLocationBusinessRole.getOidValue()));
                }
            }
        }

        private void assignAccess(@NotNull UserType user, boolean limited, boolean partialLimited) {
            InitialObjectsDefinition.JobInitialBusinessRole salesBr = InitialObjectsDefinition.JobInitialBusinessRole.SALES;
            InitialObjectsDefinition.LocationInitialBusinessRole locationNewYorkBr = InitialObjectsDefinition
                    .LocationInitialBusinessRole.LOCATION_NEW_YORK;
            InitialObjectsDefinition.LocationInitialBusinessRole randomLocationBusinessRole = getRandomLocationBusinessRole();

            assignAccess(user, limited, partialLimited, salesBr, locationNewYorkBr, randomLocationBusinessRole);
        }

        /**
         * This method is responsible for building and importing objects for Sales Users.
         * It logs the progress of the import operation and uses the build() method to construct each user.
         * After building a user, it imports the user and resolves auxiliary roles.
         *
         * @param log The logger used to log the progress of the import operation.
         * @param repository The repository service used to import the user and resolve auxiliary roles.
         * @param generatorOptions The options for the generator, used in the importUserAndResolveAuxRoles method.
         * @param total The total number of users to be imported.
         * @param names A set of names to be used for the users. A name is selected from this set for each user.
         * @param result The operation result, used in the importUserAndResolveAuxRoles method.
         */
        public void buildAndImportObjects(
                @NotNull Log log,
                @NotNull RepositoryService repository,
                @NotNull GeneratorOptions generatorOptions,
                int total, Set<String> names,
                @NotNull OperationResult result) {
            int ninetyPercent = (int) (total * 0.9);
            int seventyPercent = (int) (total * 0.7);
            String displayName = MANAGERS.getDisplayName();
            log.info("Importing " + displayName + ": 0/{}", total);
            for (int i = 0; i < total; i++) {
                log.info("Importing " + displayName + ": {}/{}", i + 1, total);
                UserType user = new UserType();
                user.setName(getNameFromSet(PolyStringType.fromOrig(displayName + " User " + i), names));
                boolean limited = true;
                boolean partialLimited = true;
                if (i < ninetyPercent) {
                    limited = false;
                    if (i < seventyPercent) {
                        partialLimited = false;
                    }
                }
                user = build(user, limited, partialLimited);

                importUserAndResolveAuxRoles(user, repository, generatorOptions, result, log);
            }
        }
    }

    /**
     * This class represents a Security Officer in the system.
     * It contains methods to build a UserType object with attributes specific to a Security Officer.
     */
    public static class SecurityOfficer {
        String organizationOid = InitialObjectsDefinition.Organization.SECURITY_OFFICERS.getOidValue();
        String birthEmployeeRole = InitialObjectsDefinition.BirthrightBusinessRole.EMPLOYEE.getOidValue();
        InitialObjectsDefinition.JobInitialBusinessRole securityOfficerRole = InitialObjectsDefinition
                .JobInitialBusinessRole.SECURITY_OFFICER;
        String securityOfficerRoleOidValue = securityOfficerRole.getOidValue();
        String archetypeOid = InitialObjectsDefinition.Archetypes.SECURITY_OFFICERS_USER.getOidValue();

        /**
         * Default constructor for the SecurityOfficer class.
         */
        public UserType build(@NotNull UserType user) {
            user.getAssignment().add(createOrgAssignment(organizationOid));

            assignAccess(user);

            setUpArchetypeUser(user, archetypeOid);

            return user;
        }

        private void assignAccess(@NotNull UserType user) {
            user.getAssignment().add(createRoleAssignment(birthEmployeeRole));
            user.getAssignment().add(createRoleAssignment(securityOfficerRoleOidValue));
        }

        /**
         * This method is responsible for building and importing objects for Security Officers.
         * It logs the progress of the import operation and uses the build() method to construct each user.
         * After building a user, it imports the user and resolves auxiliary roles.
         *
         * @param log The logger used to log the progress of the import operation.
         * @param repository The repository service used to import the user and resolve auxiliary roles.
         * @param generatorOptions The options for the generator, used in the importUserAndResolveAuxRoles method.
         * @param total The total number of users to be imported.
         * @param names A set of names to be used for the users. A name is selected from this set for each user.
         * @param result The operation result, used in the importUserAndResolveAuxRoles method.
         */
        public void buildAndImportObjects(
                @NotNull Log log,
                @NotNull RepositoryService repository,
                @NotNull GeneratorOptions generatorOptions,
                int total, Set<String> names,
                @NotNull OperationResult result) {
            String displayName = SECURITY_OFFICERS.getDisplayName();
            log.info("Importing " + displayName + ": 0/{}", total);
            for (int i = 0; i < total; i++) {
                log.info("Importing " + displayName + ": {}/{}", i + 1, total);
                UserType user = new UserType();
                user.setName(getNameFromSet(PolyStringType.fromOrig(displayName + " User " + i), names));
                user = build(user);
                importUserAndResolveAuxRoles(user, repository, generatorOptions, result, log);
            }
        }

    }

    /**
     * This class represents a Contractor in the system.
     * It contains methods to build a UserType object with attributes specific to a Contractor.
     */
    public static class Contractor {
        String organizationOid = InitialObjectsDefinition.Organization.CONTRACTORS.getOidValue();
        String birthContractorRole = InitialObjectsDefinition.BirthrightBusinessRole.CONTRACTOR.getOidValue();
        String archetypeOid = InitialObjectsDefinition.Archetypes.CONTRACTORS_USER.getOidValue();

        GeneratorOptions generatorOptions;

        /**
         * Default constructor for the Contractor class.
         */
        public Contractor(GeneratorOptions generatorOptions) {
            this.generatorOptions = generatorOptions;
        }

        /**
         * This method builds a UserType object with attributes specific to a Contractor.
         * It assigns a name, title, and assignments to the user.
         * It also sets up the archetype for the user.
         *
         * @param user The UserType object to be built.
         * @return The built UserType object.
         */
        public UserType build(@NotNull UserType user) {
            List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> randomPlanktonRoles = getRandomPlanktonRoles(
                    0, generatorOptions);

            user.getAssignment().add(createOrgAssignment(organizationOid));

            assignAccess(user, randomPlanktonRoles);

            setUpArchetypeUser(user, archetypeOid);

            return user;
        }

        private void assignAccess(@NotNull UserType user,
                boolean randomPlanktonRoles) {
            assignAccess(user, randomPlanktonRoles ? getRandomPlanktonRoles(0, generatorOptions) : new ArrayList<>());
        }

        private void assignAccess(@NotNull UserType user,
                @NotNull List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> randomPlanktonRoles) {
            user.getAssignment().add(createRoleAssignment(birthContractorRole));
            for (InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole randomPlanktonRole : randomPlanktonRoles) {
                user.getAssignment().add(createRoleAssignment(randomPlanktonRole.getOidValue()));
            }
        }

        /**
         * This method is responsible for building and importing objects for Contractors.
         * It logs the progress of the import operation and uses the build() method to construct each user.
         * After building a user, it imports the user and resolves auxiliary roles.
         *
         * @param log The logger used to log the progress of the import operation.
         * @param repository The repository service used to import the user and resolve auxiliary roles.
         * @param generatorOptions The options for the generator, used in the importUserAndResolveAuxRoles method.
         * @param total The total number of users to be imported.
         * @param names A set of names to be used for the users. A name is selected from this set for each user.
         * @param result The operation result, used in the importUserAndResolveAuxRoles method.
         */
        public void buildAndImportObjects(
                @NotNull Log log,
                @NotNull RepositoryService repository,
                @NotNull GeneratorOptions generatorOptions,
                int total, Set<String> names,
                @NotNull OperationResult result) {
            String displayName = CONTRACTORS.getDisplayName();
            log.info("Importing " + displayName + ": 0/{}", total);
            for (int i = 0; i < total; i++) {
                log.info("Importing " + displayName + ": {}/{}", i + 1, total);
                UserType user = new UserType();
                user.setName(getNameFromSet(PolyStringType.fromOrig(displayName + " User " + i), names));
                user = build(user);
                importUserAndResolveAuxRoles(user, repository, generatorOptions, result, log);
            }
        }

    }

    public static class OutlierMatuzalem {

        RbacObjectCategoryProcessor category;

        public OutlierMatuzalem(RbacObjectCategoryProcessor category) {
            this.category = category;
        }

        public void buildAndImport(
                @NotNull Log log,
                @NotNull RepositoryService repository,
                @NotNull GeneratorOptions generatorOptions,
                int total,
                @NotNull OperationResult result) {

            int outlierMatuzalemProbability = generatorOptions.getOutlierMatuzalemProbability();
            int partition = (int) ((double) total / 100) * outlierMatuzalemProbability;

            for (int i = 0; i < partition; i++) {
                UserType user = new UserType();
                user.setName(PolyStringType.fromOrig(category.getDisplayName() + "Matuzalem" + i));
                category.generateRbacObject(user, false, false, generatorOptions);
                RbacObjectCategoryProcessor randomCategory = getRandomCategory(category);
                randomCategory.assignAccessByCategory(user, generatorOptions, false);
                RbacObjectCategoryProcessor randomCategory2 = getRandomCategory(category, randomCategory);
                randomCategory2.assignAccessByCategory(user, generatorOptions, false);
                RbacObjectCategoryProcessor randomCategory3 = getRandomCategory(category);
                if (randomCategory3 != randomCategory2) {
                    randomCategory3.assignAccessByCategory(user, generatorOptions, false);
                }
                importUserAndResolveAuxRoles(user, repository, generatorOptions, result, log);
            }

        }

    }

    public static class OutlierJumper {
        RbacObjectCategoryProcessor category;

        public OutlierJumper(RbacObjectCategoryProcessor category) {
            this.category = category;
        }

        public void buildAndImport(
                @NotNull Log log,
                @NotNull RepositoryService repository,
                @NotNull GeneratorOptions generatorOptions,
                int total,
                @NotNull OperationResult result) {

            int outlierJumperProbability = generatorOptions.getOutlierJumperProbability();
            int partition = (int) ((double) total / 100) * outlierJumperProbability;

            for (int i = 0; i < partition; i++) {
                UserType user = new UserType();
                user.setName(PolyStringType.fromOrig(category.getDisplayName() + "Jumper" + i));
                category.generateRbacObject(user, false, false, generatorOptions);
                RbacObjectCategoryProcessor randomCategory = getRandomCategory(category);
                randomCategory.assignAccessByCategory(user, generatorOptions, false);

                importUserAndResolveAuxRoles(user, repository, generatorOptions, result, log);
            }

        }

    }

    //TODO fixme
    public static class OutlierZombie {

        RbacObjectCategoryProcessor category;

        public OutlierZombie(RbacObjectCategoryProcessor category) {
            this.category = category;
        }

        public void buildAndImport(
                @NotNull Log log,
                @NotNull RepositoryService repository,
                @NotNull GeneratorOptions generatorOptions,
                int total,
                @NotNull OperationResult result) {

            int outlierZombieProbability = generatorOptions.getOutlierZombieProbability();
            int partition = (int) ((double) total / 100) * outlierZombieProbability;

            for (int i = 0; i < partition; i++) {
                UserType user = new UserType();
                user.setName(PolyStringType.fromOrig(category.getDisplayName() + "Zombie" + i));
                category.generateRbacObject(user, false, false, generatorOptions);
                user.getAssignment().clear();
                List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> randomPlanktonRoles = getRandomPlanktonRoles(10);
                for (InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole randomPlanktonRole : randomPlanktonRoles) {
                    user.getAssignment().add(createRoleAssignment(randomPlanktonRole.getOidValue()));
                }

                importUserAndResolveAuxRoles(user, repository, generatorOptions, result, log);
            }

        }
    }

    //TODO fixme
    public static class OutlierMask {
        RbacObjectCategoryProcessor category;

        public OutlierMask(RbacObjectCategoryProcessor category) {
            this.category = category;
        }

        public void buildAndImport(
                @NotNull Log log,
                @NotNull RepositoryService repository,
                @NotNull GeneratorOptions generatorOptions,
                int total,
                @NotNull OperationResult result) {

            int outlierMaskProbability = generatorOptions.getOutlierMaskProbability();
            int partition = (int) ((double) total / 100) * outlierMaskProbability;

            for (int i = 0; i < partition; i++) {
                UserType user = new UserType();
                user.setName(PolyStringType.fromOrig(category.getDisplayName() + "Mask" + i));

                category.generateRbacObject(user, false, false, generatorOptions);
                user.getAssignment().clear();
                List<InitialObjectsDefinition.NoiseApplicationBusinessAbstractRole> randomPlanktonRoles = getRandomNoiseRoles(3);
                for (InitialObjectsDefinition.NoiseApplicationBusinessAbstractRole randomPlanktonRole : randomPlanktonRoles) {
                    user.getAssignment().add(createRoleAssignment(randomPlanktonRole.getOidValue()));
                }

                importUserAndResolveAuxRoles(user, repository, generatorOptions, result, log);
            }
        }
    }

}
