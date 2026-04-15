/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */
package com.evolveum.midpoint.smart.impl.activities.objectTypeStatisticsComputation;

import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionInfo;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;

/**
 * Work definition for object type statistics computation.
 *
 * <p>Defines the resource, kind and intent for which statistics
 * should be computed.</p>
 */
public class ObjectTypeStatisticsComputationWorkDefinition extends AbstractWorkDefinition {

    private final @NotNull String resourceOid;
    private final @NotNull ShadowKindType shadowTypeKind;
    private final @NotNull String intent;
    @Nullable private final String statisticsObjectOid;


    public ObjectTypeStatisticsComputationWorkDefinition(@NotNull WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
        var typed = (ObjectTypeStatisticsComputationWorkDefinitionType) info.getBean();

        resourceOid = configNonNull(
                Referencable.getOid(typed.getResourceRef()),
                "No resource OID specified");

        shadowTypeKind = configNonNull(
                typed.getKind(),
                "No shadow kind specified");

        intent = configNonNull(
                typed.getIntent(),
                "No shadow intent specified");

        statisticsObjectOid = Referencable.getOid(typed.getStatisticsRef());


    }

    public @NotNull String getResourceOid() {
        return resourceOid;
    }

    public @NotNull ShadowKindType getShadowTypeKind() {
        return shadowTypeKind;
    }

    public @NotNull String getIntent() {
        return intent;
    }

    public @Nullable String getStatisticsObjectOid() {
        return statisticsObjectOid;
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
        DebugUtil.debugDumpWithLabelLn(sb, "shadowTypeKind", shadowTypeKind, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "intent", intent, indent + 1);
            DebugUtil.debugDumpWithLabel(sb, "statisticsObjectOid", statisticsObjectOid, indent + 1);
    }
}
