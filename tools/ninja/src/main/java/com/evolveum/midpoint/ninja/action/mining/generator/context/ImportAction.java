/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining.generator.context;

import static com.evolveum.midpoint.ninja.action.mining.generator.context.RbacGeneratorUtils.*;
import static com.evolveum.midpoint.ninja.action.mining.generator.context.RbacObjectCategoryProcessor.generateRbacData;
import static com.evolveum.midpoint.ninja.action.mining.generator.object.InitialObjectsDefinition.getNoiseRolesObjects;

import java.io.IOException;
import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.ninja.action.mining.generator.GeneratorOptions;
import com.evolveum.midpoint.ninja.action.mining.generator.object.InitialObjectsDefinition;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
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
            remakeUsersBusinessRoles(context, result, generatorOptions, null, null);
        }
        
        log.info("NOTE: Do not forget to recompute FocusType objects");
    }

    private void initialObjectsImport(@NotNull RepositoryService repositoryService) {
        log.info("Importing initial role objects");
        InitialObjectsDefinition initialObjectsDefinition = new InitialObjectsDefinition(generatorOptions);
        importOrganizations(initialObjectsDefinition, repositoryService, result, log);

        if (isArchetypeUserEnable) {
            importArchetypes(initialObjectsDefinition, repositoryService, result, log);
        }

        if (!generatorOptions.isPlanktonDisable() || generatorOptions.getOutlierZombieProbability() != 0.0) {
            importPlanktonRoles(initialObjectsDefinition, repositoryService, result, log);
        }

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

    public static void importUserAndResolveAuxRoles(
            @NotNull UserType user,
            @NotNull RepositoryService repositoryService,
            @NotNull GeneratorOptions generatorOptions,
            @NotNull OperationResult result,
            @NotNull Log log) {

        if (generatorOptions.isAuxInclude()) {
            resolveAuxRoles(user);
        }

        try {
            //TODO uncomment for extension adding (remember to add extension to UserType schema and correct name)
//            user.extension(new ExtensionType());
//            ExtensionType ext = user.getExtension();
//            addExtensionValue(ext, "hatSize","XL","L","M","S");
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
        resolveUsers(repository, regularUsersCount, RbacObjectCategoryProcessor.Category.REGULR, names);
        resolveUsers(repository, semiRegularUsersCount, RbacObjectCategoryProcessor.Category.SEMI_REGULAR, names);
        resolveUsers(repository, irregularUsersCount, RbacObjectCategoryProcessor.Category.IRREGULAR, names);
        resolveUsers(repository, managersCount, RbacObjectCategoryProcessor.Category.MANAGERS, names);
        resolveUsers(repository, salesCount, RbacObjectCategoryProcessor.Category.SALES, names);
        resolveUsers(repository, securityOfficersCount, RbacObjectCategoryProcessor.Category.SECURITY_OFFICERS, names);
        resolveUsers(repository, contractorsCount, RbacObjectCategoryProcessor.Category.CONTRACTORS, names);
    }

    private void resolveUsers(@NotNull RepositoryService repository, int usersCount, RbacObjectCategoryProcessor.Category category, Set<String> names) {
        generateRbacData(repository, category, log, generatorOptions, usersCount, names, result);
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
     * @param names The set of extracted names from csv.
     * @return A PolyStringType representing the name.
     */
    public static PolyStringType getNameFromSet(PolyStringType initialName, Set<String> names) {
        if (names == null || names.isEmpty()) {
            return initialName;
        }

        String name = names.iterator().next();
        names.remove(name);
        return PolyStringType.fromOrig(name);
    }

    public static <V> void addExtensionValue(
            Containerable extContainer, String itemName, V... values) {
        PrismContainerValue<?> pcv = extContainer.asPrismContainerValue();
        ItemDefinition<?> itemDefinition =
                pcv.getDefinition().findItemDefinition(new ItemName(itemName));

        try {
            if (itemDefinition instanceof PrismReferenceDefinition) {
                PrismReference ref = (PrismReference) itemDefinition.instantiate();
                for (V value : values) {
                    ref.add(value instanceof PrismReferenceValue
                            ? (PrismReferenceValue) value
                            : ((Referencable) value).asReferenceValue());
                }
                pcv.add(ref);
            } else {
                //noinspection unchecked
                PrismProperty<V> property = (PrismProperty<V>) itemDefinition.instantiate();
                property.setRealValues(values);
                pcv.add(property);
            }
        } catch (SchemaException e) {
            throw new RuntimeException("Cloud not add extension value", e);
        }

    }

}
