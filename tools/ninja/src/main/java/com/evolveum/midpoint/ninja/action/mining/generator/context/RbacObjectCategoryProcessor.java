package com.evolveum.midpoint.ninja.action.mining.generator.context;

import com.evolveum.midpoint.ninja.action.mining.generator.GeneratorOptions;
import com.evolveum.midpoint.ninja.action.mining.generator.object.InitialObjectsDefinition;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.ninja.action.mining.generator.context.ImportAction.getNameFromSet;
import static com.evolveum.midpoint.ninja.action.mining.generator.context.ImportAction.importUserAndResolveAuxRoles;
import static com.evolveum.midpoint.ninja.action.mining.generator.context.RbacGeneratorUtils.*;

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

    public void generateRbacData(@NotNull RepositoryService repository,
            @NotNull Log log,
            @NotNull GeneratorOptions generatorOptions,
            int total, Set<String> names, OperationResult result) {
        switch (this) {
            case REGULR -> new RegularUser()
                    .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
            case SEMI_REGULAR ->
                    new SemiRegularUser(generatorOptions)
                            .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
            case IRREGULAR ->
                    new IrregularUser(generatorOptions)
                            .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
            case MANAGERS ->
                    new ManagerUser(generatorOptions)
                            .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
            case SALES ->
                    new SalesUser(generatorOptions)
                            .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
            case SECURITY_OFFICERS ->
                    new SecurityOfficer()
                            .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
            case CONTRACTORS ->
                    new Contractor(generatorOptions)
                            .buildAndImportObjects(log, repository, generatorOptions, total, names, result);
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

            user.getAssignment().add(createRoleAssignment(birthEmployeeRole));
            user.getAssignment().add(createRoleAssignment(locationBusinessRoleOidValue));
            user.getAssignment().add(createRoleAssignment(randomJobBusinessRoleOidValue));

            user.getAssignment().add(createOrgAssignment(organizationOid));

            setUpArchetypeUser(user, archetypeOid);

            return user;
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

            user.getAssignment().add(createRoleAssignment(birthEmployeeRole));

            if (!limited) {
                user.setLocality(locale);
            }

            if (!randomlyJobTitleStructure.isEmpty()) {
                user.setTitle(PolyStringType.fromOrig(randomlyJobTitleStructure));
            }

            for (InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole randomPlanktonRole : randomPlanktonRoles) {
                user.getAssignment().add(createRoleAssignment(randomPlanktonRole.getOidValue()));
            }

            user.getAssignment().add(createRoleAssignment(locationBusinessRoleOidValue));
            user.getAssignment().add(createOrgAssignment(organizationOid));

            setUpArchetypeUser(user, archetypeOid);

            return user;
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
            user.getAssignment().add(createRoleAssignment(birthEmployeeRole));

            if (!randomlyJobTitleStructureWithNone.isEmpty()) {
                user.setTitle(PolyStringType.fromOrig(randomlyJobTitleStructureWithNone));
            }

            for (InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole randomPlanktonRole : randomPlanktonRoles) {
                user.getAssignment().add(createRoleAssignment(randomPlanktonRole.getOidValue()));
            }

            setUpArchetypeUser(user, archetypeOid);
            return user;
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

            user.getAssignment().add(createRoleAssignment(birthEmployeeRole));
            user.setTitle(PolyStringType.fromOrig(jobTitle));
            user.getAssignment().add(createRoleAssignment(managerRole.getOidValue()));
            user.getAssignment().add(createRoleAssignment(locationBusinessRoleOidValue));
            user.getAssignment().add(createOrgAssignment(organizationOid));

            if (!limited) {
                user.setLocality(locale);
            }

            for (InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole randomPlanktonRole : randomPlanktonRoles) {
                user.getAssignment().add(createRoleAssignment(randomPlanktonRole.getOidValue()));
            }

            setUpArchetypeUser(user, archetypeOid);

            return user;
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

            user.getAssignment().add(createRoleAssignment(birthEmployeeRole));
            user.getAssignment().add(createOrgAssignment(organizationOid));
            user.getAssignment().add(createRoleAssignment(salesBr.getOidValue()));

            if (!limited) {
                user.setTitle(PolyStringType.fromOrig(jobTitle));

                if (!partialLimited) {
                    user.setLocality(PolyStringType.fromOrig(locationNewYorkBr.getLocale()));
                    user.getAssignment().add(createRoleAssignment(locationNewYorkBr.getOidValue()));
                } else {
                    user.setLocality(PolyStringType.fromOrig(randomLocationBusinessRole.getLocale()));
                    user.getAssignment().add(createRoleAssignment(randomLocationBusinessRole.getOidValue()));
                }

            }

            setUpArchetypeUser(user, archetypeOid);

            return user;
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
                boolean limited = false;
                boolean partialLimited = false;
                if (i < ninetyPercent) {
                    limited = true;
                    if (i < seventyPercent) {
                        partialLimited = true;
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
            user.getAssignment().add(createRoleAssignment(birthEmployeeRole));
            user.getAssignment().add(createOrgAssignment(organizationOid));
            user.getAssignment().add(createRoleAssignment(securityOfficerRoleOidValue));

            setUpArchetypeUser(user, archetypeOid);

            return user;
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

            user.getAssignment().add(createRoleAssignment(birthContractorRole));
            user.getAssignment().add(createOrgAssignment(organizationOid));

            for (InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole randomPlanktonRole : randomPlanktonRoles) {
                user.getAssignment().add(createRoleAssignment(randomPlanktonRole.getOidValue()));
            }

            setUpArchetypeUser(user, archetypeOid);

            return user;
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

}
