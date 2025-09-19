/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public class ObjectCollectionViewUtil {

    public static @NotNull List<ObjectReferenceType> getArchetypeReferencesList(CompiledObjectCollectionView collectionView) {
        if (collectionView == null) {
            return new ArrayList<>();
        }
        var archetypeRef = collectionView.getArchetypeRef();
        return archetypeRef != null ? Arrays.asList(archetypeRef) : new ArrayList<>();
    }

    /**
     * Returns true in case there is at least one collection view in the system which has an assignment
     * to Business Role archetype (or any of its sub-archetypes).
     * This condition should be satisfied e.g. for creating a new role candidate (while role mining).
     * @param modelServiceLocator
     * @return
     */
    public static boolean businessRoleArchetypeViewExists(ModelServiceLocator modelServiceLocator) {
        CompiledGuiProfile compiledGuiProfile = WebComponentUtil.getCompiledGuiProfile();
        var views = compiledGuiProfile.findAllApplicableArchetypeViews(RoleType.class, OperationTypeType.ADD);
        for (var view : views) {
            if (isBusinessRole(view, modelServiceLocator)) {
                return true;
            }
         }
        return false;
    }

    public static boolean isBusinessRole(CompiledObjectCollectionView view, ModelServiceLocator modelServiceLocator) {
        String archetypeOid = view.getArchetypeOid();
        if (archetypeOid == null) {
            return false;
        }
        return modelServiceLocator.getModelInteractionService().isSubarchetypeOrArchetype(archetypeOid,
                SystemObjectsType.ARCHETYPE_BUSINESS_ROLE.value(), new OperationResult("check archetype"));
    }
}
