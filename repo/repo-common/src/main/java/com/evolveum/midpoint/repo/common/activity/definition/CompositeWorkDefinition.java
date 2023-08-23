/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityCompositionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;

/**
 * Definition for pure composite activity.
 */
public class CompositeWorkDefinition extends AbstractWorkDefinition implements AffectedObjectsProvider {

    @NotNull private final ActivityCompositionType composition;

    CompositeWorkDefinition(
            @NotNull ActivityCompositionType composition,
            @NotNull QName activityTypeName,
            @NotNull ConfigurationItemOrigin origin) {
        super(activityTypeName, origin);
        this.composition = composition;
    }

    public @NotNull ActivityCompositionType getComposition() {
        return composition;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "composition", composition, indent+1);
    }

    /**
     * Composite activities process multiple object sets, so they have to be treated differently.
     * See {@link #getAffectedObjectsInformation()}.
     */
    @Override
    public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation()
            throws SchemaException, ConfigurationException {
        return AffectedObjectsInformation.ObjectSet.notSupported();
    }

    @NotNull
    public AffectedObjectsInformation getAffectedObjectsInformation() throws SchemaException, ConfigurationException {
        List<AffectedObjectsInformation> informationForChildren = new ArrayList<>();
        for (ActivityDefinitionType childDefinitionBean : composition.getActivity()) {
            var childDefinition = ActivityDefinition.createChild(childDefinitionBean, getOrigin()); // the origin is not relevant
            informationForChildren.add(childDefinition.getAffectedObjectsInformation());
        }
        return AffectedObjectsInformation.complex(informationForChildren);
    }
}
