/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.scanner;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class FocusValidityScanWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

    @NotNull private final ObjectSetType objects;
    @NotNull private final ValidityScanQueryStyleType queryStyle;
    private final TimeValidityPolicyConstraintType validityConstraint;

    FocusValidityScanWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) {
        super(info);
        var typedDefinition = (FocusValidityScanWorkDefinitionType) info.getBean();

        objects = ObjectSetUtil.emptyIfNull(typedDefinition.getObjects());
        // We allow user to use types above FocusType if he needs to check e.g. assignments validity
        // on AssignmentHolderType objects.
        ObjectSetUtil.applyDefaultObjectType(objects, FocusType.COMPLEX_TYPE);

        queryStyle = Objects.requireNonNullElse(typedDefinition.getQueryStyle(), ValidityScanQueryStyleType.SINGLE_QUERY);
        validityConstraint = typedDefinition.getValidityConstraint();
    }

    @Override
    public @NotNull ObjectSetType getObjectSetSpecification() {
        return objects;
    }

    @NotNull ValidityScanQueryStyleType getQueryStyle() {
        return queryStyle;
    }

    TimeValidityPolicyConstraintType getValidityConstraint() {
        return validityConstraint;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "objects", objects, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "queryStyle", queryStyle, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "validityConstraint", validityConstraint, indent+1);
    }
}
