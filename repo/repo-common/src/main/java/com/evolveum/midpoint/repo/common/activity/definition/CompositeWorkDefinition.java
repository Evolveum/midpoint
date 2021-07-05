/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityCompositionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;

/**
 * Definition for pure composite activity.
 */
public class CompositeWorkDefinition extends AbstractWorkDefinition {

    private final ActivityCompositionType composition;

    CompositeWorkDefinition(ActivityCompositionType composition) {
        this.composition = composition;
    }

    public ActivityCompositionType getComposition() {
        return composition;
    }

    public List<ActivityDefinition<?>> createChildDefinitions(WorkDefinitionFactory workDefinitionFactory) throws SchemaException {
        List<ActivityDefinition<?>> definitions = new ArrayList<>();
        for (ActivityDefinitionType activityDefinitionBean : composition.getActivity()) {
            definitions.add(ActivityDefinition.createChild(activityDefinitionBean, workDefinitionFactory));
        }
        return definitions;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "composition", composition, indent+1);
    }
}
