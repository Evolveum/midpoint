/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.cleanup;

import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPoliciesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupWorkDefinitionType;

public class CleanupWorkDefinition extends AbstractWorkDefinition {

    @Nullable
    private final CleanupPoliciesType cleanupPolicies;

    CleanupWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) {
        super(info);
        var typedDefinition = (CleanupWorkDefinitionType) info.getBean();
        cleanupPolicies = typedDefinition.getPolicies();
    }

    @Nullable CleanupPoliciesType getCleanupPolicies() {
        return cleanupPolicies;
    }

    @Override
    public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation() {
        return AffectedObjectsInformation.ObjectSet.notSupported(); // not easily determinable nor describable
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "cleanupPolicies", cleanupPolicies, indent + 1);
    }
}
