/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.smart.impl.activities;

import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionInfo;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BasicResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectClassStatisticsComputationWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Work definition for object class statistics computation.
 *
 * <p>Defines the resource and object class for which statistics
 * should be computed.</p>
 */
public class ObjectClassStatisticsComputationWorkDefinition extends AbstractWorkDefinition {

    private final @NotNull String resourceOid;
    private final @NotNull QName objectClassName;

    public ObjectClassStatisticsComputationWorkDefinition(@NotNull WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
        var typed = (ObjectClassStatisticsComputationWorkDefinitionType) info.getBean();

        resourceOid = configNonNull(
                Referencable.getOid(typed.getResourceRef()),
                "No resource OID specified");

        objectClassName = configNonNull(
                typed.getObjectClassName(),
                "No object class name specified");
    }

    public @NotNull String getResourceOid() {
        return resourceOid;
    }

    public @NotNull QName getObjectClassName() {
        return objectClassName;
    }

    @Override
    public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation(
            @Nullable AbstractActivityWorkStateType state) {
        return AffectedObjectsInformation.ObjectSet.resource(
                new BasicResourceObjectSetType()
                        .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE));
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "resourceOid", resourceOid, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "objectClassName", objectClassName, indent + 1);
    }
}
