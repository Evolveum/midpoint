/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining.generator.context;

import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.action.mining.generator.GeneratorOptions;
import com.evolveum.midpoint.ninja.action.mining.generator.object.InitialBusinessRole;
import com.evolveum.midpoint.ninja.action.mining.generator.object.InitialObjectsDefinition;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;

import org.jetbrains.annotations.Nullable;

/**
 * This interface represents a basic property used for rbac user type generation.
 * <p>
 * Part of RBAC Testing Data, which provides testing data for role mining and other RBAC-related processes.
 */
public interface RbacBasicStructure {

    String getBirthRole();

    String getProfessionOrganizationOid();

    String getCorrespondingArchetypeOid();

    @NotNull
    InitialBusinessRole getPrimaryRole();

    @Nullable
    default InitialObjectsDefinition.LocationInitialBusinessRole getLocationRole(){
        InitialObjectsDefinition.LocationOrg localityOrg = getLocalityOrg();
        if (localityOrg == null) {
            return null;
        }
        return localityOrg.getAssociatedLocationRole();
    }

    @Nullable
    default List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> getPlanktonApplicationRoles(){
        return null;
    }

    default @Nullable Boolean isNotAssignToLocationOrg(){
        return getLocationRole() == null;
    }

    @Nullable
    InitialObjectsDefinition.LocationOrg getLocalityOrg();

    @Nullable
    String getTitle();

    void buildAndImportObjects(
            @NotNull Log log,
            @NotNull RepositoryService repository,
            @NotNull GeneratorOptions generatorOptions,
            int total, Set<String> names,
            @NotNull OperationResult result);

    void additionalChanges(UserType user);
}
