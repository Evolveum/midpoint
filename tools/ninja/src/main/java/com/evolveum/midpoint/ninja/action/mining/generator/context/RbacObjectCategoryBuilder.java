/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining.generator.context;

import static com.evolveum.midpoint.ninja.action.mining.generator.context.ImportAction.addExtensionValue;
import static com.evolveum.midpoint.ninja.action.mining.generator.context.ImportAction.importUserAndResolveAuxRoles;
import static com.evolveum.midpoint.ninja.action.mining.generator.context.RbacGeneratorUtils.*;
import static com.evolveum.midpoint.ninja.action.mining.generator.context.RbacObjectCategoryProcessor.*;
import static com.evolveum.midpoint.ninja.action.mining.generator.context.RbacObjectCategoryProcessor.Category.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.action.mining.generator.GeneratorOptions;
import com.evolveum.midpoint.ninja.action.mining.generator.object.InitialBusinessRole;
import com.evolveum.midpoint.ninja.action.mining.generator.object.InitialObjectsDefinition;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RbacObjectCategoryBuilder {

    /**
     * This class represents a Regular User in the system.
     * It contains methods to build a UserType object with attributes specific to a Regular User.
     */
    public static class RegularUserType extends RbacUserType {

        InitialBusinessRole primaryRole;
        InitialObjectsDefinition.LocationInitialBusinessRole locationBusinessRole;

        /**
         * Default constructor for the RegularUser class.
         */
        public RegularUserType(GeneratorOptions generatorOptions) {
            super(generatorOptions);
        }

        @Override
        public String getBirthRole() {
            return InitialObjectsDefinition.BirthrightBusinessRole.EMPLOYEE.getOidValue();
        }

        @Override
        public String getOrganizationOid() {
            return InitialObjectsDefinition.Organization.REGULAR.getOidValue();
        }

        @Override
        public String getCorrespondingArchetypeOid() {
            return InitialObjectsDefinition.Archetypes.REGULAR_USER.getOidValue();
        }

        @Override
        public InitialBusinessRole getPrimaryRole(boolean generateNew) {
            if (generateNew) {
                List<InitialObjectsDefinition.JobInitialBusinessRole> primaryRoles = new ArrayList<>();

                InitialObjectsDefinition.JobInitialBusinessRole assistant = InitialObjectsDefinition.JobInitialBusinessRole.ASSISTANT;
                InitialObjectsDefinition.JobInitialBusinessRole supervisor = InitialObjectsDefinition.JobInitialBusinessRole.SUPERVISOR;
                InitialObjectsDefinition.JobInitialBusinessRole hrClerkAssistant = InitialObjectsDefinition.JobInitialBusinessRole.HR_CLERK;
                primaryRoles.add(assistant);
                primaryRoles.add(supervisor);
                primaryRoles.add(hrClerkAssistant);

                Random random = new Random();
                int index = random.nextInt(primaryRoles.size());
                primaryRole = primaryRoles.get(index);
            }

            return primaryRole;
        }

        @Override
        public InitialObjectsDefinition.@NotNull LocationInitialBusinessRole getLocationRole(boolean generateNew) {
            if (generateNew) {
                locationBusinessRole = getRandomLocationBusinessRole();
            }
            return locationBusinessRole;
        }

        @Override
        public List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> getPlanktonApplicationRoles() {
            return null;
        }

        @Override
        public String getLocality() {
            if (locationBusinessRole != null) {
                return locationBusinessRole.getLocale();
            }
            return null;
        }

        @Override
        public String getTitle() {
            if (primaryRole != null) {
                return primaryRole.getName();
            }
            return null;
        }

        @Override
        public void additionalChanges(UserType user) {
            additionalChangesOnAllUsers(user);
        }

        @Override
        protected String getDisplayName() {
            return REGULR.getDisplayName();
        }
    }

    /**
     * This class represents a Semi Regular User in the system.
     * It contains methods to build a UserType object with attributes specific to a Semi Regular User.
     */
    public static class SemiRegularUserType extends RbacUserType {

        InitialBusinessRole primaryRole;
        InitialObjectsDefinition.LocationInitialBusinessRole locationBusinessRole;

        public SemiRegularUserType(GeneratorOptions generatorOptions) {
            super(generatorOptions);
        }

        @Override
        public String getBirthRole() {
            return InitialObjectsDefinition.BirthrightBusinessRole.EMPLOYEE.getOidValue();
        }

        @Override
        public String getOrganizationOid() {
            return InitialObjectsDefinition.Organization.SEMI_REGULAR.getOidValue();
        }

        @Override
        public String getCorrespondingArchetypeOid() {
            return InitialObjectsDefinition.Archetypes.SEMI_REGULAR_USER.getOidValue();
        }

        @Override
        public InitialBusinessRole getPrimaryRole(boolean generateNew) {
            primaryRole = InitialObjectsDefinition.JobInitialBusinessRole.HQ_CLERK;
            return InitialObjectsDefinition.JobInitialBusinessRole.HQ_CLERK;
        }

        @Override
        public InitialObjectsDefinition.@NotNull LocationInitialBusinessRole getLocationRole(boolean generateNew) {
            if (generateNew) {
                locationBusinessRole = getRandomLocationBusinessRole();
            }
            return locationBusinessRole;
        }

        @Override
        public List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> getPlanktonApplicationRoles() {
            return getRandomPlanktonRoles(
                    0, 4, generatorOptions);
        }

        @Override
        public String getLocality() {
            if (locationBusinessRole != null) {
                boolean candidate = isCandidate(90);
                if (candidate) {
                    return locationBusinessRole.getLocale();
                }
            }
            return null;
        }

        @Override
        public String getTitle() {
            return getRandomlyJobTitlesWithNone();
        }

        @Override
        public void additionalChanges(UserType user) {
            additionalChangesOnAllUsers(user);
        }

        @Override
        protected String getDisplayName() {
            return SEMI_REGULAR.getDisplayName();
        }
    }

    /**
     * This class represents an Irregular User in the system.
     * It contains methods to build a UserType object with attributes specific to an Irregular User.
     */
    public static class IrregularUserType extends RbacUserType {
        InitialBusinessRole primaryRole;

        public IrregularUserType(GeneratorOptions generatorOptions) {
            super(generatorOptions);
        }

        @Override
        public String getOrganizationOid() {
            return InitialObjectsDefinition.Organization.IRREGULAR.getOidValue();
        }

        @Override
        public List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> getPlanktonApplicationRoles() {
            return getRandomPlanktonRoles(5, generatorOptions);
        }

        @Override
        public String getLocality() {
            return null;
        }

        @Override
        public String getTitle() {
            return getRandomlyJobTitlesWithNone();
        }

        @Override
        public void additionalChanges(UserType user) {
            additionalChangesOnAllUsers(user);
        }

        @Override
        public String getCorrespondingArchetypeOid() {
            return InitialObjectsDefinition.Archetypes.IRREGULAR_USER.getOidValue();
        }

        @Override
        public InitialBusinessRole getPrimaryRole(boolean generateNew) {
            primaryRole = InitialObjectsDefinition.JobInitialBusinessRole.IRREGULAR;
            return primaryRole;
        }

        @Override
        public InitialObjectsDefinition.LocationInitialBusinessRole getLocationRole(boolean generateNew) {
            return null;
        }

        @Override
        public String getBirthRole() {
            return InitialObjectsDefinition.BirthrightBusinessRole.EMPLOYEE.getOidValue();
        }

        @Override
        protected String getDisplayName() {
            return IRREGULAR.getDisplayName();
        }
    }

    /**
     * This class represents a Manager User in the system.
     * It contains methods to build a UserType object with attributes specific to a Manager User.
     */
    public static class ManagerUserType extends RbacUserType {
        InitialBusinessRole primaryRole;
        InitialObjectsDefinition.LocationInitialBusinessRole locationBusinessRole;

        /**
         * Default constructor for the ManagerUser class.
         */
        public ManagerUserType(GeneratorOptions generatorOptions) {
            super(generatorOptions);
        }

        @Override
        public String getOrganizationOid() {
            return InitialObjectsDefinition.Organization.MANAGERS.getOidValue();
        }

        @Override
        public String getCorrespondingArchetypeOid() {
            return InitialObjectsDefinition.Archetypes.MANAGERS_USER.getOidValue();
        }

        @Override
        public InitialBusinessRole getPrimaryRole(boolean generateNew) {
            primaryRole = InitialObjectsDefinition.JobInitialBusinessRole.MANAGER;
            return primaryRole;
        }

        @Override
        public InitialObjectsDefinition.LocationInitialBusinessRole getLocationRole(boolean generateNew) {
            if (generateNew) {
                locationBusinessRole = getRandomLocationBusinessRole();
            }
            return locationBusinessRole;
        }

        @Override
        public String getBirthRole() {
            return InitialObjectsDefinition.BirthrightBusinessRole.EMPLOYEE.getOidValue();
        }

        @Override
        public List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> getPlanktonApplicationRoles() {
            return getRandomPlanktonRoles(0, generatorOptions);
        }

        @Override
        public String getLocality() {
            if (locationBusinessRole != null) {
                boolean candidate = isCandidate(90);
                if (candidate) {
                    return locationBusinessRole.getLocale();
                }
            }
            return null;
        }

        @Override
        public String getTitle() {
            return "manager";
        }

        @Override
        public void additionalChanges(UserType user) {
            additionalChangesOnAllUsers(user);
        }

        @Override
        protected String getDisplayName() {
            return MANAGERS.getDisplayName();
        }
    }

    /**
     * This class represents a Sales User in the system.
     * It contains methods to build a UserType object with attributes specific to a Sales User.
     */
    public static class SalesUserType extends RbacUserType {
        InitialBusinessRole primaryRole;
        InitialObjectsDefinition.LocationInitialBusinessRole locationBusinessRole;

        public SalesUserType(GeneratorOptions generatorOptions) {
            super(generatorOptions);
        }

        @Override
        public String getOrganizationOid() {
            return InitialObjectsDefinition.Organization.SALES.getOidValue();
        }

        @Override
        public String getBirthRole() {
            return InitialObjectsDefinition.BirthrightBusinessRole.EMPLOYEE.getOidValue();
        }

        @Override
        public String getCorrespondingArchetypeOid() {
            return InitialObjectsDefinition.Archetypes.SALES_USER.getOidValue();
        }

        @Override
        public InitialBusinessRole getPrimaryRole(boolean generateNew) {
            primaryRole = InitialObjectsDefinition.JobInitialBusinessRole.SALES;
            return primaryRole;
        }

        @Override
        public InitialObjectsDefinition.LocationInitialBusinessRole getLocationRole(boolean generateNew) {
            if (generateNew) {
                boolean candidate = isCandidate(10);
                if (candidate) {
                    locationBusinessRole = null;
                } else {
                    candidate = isCandidate(20);
                    if (candidate) {
                        locationBusinessRole = getRandomLocationBusinessRole();
                    } else {
                        locationBusinessRole = InitialObjectsDefinition.LocationInitialBusinessRole.LOCATION_NEW_YORK;
                    }
                }
            }
            return locationBusinessRole;
        }

        @Override
        public List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> getPlanktonApplicationRoles() {
            return null;
        }

        @Override
        public String getLocality() {
            if (locationBusinessRole != null) {
                return locationBusinessRole.getLocale();
            }
            return null;
        }

        @Override
        public String getTitle() {
            boolean candidate = isCandidate(80);
            if (candidate) {
                return "salesperson";
            }
            return null;
        }

        @Override
        public void additionalChanges(UserType user) {
            additionalChangesOnAllUsers(user);
        }

        @Override
        protected String getDisplayName() {
            return SALES.getDisplayName();
        }
    }

    /**
     * This class represents a Security Officer in the system.
     * It contains methods to build a UserType object with attributes specific to a Security Officer.
     */
    public static class SecurityOfficer extends RbacUserType {

        InitialBusinessRole primaryRole;

        /**
         * Default constructor for the SecurityOfficer class.
         */
        public SecurityOfficer(GeneratorOptions generatorOptions) {
            super(generatorOptions);
        }

        @Override
        public String getOrganizationOid() {
            return InitialObjectsDefinition.Organization.SECURITY_OFFICERS.getOidValue();
        }

        @Override
        public String getBirthRole() {
            return InitialObjectsDefinition.BirthrightBusinessRole.EMPLOYEE.getOidValue();
        }

        @Override
        public String getCorrespondingArchetypeOid() {
            return InitialObjectsDefinition.Archetypes.SECURITY_OFFICERS_USER.getOidValue();
        }

        @Override
        public InitialBusinessRole getPrimaryRole(boolean generateNew) {
            primaryRole = InitialObjectsDefinition.JobInitialBusinessRole.SECURITY_OFFICER;
            return primaryRole;
        }

        @Override
        public InitialObjectsDefinition.LocationInitialBusinessRole getLocationRole(boolean generateNew) {
            return null;
        }

        @Override
        public List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> getPlanktonApplicationRoles() {
            return null;
        }

        @Override
        public String getLocality() {
            return null;
        }

        @Override
        public String getTitle() {
            return null;
        }

        @Override
        public void additionalChanges(UserType user) {
            additionalChangesOnAllUsers(user);
//            user.extension(new ExtensionType());
//            ExtensionType ext = user.getExtension();
//            addExtensionValue(ext, "itemPath", "value");
        }

        @Override
        protected String getDisplayName() {
            return SECURITY_OFFICERS.getDisplayName();
        }

    }

    /**
     * This class represents a Contractor in the system.
     * It contains methods to build a UserType object with attributes specific to a Contractor.
     */
    public static class Contractor extends RbacUserType {
        InitialBusinessRole primaryRole;

        /**
         * Default constructor for the Contractor class.
         */
        public Contractor(GeneratorOptions generatorOptions) {
            super(generatorOptions);
        }

        @Override
        public String getBirthRole() {
            return InitialObjectsDefinition.BirthrightBusinessRole.CONTRACTOR.getOidValue();
        }

        @Override
        public String getOrganizationOid() {
            return InitialObjectsDefinition.Organization.CONTRACTORS.getOidValue();
        }

        @Override
        public String getCorrespondingArchetypeOid() {
            return InitialObjectsDefinition.Archetypes.CONTRACTORS_USER.getOidValue();
        }

        @Override
        public InitialBusinessRole getPrimaryRole(boolean generateNew) {
            primaryRole = InitialObjectsDefinition.BirthrightBusinessRole.CONTRACTOR;
            return primaryRole;
        }

        @Override
        public InitialObjectsDefinition.LocationInitialBusinessRole getLocationRole(boolean generateNew) {
            return null;
        }

        @Override
        public List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> getPlanktonApplicationRoles() {
            return getRandomPlanktonRoles(0, 7, generatorOptions);
        }

        @Override
        public String getLocality() {
            return null;
        }

        @Override
        public String getTitle() {
            return null;
        }

        @Override
        public void additionalChanges(UserType user) {
            additionalChangesOnAllUsers(user);
        }

        @Override
        protected String getDisplayName() {
            return CONTRACTORS.getDisplayName();
        }

    }

    public static class OutlierMatuzalem {

        Category category;

        public OutlierMatuzalem(@NotNull Category category) {
            this.category = category;
        }

        public void buildAndImport(
                @NotNull Log log,
                @NotNull RepositoryService repository,
                @NotNull GeneratorOptions generatorOptions,
                int index,
                @NotNull OperationResult result) {

            UserType user = new UserType();
            user.setName(PolyStringType.fromOrig("Matuzalem(" + index + "): " + category.getDisplayName()));
            generateRbacObject(user, category, generatorOptions);
            Category randomCategory = getRandomCategory(category);
            assignPrimaryAccessByCategory(user, category, generatorOptions);
            Category randomCategory2 = getRandomCategory(category, randomCategory);
            assignPrimaryAccessByCategory(user, randomCategory2, generatorOptions);
            Category randomCategory3 = getRandomCategory(category);
            if (randomCategory3 != randomCategory2) {
                assignPrimaryAccessByCategory(user, randomCategory3, generatorOptions);
            }
            importUserAndResolveAuxRoles(user, repository, generatorOptions, result, log);

        }
    }

    public static class OutlierJumper {
        private final Category category;

        public OutlierJumper(@NotNull Category category) {
            this.category = category;
        }

        public void buildAndImport(
                @NotNull Log log,
                @NotNull RepositoryService repository,
                @NotNull GeneratorOptions generatorOptions,
                int index,
                @NotNull OperationResult result) {

            UserType user = new UserType();
            user.setName(PolyStringType.fromOrig("Jumper (" + index + "): " + category.getDisplayName()));
            generateRbacObject(user, category, generatorOptions);
            Category randomCategory = getRandomCategory(category);
            assignPrimaryAccessByCategory(user, randomCategory, generatorOptions);

            importUserAndResolveAuxRoles(user, repository, generatorOptions, result, log);

        }

    }

    //TODO fixme
    public static class OutlierZombie {

        Category category;

        public OutlierZombie(@NotNull Category category) {
            this.category = category;
        }

        public void buildAndImport(
                @NotNull Log log,
                @NotNull RepositoryService repository,
                @NotNull GeneratorOptions generatorOptions,
                int index,
                @NotNull OperationResult result) {

            UserType user = new UserType();
            user.setName(PolyStringType.fromOrig("Zombie (" + index + "): " + category.getDisplayName()));
            generateRbacObject(user, category, generatorOptions);
            user.getAssignment().clear();
            String orgOid = retrieveOrgUnit(generatorOptions, category);
            if (orgOid != null) {
                user.getAssignment().add(createOrgAssignment(orgOid));
            }
            if (generatorOptions.isPlanktonDisable()) {
                List<InitialObjectsDefinition.NoiseApplicationBusinessAbstractRole> randomPlanktonRoles = getRandomNoiseRoles(6);
                for (InitialObjectsDefinition.NoiseApplicationBusinessAbstractRole randomPlanktonRole : randomPlanktonRoles) {
                    user.getAssignment().add(createRoleAssignment(randomPlanktonRole.getOidValue()));
                }
            } else {
                List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> randomPlanktonRoles = getRandomPlanktonRoles(10);
                for (InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole randomPlanktonRole : randomPlanktonRoles) {
                    user.getAssignment().add(createRoleAssignment(randomPlanktonRole.getOidValue()));
                }
            }
            importUserAndResolveAuxRoles(user, repository, generatorOptions, result, log);
        }
    }

    //TODO fixme
    public static class OutlierMask {

        Category category;

        public OutlierMask(@NotNull Category category) {
            this.category = category;
        }

        public void buildAndImport(
                @NotNull Log log,
                @NotNull RepositoryService repository,
                @NotNull GeneratorOptions generatorOptions,
                int index,
                @NotNull OperationResult result) {

            UserType user = new UserType();
            user.setName(PolyStringType.fromOrig("Mask (" + index + "): " + category.getDisplayName()));
            generateRbacObject(user, category, generatorOptions);
            List<InitialObjectsDefinition.NoiseApplicationBusinessAbstractRole> randomNoiseRoles = getRandomNoiseRoles(3);
            for (InitialObjectsDefinition.NoiseApplicationBusinessAbstractRole noiseRole : randomNoiseRoles) {
                user.getAssignment().add(createRoleAssignment(noiseRole.getOidValue()));
            }

            importUserAndResolveAuxRoles(user, repository, generatorOptions, result, log);

        }
    }

    protected static void additionalChangesOnAllUsers(UserType user){
//            user.extension(new ExtensionType());
//            ExtensionType ext = user.getExtension();
//            addExtensionValue(ext, "itemPath", "value");
    }
}
