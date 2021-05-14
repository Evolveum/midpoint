/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.definition;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityCompositionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Definition for pure composite activity.
 */
public class CompositeWorkDefinition extends AbstractWorkDefinition {

    private static final Trace LOGGER = TraceManager.getTrace(CompositeWorkDefinition.class);

    private final ActivityCompositionType composition;

    CompositeWorkDefinition(ActivityCompositionType composition) {
        this.composition = composition;
    }

    public ActivityCompositionType getComposition() {
        return composition;
    }

    public List<ActivityDefinition<?>> createChildDefinitions() throws SchemaException {
        List<ActivityDefinition<?>> definitions = new ArrayList<>();
        for (ActivityDefinitionType activityDefinitionBean : composition.getActivity()) {
            definitions.add(ActivityDefinition.createChild(activityDefinitionBean, getOwningActivity()));
        }
        LOGGER.trace("Child definitions:\n{}", DebugUtil.debugDumpLazily(definitions));
        return definitions;
    }

    @Override
    public @NotNull QName getType() {
        return ActivityCompositionType.COMPLEX_TYPE;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "composition", composition, indent+1);
    }
}
