/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.cleanup;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.task.work.LegacyWorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPoliciesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupWorkDefinitionType;

import org.jetbrains.annotations.Nullable;

public class CleanupWorkDefinition extends AbstractWorkDefinition {

    @Nullable
    private final CleanupPoliciesType cleanupPolicies;

    CleanupWorkDefinition(WorkDefinitionSource source) {
        if (source instanceof LegacyWorkDefinitionSource) {
            LegacyWorkDefinitionSource legacySource = (LegacyWorkDefinitionSource) source;
            cleanupPolicies = legacySource.getExtensionItemRealValue(SchemaConstants.MODEL_EXTENSION_CLEANUP_POLICIES,
                    CleanupPoliciesType.class);
        } else {
            CleanupWorkDefinitionType typedDefinition = (CleanupWorkDefinitionType)
                    ((WorkDefinitionWrapper.TypedWorkDefinitionWrapper) source).getTypedDefinition();
            cleanupPolicies = typedDefinition.getPolicies();
        }
    }

    public @Nullable CleanupPoliciesType getCleanupPolicies() {
        return cleanupPolicies;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "cleanupPolicies", cleanupPolicies, indent + 1);
    }
}
