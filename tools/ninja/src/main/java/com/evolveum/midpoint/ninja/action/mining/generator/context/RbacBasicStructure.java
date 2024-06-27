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

/**
 * This interface represents a basic property used for rbac user type generation.
 * <p>
 * Part of RBAC Testing Data, which provides testing data for role mining and other RBAC-related processes.
 */
public interface RbacBasicStructure {

    String getBirthRole();

    String getOrganizationOid();

    String getCorrespondingArchetypeOid();

    InitialBusinessRole getPrimaryRole(boolean generateNew);

    InitialObjectsDefinition.LocationInitialBusinessRole getLocationRole(boolean generateNew);

    List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> getPlanktonApplicationRoles();

    String getLocality();

    String getTitle();

    void buildAndImportObjects(
            @NotNull Log log,
            @NotNull RepositoryService repository,
            @NotNull GeneratorOptions generatorOptions,
            int total, Set<String> names,
            @NotNull OperationResult result);

    void additionalChanges(UserType user);
}
