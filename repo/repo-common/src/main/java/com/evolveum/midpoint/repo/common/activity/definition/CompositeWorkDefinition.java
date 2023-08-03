/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.util.CloneUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Definition for pure composite activity.
 */
public class CompositeWorkDefinition extends AbstractWorkDefinition {

    @NotNull private final ActivityCompositionType composition;

    CompositeWorkDefinition(@NotNull ActivityCompositionType composition, @NotNull QName activityTypeName) {
        super(activityTypeName);
        this.composition = composition;
    }

    public @NotNull ActivityCompositionType getComposition() {
        return composition;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "composition", composition, indent+1);
    }

    @Override
    public @Nullable TaskAffectedObjectsType getAffectedObjects() throws SchemaException, ConfigurationException {

        // We rely on the duplicate filtering provided by sets; it should be adequate here.
        Set<ActivityAffectedObjectsType> objectsByActivityType = new HashSet<>();

        for (ActivityDefinitionType activityDefinitionBean : composition.getActivity()) {
            var forChild = CommonTaskBeans.get().activityManager.computeAffectedObjects(activityDefinitionBean);
            objectsByActivityType.addAll(forChild.getActivity());
        }
        var result = new TaskAffectedObjectsType();
        result.getActivity().addAll(
                CloneUtil.cloneCollectionMembers(objectsByActivityType));
        return result;
    }
}
