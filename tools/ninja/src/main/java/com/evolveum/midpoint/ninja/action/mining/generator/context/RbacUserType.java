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

import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.ninja.action.mining.generator.object.InitialOrg;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.action.mining.generator.GeneratorOptions;
import com.evolveum.midpoint.ninja.action.mining.generator.object.InitialBusinessRole;
import com.evolveum.midpoint.ninja.action.mining.generator.object.InitialObjectsDefinition;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * This abstract class represents a user type generator used for initial user object RBAC generation.
 * <p>
 * Part of RBAC Testing Data, which provides testing data for role mining and other RBAC-related processes.
 */
public abstract class RbacUserType implements RbacBasicStructure {

    protected GeneratorOptions generatorOptions;

    protected RbacUserType(GeneratorOptions generatorOptions) {
        this.generatorOptions = generatorOptions;
        updateParameters();
    }

    /**
     * Updates the parameters for the RegularUserType.
     * This method is primary designated to sets the location organization and initializes the primary role.
     * It is called in the constructor of the RegularUserType for default initialization and in the build() method for
     * dynamic initialization.
     */
    protected abstract void updateParameters();

    /**
     * This method is responsible for building a UserType object with specific attributes.
     * It sets up the archetype for the user and adds assignments such as organization and roles.
     * It also sets the locality and title for the user if they are not null.
     * If there are any PlanktonApplicationRoles, it adds them to the user's assignments.
     *
     * @param user The UserType object to be built.
     * @return The built UserType object with specific attributes.
     */
    public UserType build(@NotNull UserType user) {
        updateParameters();

        setupUserArchetype(user);

        setupUserProfessionOrganization(user);

        setupUserBirthRole(user);

        setupUserLocationProperties(user);

        additionalChanges(user);

        setupUserPrimaryRoles(user);

        setupUserTitle(user);

        setupUserPlanktonRoles(user);

        return user;
    }

    private void setupUserPlanktonRoles(@NotNull UserType user) {
        List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> planktonApplicationRoles
                = getPlanktonApplicationRoles();
        if (planktonApplicationRoles != null && !planktonApplicationRoles.isEmpty()) {
            for (InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole planktonApplicationRole
                    : planktonApplicationRoles) {
                user.getAssignment().add(createRoleAssignment(planktonApplicationRole.getOidValue()));
            }
        }
    }

    private void setupUserTitle(@NotNull UserType user) {
        String title = getTitle();
        if (title != null) {
            user.setTitle(PolyStringType.fromOrig(title));
        }
    }

    private void setupUserPrimaryRoles(@NotNull UserType user) {
        InitialBusinessRole primaryRole = getPrimaryRole();
        user.getAssignment().add(createRoleAssignment(primaryRole.getOidValue()));
    }

    private void setupUserBirthRole(@NotNull UserType user) {
        String birthRole = getBirthRole();
        if (birthRole != null) {
            user.getAssignment().add(createRoleAssignment(getBirthRole()));
        }
    }

    private void setupUserProfessionOrganization(@NotNull UserType user) {
        String organizationOid = getProfessionOrganizationOid();
        if (organizationOid != null) {
            user.getAssignment().add(createOrgAssignment(organizationOid));
        }
    }

    private void setupUserArchetype(@NotNull UserType user) {
        String correspondingArchetypeOid = getCorrespondingArchetypeOid();
        if (correspondingArchetypeOid != null) {
            setUpArchetypeUser(user, correspondingArchetypeOid);
        }
    }

    /**
     * Sets up the location properties for the given user.
     * This method assigns the location role, setup user locality attribute, and add location organization assignment to the user.
     *
     * @param user The UserType object to which the location properties will be assigned.
     */
    private void setupUserLocationProperties(@NotNull UserType user) {
        InitialObjectsDefinition.LocationInitialBusinessRole businessRole = getLocationRole();
        if (businessRole == null) {
            return;
        }

        user.getAssignment().add(createRoleAssignment(businessRole.getOidValue()));

        String locality = getLocality();
        if (locality == null) {
            return;
        }

        user.setLocality(PolyStringType.fromOrig(locality));

        InitialOrg locationOrg = getLocalityOrg();
        if (locationOrg != null) {
            user.getAssignment().add(createOrgAssignment(locationOrg.getOidValue()));
        }
    }

    /**
     * This method is responsible for building and importing objects for a specific user type.
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
    @Override
    public void buildAndImportObjects(
            @NotNull Log log,
            @NotNull RepositoryService repository,
            @NotNull GeneratorOptions generatorOptions,
            int total, Set<String> names,
            @NotNull OperationResult result) {
        String displayName = getDisplayName();
        log.info("Importing " + displayName + ": 0/{}", total);
        for (int i = 0; i < total; i++) {
            log.info("Importing " + displayName + ": {}/{}", i + 1, total);
            UserType user = new UserType();
            user.setName(getNameFromSet(PolyStringType.fromOrig(displayName + " User " + i), names));
            user = build(user);
            importUserAndResolveAuxRoles(user, repository, generatorOptions, result, log);
        }
    }

    protected abstract String getDisplayName();

}
