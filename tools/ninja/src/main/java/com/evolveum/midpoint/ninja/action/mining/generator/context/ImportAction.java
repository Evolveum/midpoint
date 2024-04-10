/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining.generator.context;

import static com.evolveum.midpoint.ninja.action.mining.generator.context.RbacGeneratorUtils.*;
import static com.evolveum.midpoint.ninja.action.mining.generator.object.InitialObjectsDefinition.getNoiseRolesObjects;

import java.io.IOException;
import java.util.*;

import com.evolveum.midpoint.schema.ResultHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.ninja.action.mining.generator.GeneratorOptions;
import com.evolveum.midpoint.ninja.action.mining.generator.object.InitialObjectsDefinition;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Class responsible for importing initial objects into the Midpoint system.
 * <p>
 * Part of RBAC Testing Data, which provides testing data for role mining and other RBAC-related processes.
 */
public class ImportAction {

    NinjaContext context;
    GeneratorOptions generatorOptions;
    OperationResult result;
    Log log;
    Set<String> names = null;
    boolean isArchetypeUserEnable;

    public ImportAction(
            @NotNull NinjaContext context,
            @NotNull GeneratorOptions generatorOptions,
            @NotNull OperationResult result) {
        this.context = context;
        this.generatorOptions = generatorOptions;
        this.isArchetypeUserEnable = generatorOptions.isArchetypeUserEnable();
        this.result = result;
        this.log = context.getLog();
    }

    public void executeImport() {
        RepositoryService repositoryService = context.getRepository();
        if (generatorOptions.isImport()) {
            initialObjectsImport(repositoryService);
            importUsers(generatorOptions.getUsersCount(), generatorOptions.getCsvPath());
        }

        if (generatorOptions.isTransform()) {
            log.info("Make sure that RoleType objects is recomputed");
            remakeUsersBusinessRoles(context, result, generatorOptions, null, null);
        }
    }

    private void initialObjectsImport(@NotNull RepositoryService repositoryService) {
        log.info("Importing initial role objects");
        InitialObjectsDefinition initialObjectsDefinition = new InitialObjectsDefinition(generatorOptions);
        importOrganizations(initialObjectsDefinition, repositoryService, result, log);

        if (isArchetypeUserEnable) {
            importArchetypes(initialObjectsDefinition, repositoryService, result, log);
        }

        importPlanktonRoles(initialObjectsDefinition, repositoryService, result, log);
        importMultipliedBasicRoles(initialObjectsDefinition, repositoryService, result, log);
        importBusinessRoles(initialObjectsDefinition, repositoryService, result, log);
        importNoiseRoles(repositoryService, result, log);
        log.info("Initial role objects imported");
    }

    private void importMultipliedBasicRoles(
            @NotNull InitialObjectsDefinition initialObjectsDefinition,
            @NotNull RepositoryService repositoryService,
            OperationResult result, @NotNull Log log) {

        InitialObjectsDefinition.BasicAbstractRole[] basicMultiplierRoles = initialObjectsDefinition.getBasicRolesObjects();
        log.info("Importing basic roles: 0/{}", basicMultiplierRoles.length);
        for (int i = 0; i < basicMultiplierRoles.length; i++) {
            log.info("Importing basic roles: {}/{}", i + 1, basicMultiplierRoles.length);
            InitialObjectsDefinition.BasicAbstractRole rolesObject = basicMultiplierRoles[i];
            List<RoleType> role = rolesObject.generateRoleObject();
            for (RoleType roleType : role) {
                importRole(roleType, repositoryService, result, log);
            }
        }
    }

    private void importBusinessRoles(
            @NotNull InitialObjectsDefinition initialObjectsDefinition,
            @NotNull RepositoryService repositoryService,
            @NotNull OperationResult result,
            @NotNull Log log) {

        List<RoleType> rolesObjects = initialObjectsDefinition.getBusinessRolesObjects();
        log.info("Importing business roles: 0/{}", rolesObjects.size());
        for (int i = 0; i < rolesObjects.size(); i++) {
            log.info("Importing business roles: {}/{}", i + 1, rolesObjects.size());
            RoleType role = rolesObjects.get(i);
            try {
                repositoryService.addObject(role.asPrismObject(), null, result);
            } catch (ObjectAlreadyExistsException e) {
                log.warn("Role {} already exists", role.getName());
            } catch (SchemaException e) {
                log.error("Error adding role {}", role.getName(), e);
                throw new RuntimeException(e);
            }
        }
    }

    private void importPlanktonRoles(
            @NotNull InitialObjectsDefinition initialObjectsDefinition,
            @NotNull RepositoryService repositoryService,
            @NotNull OperationResult result,
            @NotNull Log log) {

        List<RoleType> rolesObjects = initialObjectsDefinition.getPlanktonRolesObjects();
        log.info("Importing plankton roles: 0/{}", rolesObjects.size());
        for (int i = 0; i < rolesObjects.size(); i++) {
            log.info("Importing plankton roles: {}/{}", i + 1, rolesObjects.size());
            RoleType role = rolesObjects.get(i);
            try {
                repositoryService.addObject(role.asPrismObject(), null, result);
            } catch (ObjectAlreadyExistsException e) {
                log.warn("Role {} already exists", role.getName());
            } catch (SchemaException e) {
                log.error("Error adding role {}", role.getName(), e);
                throw new RuntimeException(e);
            }
        }
    }

    private void importNoiseRoles(
            @NotNull RepositoryService repositoryService,
            @NotNull OperationResult result,
            @NotNull Log log) {
        List<RoleType> rolesObjects = getNoiseRolesObjects();
        log.info("Importing noise roles: 0/{}", rolesObjects.size());
        for (int i = 0; i < rolesObjects.size(); i++) {
            log.info("Importing noise roles: {}/{}", i + 1, rolesObjects.size());
            RoleType role = rolesObjects.get(i);
            try {
                repositoryService.addObject(role.asPrismObject(), null, result);
            } catch (ObjectAlreadyExistsException e) {
                log.warn("Role {} already exists", role.getName());
            } catch (SchemaException e) {
                log.error("Error adding role {}", role.getName(), e);
                throw new RuntimeException(e);
            }
        }
    }

    private void importRole(
            @NotNull RoleType role,
            @NotNull RepositoryService repositoryService,
            @NotNull OperationResult result,
            @NotNull Log log) {

        try {
            repositoryService.addObject(role.asPrismObject(), null, result);
        } catch (ObjectAlreadyExistsException e) {
            log.warn("Role {} already exists", role.getName());
        } catch (SchemaException e) {
            log.error("Error adding role {}", role.getName(), e);
            throw new RuntimeException(e);
        }
    }

    private void importOrganizations(
            @NotNull InitialObjectsDefinition initialObjectsDefinition,
            @NotNull RepositoryService repositoryService,
            @NotNull OperationResult result,
            @NotNull Log log) {
        List<OrgType> orgObjects = initialObjectsDefinition.getOrgObjects();
        log.info("Importing organizations: 0/{}", orgObjects.size());

        for (int i = 0; i < orgObjects.size(); i++) {
            log.info("Importing organizations: {}/{}", i + 1, orgObjects.size());
            OrgType org = orgObjects.get(i);
            try {
                repositoryService.addObject(org.asPrismObject(), null, result);
            } catch (ObjectAlreadyExistsException e) {
                log.warn("Org {} already exists", org.getName());
            } catch (SchemaException e) {
                log.error("Error adding org {}", org.getName(), e);
                throw new RuntimeException(e);
            }
        }
    }

    private void importArchetypes(
            @NotNull InitialObjectsDefinition initialObjectsDefinition,
            @NotNull RepositoryService repositoryService,
            @NotNull OperationResult result,
            @NotNull Log log) {
        List<ArchetypeType> archetypeObjects = initialObjectsDefinition.getArchetypeObjects();
        log.info("Importing archetypes: 0/{}", archetypeObjects.size());

        for (int i = 0; i < archetypeObjects.size(); i++) {
            log.info("Importing archetypes: {}/{}", i + 1, archetypeObjects.size());
            ArchetypeType archetype = archetypeObjects.get(i);
            try {
                repositoryService.addObject(archetype.asPrismObject(), null, result);
            } catch (ObjectAlreadyExistsException e) {
                log.warn("Archetype {} already exists", archetype.getName());
            } catch (SchemaException e) {
                log.error("Error adding org {}", archetype.getName(), e);
                throw new RuntimeException(e);
            }
        }
    }

    private void importUserAndResolveAuxRoles(
            @NotNull UserType user,
            @NotNull RepositoryService repositoryService,
            @NotNull OperationResult result,
            @NotNull Log log) {

        if (generatorOptions.isAuxInclude()) {
            resolveAuxRoles(user);
        }

        try {
            repositoryService.addObject(user.asPrismObject(), null, result);
        } catch (ObjectAlreadyExistsException e) {
            log.warn("User {} already exists", user.getName());
        } catch (SchemaException e) {
            log.error("Error adding user {}", user.getName(), e);
            throw new RuntimeException(e);
        }
    }

    private void importUsers(int usersCount, @Nullable String csvPath) {
        log.info("Importing users");

        if (csvPath != null) {
            this.names = new HashSet<>();
            try {
                names = loadNamesFromCSV(csvPath);
            } catch (IOException e) {
                log.error("Error loading names from CSV file", e);
                throw new RuntimeException(e);
            }
        }

        generateAndImportUsers(usersCount);
        log.info("Users imported");
    }

    public void generateAndImportUsers(int userCount) {
        String division = generatorOptions.getDivision();
        String[] parts = division.split(":");
        int sum = 0;
        int[] partsInt = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            int value = Integer.parseInt(part);
            sum += value;
            partsInt[i] = value;
        }

        if (sum != 100) {
            log.error("Division is not valid. Sum of parts is not 100 but " + sum);
            throw new IllegalArgumentException("Division is not valid. Sum of parts is not 100 but " + sum);
        } else if (parts.length != 7) {
            log.error("Division is not valid. It should contain 7 parts but it contains " + parts.length);
            throw new IllegalArgumentException("Division is not valid. It should contain 7 parts but it contains " + parts.length);
        }

        int regularUsersCount = (int) (userCount * (partsInt[0] / 100.0));
        int semiRegularUsersCount = (int) (userCount * (partsInt[1] / 100.0));
        int irregularUsersCount = (int) (userCount * (partsInt[2] / 100.0));
        int managersCount = (int) (userCount * (partsInt[3] / 100.0));
        int salesCount = (int) (userCount * (partsInt[4] / 100.0));
        int securityOfficersCount = (int) (userCount * (partsInt[5] / 100.0));
        int contractorsCount = (int) (userCount * (partsInt[6] / 100.0));

        RepositoryService repository = context.getRepository();
        resolveRegularUsers(regularUsersCount, repository);
        resolveSemiRegularUsers(semiRegularUsersCount, repository);
        resolveIrregularUsers(irregularUsersCount, repository);
        resolveManagers(managersCount, repository);
        resolveSales(salesCount, repository);
        resolveSecurityOfficers(securityOfficersCount, repository);
        resolveContractors(contractorsCount, repository);
    }

    /**
     * Resolves and imports regular users into the system, setting up their attributes based on specified criteria.
     *
     * @param regularUsersCount The number of regular users to import.
     * @param repository The repository service used for importing users.
     */
    private void resolveRegularUsers(int regularUsersCount, RepositoryService repository) {
        log.info("Importing regular users: 0/{}", regularUsersCount);
        String organizationOid = InitialObjectsDefinition.Organization.REGULAR.getOidValue();
        String birthEmployeeRole = InitialObjectsDefinition.BirthrightBusinessRole.EMPLOYEE.getOidValue();
        String archetypeOid = InitialObjectsDefinition.Archetypes.REGULAR_USER.getOidValue();

        for (int i = 0; i < regularUsersCount; i++) {
            log.info("Importing regular users: {}/{}", i + 1, regularUsersCount);
            InitialObjectsDefinition.LocationInitialBusinessRole randomLocationBusinessRole = getRandomLocationBusinessRole();
            String locationBusinessRoleOidValue = randomLocationBusinessRole.getOidValue();
            PolyStringType locale = PolyStringType.fromOrig(randomLocationBusinessRole.getLocale());
            InitialObjectsDefinition.JobInitialBusinessRole randomJobBusinessRole = getRandomJobBusinessRole();
            String randomJobBusinessRoleOidValue = randomJobBusinessRole.getOidValue();
            String randomlyJobTitleStructure = getRandomlyJobTitles();

            UserType user = new UserType();
            user.setName(getNameFromSet(PolyStringType.fromOrig("Regular User " + i)));
            user.setLocality(locale);
            user.setTitle(PolyStringType.fromOrig(randomlyJobTitleStructure));
            user.getAssignment().add(createRoleAssignment(birthEmployeeRole));
            user.getAssignment().add(createRoleAssignment(locationBusinessRoleOidValue));
            user.getAssignment().add(createRoleAssignment(randomJobBusinessRoleOidValue));
            user.getAssignment().add(createOrgAssignment(organizationOid));

            setUpArchetypeUser(user, archetypeOid);

            importUserAndResolveAuxRoles(user, repository, result, log);
        }
    }

    /**
     * Resolves and imports semi-regular users into the system, setting up their attributes based on specified criteria.
     *
     * @param semiRegularUsersCount The number of semi-regular users to import.
     * @param repository The repository service used for importing users.
     */
    private void resolveSemiRegularUsers(int semiRegularUsersCount, RepositoryService repository) {
        log.info("Importing semi-regular users: 0/{}", semiRegularUsersCount);
        String organizationOid = InitialObjectsDefinition.Organization.SEMI_REGULAR.getOidValue();
        String birthEmployeeRole = InitialObjectsDefinition.BirthrightBusinessRole.EMPLOYEE.getOidValue();
        String archetypeOid = InitialObjectsDefinition.Archetypes.SEMI_REGULAR_USER.getOidValue();
        int ninetyPercent = (int) (semiRegularUsersCount * 0.9);

        for (int i = 0; i < semiRegularUsersCount; i++) {
            log.info("Importing semi-regular users: {}/{}", i + 1, semiRegularUsersCount);
            InitialObjectsDefinition.LocationInitialBusinessRole randomLocationBusinessRole = getRandomLocationBusinessRole();
            String locationBusinessRoleOidValue = randomLocationBusinessRole.getOidValue();
            PolyStringType locale = PolyStringType.fromOrig(randomLocationBusinessRole.getLocale());
            String randomlyJobTitleStructure = getRandomlyJobTitlesWithNone();
            List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> randomPlanktonRoles = getRandomPlanktonRoles(0);

            UserType user = new UserType();
            user.setName(getNameFromSet(PolyStringType.fromOrig("Semi-Regular User " + i)));
            user.getAssignment().add(createRoleAssignment(birthEmployeeRole));

            if (i < ninetyPercent) {
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

            importUserAndResolveAuxRoles(user, repository, result, log);
        }
    }

    /**
     * Resolves and imports irregular users into the system, setting up their attributes based on specified criteria.
     *
     * @param irregularUsersCount The number of irregular users to import.
     * @param repository The repository service used for importing users.
     */
    private void resolveIrregularUsers(int irregularUsersCount, RepositoryService repository) {
        log.info("Importing irregular users: 0/{}", irregularUsersCount);
        String organizationOid = InitialObjectsDefinition.Organization.IRREGULAR.getOidValue();
        String birthEmployeeRole = InitialObjectsDefinition.BirthrightBusinessRole.EMPLOYEE.getOidValue();
        String archetypeOid = InitialObjectsDefinition.Archetypes.IRREGULAR_USER.getOidValue();

        for (int i = 0; i < irregularUsersCount; i++) {
            log.info("Importing irregular users: {}/{}", i + 1, irregularUsersCount);
            String randomlyJobTitleStructureWithNone = getRandomlyJobTitlesWithNone();
            List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> randomPlanktonRoles = getRandomPlanktonRoles(7);
            UserType user = new UserType();
            user.setName(getNameFromSet(PolyStringType.fromOrig("Irregular User " + i)));
            user.getAssignment().add(createOrgAssignment(organizationOid));
            user.getAssignment().add(createRoleAssignment(birthEmployeeRole));

            if (!randomlyJobTitleStructureWithNone.isEmpty()) {
                user.setTitle(PolyStringType.fromOrig(randomlyJobTitleStructureWithNone));
            }

            for (InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole randomPlanktonRole : randomPlanktonRoles) {
                user.getAssignment().add(createRoleAssignment(randomPlanktonRole.getOidValue()));
            }

            setUpArchetypeUser(user, archetypeOid);

            importUserAndResolveAuxRoles(user, repository, result, log);

        }
    }

    /**
     * Resolves and imports managers into the system, setting up their attributes based on specified criteria.
     *
     * @param managersCount The number of manager users to import.
     * @param repository The repository service used for importing users.
     */
    private void resolveManagers(int managersCount, RepositoryService repository) {
        log.info("Importing manager users: 0/{}", managersCount);
        String organizationOid = InitialObjectsDefinition.Organization.MANAGERS.getOidValue();
        String birthEmployeeRole = InitialObjectsDefinition.BirthrightBusinessRole.EMPLOYEE.getOidValue();
        String archetypeOid = InitialObjectsDefinition.Archetypes.MANAGERS_USER.getOidValue();
        int ninetyPercent = (int) (managersCount * 0.9);

        InitialObjectsDefinition.JobInitialBusinessRole managerRole = InitialObjectsDefinition.JobInitialBusinessRole.MANAGER;
        String jobTitle = "manager";

        for (int i = 0; i < managersCount; i++) {
            log.info("Importing manager users: {}/{}", i + 1, managersCount);
            InitialObjectsDefinition.LocationInitialBusinessRole randomLocationBusinessRole = getRandomLocationBusinessRole();
            String locationBusinessRoleOidValue = randomLocationBusinessRole.getOidValue();
            PolyStringType locale = PolyStringType.fromOrig(randomLocationBusinessRole.getLocale());
            List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> randomPlanktonRoles = getRandomPlanktonRoles(0);

            UserType user = new UserType();
            user.setName(getNameFromSet(PolyStringType.fromOrig("Manager User " + i)));
            user.getAssignment().add(createRoleAssignment(birthEmployeeRole));

            user.setTitle(PolyStringType.fromOrig(jobTitle));
            user.getAssignment().add(createRoleAssignment(managerRole.getOidValue()));
            user.getAssignment().add(createRoleAssignment(locationBusinessRoleOidValue));
            user.getAssignment().add(createOrgAssignment(organizationOid));

            if (i < ninetyPercent) {
                user.setLocality(locale);
            }

            for (InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole randomPlanktonRole : randomPlanktonRoles) {
                user.getAssignment().add(createRoleAssignment(randomPlanktonRole.getOidValue()));
            }

            setUpArchetypeUser(user, archetypeOid);

            importUserAndResolveAuxRoles(user, repository, result, log);

        }

    }

    /**
     * Resolves and imports sales users into the system, setting up their attributes based on specified criteria.
     *
     * @param salesCount The number of sales users to import.
     * @param repository The repository service used for importing users.
     */
    private void resolveSales(int salesCount, RepositoryService repository) {
        log.info("Importing sales users: 0/{}", salesCount);
        String organizationOid = InitialObjectsDefinition.Organization.SALES.getOidValue();
        String birthEmployeeRole = InitialObjectsDefinition.BirthrightBusinessRole.EMPLOYEE.getOidValue();
        String archetypeOid = InitialObjectsDefinition.Archetypes.SALES_USER.getOidValue();

        int ninetyPercent = (int) (salesCount * 0.9);
        int seventyPercent = (int) (salesCount * 0.7);

        InitialObjectsDefinition.JobInitialBusinessRole salesBr = InitialObjectsDefinition.JobInitialBusinessRole.SALES;
        InitialObjectsDefinition.LocationInitialBusinessRole locationNewYorkBr = InitialObjectsDefinition.LocationInitialBusinessRole.LOCATION_NEW_YORK;
        String jobTitle = "salesperson";

        for (int i = 0; i < salesCount; i++) {
            log.info("Importing sales users: {}/{}", i + 1, salesCount);
            InitialObjectsDefinition.LocationInitialBusinessRole randomLocationBusinessRole = getRandomLocationBusinessRole();

            UserType user = new UserType();
            user.setName(getNameFromSet(PolyStringType.fromOrig("Sales User " + i)));
            user.getAssignment().add(createRoleAssignment(birthEmployeeRole));
            user.getAssignment().add(createOrgAssignment(organizationOid));
            user.getAssignment().add(createRoleAssignment(salesBr.getOidValue()));

            if (i < ninetyPercent) {
                user.setTitle(PolyStringType.fromOrig(jobTitle));

                if (i < seventyPercent) {
                    user.setLocality(PolyStringType.fromOrig(locationNewYorkBr.getLocale()));
                    user.getAssignment().add(createRoleAssignment(locationNewYorkBr.getOidValue()));
                } else {
                    user.setLocality(PolyStringType.fromOrig(randomLocationBusinessRole.getLocale()));
                    user.getAssignment().add(createRoleAssignment(randomLocationBusinessRole.getOidValue()));
                }

            }

            setUpArchetypeUser(user, archetypeOid);

            importUserAndResolveAuxRoles(user, repository, result, log);

        }
    }

    /**
     * Resolves and imports security officer users into the system, setting up their attributes based on specified criteria.
     *
     * @param securityOfficersCount The number of security officer users to import.
     * @param repository The repository service used for importing users.
     */
    private void resolveSecurityOfficers(int securityOfficersCount, RepositoryService repository) {
        log.info("Importing security officer users: 0/{}", securityOfficersCount);
        String organizationOid = InitialObjectsDefinition.Organization.SECURITY_OFFICERS.getOidValue();
        String birthEmployeeRole = InitialObjectsDefinition.BirthrightBusinessRole.EMPLOYEE.getOidValue();
        InitialObjectsDefinition.JobInitialBusinessRole securityOfficerRole = InitialObjectsDefinition.JobInitialBusinessRole.SECURITY_OFFICER;
        String securityOfficerRoleOidValue = securityOfficerRole.getOidValue();
        String archetypeOid = InitialObjectsDefinition.Archetypes.SECURITY_OFFICERS_USER.getOidValue();

        for (int i = 0; i < securityOfficersCount; i++) {
            log.info("Importing security officer users: {}/{}", i + 1, securityOfficersCount);
            UserType user = new UserType();
            user.setName(getNameFromSet(PolyStringType.fromOrig("Security Officer User " + i)));
            user.getAssignment().add(createRoleAssignment(birthEmployeeRole));
            user.getAssignment().add(createOrgAssignment(organizationOid));
            user.getAssignment().add(createRoleAssignment(securityOfficerRoleOidValue));

            setUpArchetypeUser(user, archetypeOid);

            importUserAndResolveAuxRoles(user, repository, result, log);
        }
    }

    /**
     * Resolves and imports contractor users into the system, setting up their attributes based on specified criteria.
     *
     * @param contractorsCount The number of contractor users to import.
     * @param repository The repository service used for importing users.
     */
    private void resolveContractors(int contractorsCount, RepositoryService repository) {
        log.info("Importing contractor users: 0/{}", contractorsCount);
        String organizationOid = InitialObjectsDefinition.Organization.CONTRACTORS.getOidValue();
        String birthContractorRole = InitialObjectsDefinition.BirthrightBusinessRole.CONTRACTOR.getOidValue();
        String archetypeOid = InitialObjectsDefinition.Archetypes.CONTRACTORS_USER.getOidValue();

        for (int i = 0; i < contractorsCount; i++) {
            log.info("Importing contractor users: {}/{}", i + 1, contractorsCount);
            List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> randomPlanktonRoles = getRandomPlanktonRoles(0);

            UserType user = new UserType();
            user.setName(getNameFromSet(PolyStringType.fromOrig("Contractor User " + i)));
            user.getAssignment().add(createRoleAssignment(birthContractorRole));
            user.getAssignment().add(createOrgAssignment(organizationOid));

            for (InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole randomPlanktonRole : randomPlanktonRoles) {
                user.getAssignment().add(createRoleAssignment(randomPlanktonRole.getOidValue()));
            }

            setUpArchetypeUser(user, archetypeOid);

            importUserAndResolveAuxRoles(user, repository, result, log);
        }
    }

    /**
     * Remakes business roles for their inducements on users.
     * <p>
     * This method replaces business roles with their inducements.
     *
     * @param context The Ninja context.
     * @param result The operation result used for tracking the operation.
     * @param query The query for searching users.
     * @param options The options for retrieving users.
     * @throws RuntimeException If an error occurs during the process.
     */
    public static void remakeUsersBusinessRoles(@NotNull NinjaContext context,
            @NotNull OperationResult result,
            @NotNull GeneratorOptions generatorOptions,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options) {

        RepositoryService repository = context.getRepository();
        Log log = context.getLog();
        log.info("Replace business role for their inducements on users started");

        ResultHandler<UserType> handler = (object, parentResult) -> {
            executeChangesOnUser(result, object, generatorOptions, repository, log);
            return true;
        };

        try {
            repository.searchObjectsIterative(UserType.class, query, handler, options, false, result);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        log.info("Replace business role for their inducements on users finished");
    }

    /**
     * Executes changes on a user object.
     * <p>
     * This method replaces business roles with their inducements on a user object.
     *
     * @param result The operation result used for tracking the operation.
     * @param object The user object to execute changes on.
     * @param repository The repository service used for executing changes.
     * @param log The log used for logging the operation.
     * @throws RuntimeException If an error occurs during the process.
     */
    private static void executeChangesOnUser(
            @NotNull OperationResult result,
            @NotNull PrismObject<UserType> object,
            @NotNull GeneratorOptions generatorOptions,
            @NotNull RepositoryService repository,
            @NotNull Log log) {
        String userOid = object.getOid();
        PolyString name = object.getName();
        if (name == null) {
            return;
        }

        String stringName = name.toString();

        if (stringName.equals("administrator")) {
            return;
        }

        UserType userObject = object.asObjectable();

        List<PrismObject<RoleType>> rolesOidAssignment;
        try {
            rolesOidAssignment = getBusinessRolesOidAssignment(userObject, repository, result);
        } catch (SchemaException | ObjectNotFoundException e) {
            log.error("Error while getting roles oid assignment for user: {}", userOid, e);
            throw new RuntimeException(e);
        }

        for (PrismObject<RoleType> roleTypePrismObject : rolesOidAssignment) {
            RoleType role = roleTypePrismObject.asObjectable();
            List<AssignmentType> inducement = role.getInducement();

            List<ItemDelta<?, ?>> modifications = new ArrayList<>();
            try {

                RoleType noiseRole = getAdditionNoiseRole(generatorOptions.getAdditionNoise());
                if (noiseRole != null) {
                    modifications.add(PrismContext.get().deltaFor(UserType.class)
                            .item(UserType.F_ASSIGNMENT).add(createRoleAssignment(noiseRole.getOid()))
                            .asItemDelta());
                }

                for (AssignmentType assignmentType : inducement) {
                    boolean allowed = isForgetRole(generatorOptions.getForgetNoise());
                    if (allowed) {
                        continue;
                    }

                    modifications.add(PrismContext.get().deltaFor(UserType.class)
                            .item(UserType.F_ASSIGNMENT).add(createRoleAssignment(assignmentType.getTargetRef().getOid()))
                            .asItemDelta());
                }

                modifications.add(PrismContext.get().deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).delete(createRoleAssignment(role.getOid()))
                        .asItemDelta());

                repository.modifyObject(UserType.class, userOid, modifications, result);

            } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
                throw new RuntimeException(e);
            }

        }

        log.info("User {} prepared", name);
    }

    /**
     * Gets a name from the set of extracted names from csv and removes it from the set due to objects name collision.
     *
     * @param initialName The initial name to use if the set is empty.
     * @return A PolyStringType representing the name.
     */
    public PolyStringType getNameFromSet(PolyStringType initialName) {
        if (names == null || names.isEmpty()) {
            return initialName;
        }

        String name = names.iterator().next();
        names.remove(name);
        return PolyStringType.fromOrig(name);
    }

}
