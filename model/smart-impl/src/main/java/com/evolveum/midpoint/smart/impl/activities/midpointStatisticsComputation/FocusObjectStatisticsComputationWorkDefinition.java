/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */
package com.evolveum.midpoint.smart.impl.activities.midpointStatisticsComputation;

import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BasicObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusObjectStatisticsComputationWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionInfo;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import javax.xml.namespace.QName;

/**
 * Work definition for focus object statistics computation.
 *
 * <p>Defines the object type (QName) for which statistics should be computed.</p>
 */
public class FocusObjectStatisticsComputationWorkDefinition extends AbstractWorkDefinition {

    private final @NotNull QName objectType;
    private final @NotNull String resourceOid;
    private final @NotNull ResourceObjectTypeIdentification typeIdentification;
    @Nullable private final String statisticsObjectOid;

    public FocusObjectStatisticsComputationWorkDefinition(@NotNull WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
        var typed = (FocusObjectStatisticsComputationWorkDefinitionType) info.getBean();

        objectType = configNonNull(
                typed.getType(),
                "No object type specified");

        resourceOid = configNonNull(
                Referencable.getOid(typed.getResourceRef()),
                "No resource OID specified");
        var kind = configNonNull(
                typed.getKind(),
                "No shadow kind specified");
        var intent = configNonNull(
                typed.getIntent(),
                "No shadow intent specified");
        typeIdentification = ResourceObjectTypeIdentification.of(kind, intent);
        statisticsObjectOid = Referencable.getOid(typed.getStatisticsRef());
    }

    public @NotNull QName getObjectType() {
        return objectType;
    }

    public @NotNull String getResourceOid() {
        return resourceOid;
    }

    public @NotNull ResourceObjectTypeIdentification getTypeIdentification() {
        return typeIdentification;
    }

    public @NotNull ShadowKindType getKind() {
        return typeIdentification.getKind();
    }

    public @NotNull String getIntent() {
        return typeIdentification.getIntent();
    }

    public @Nullable String getStatisticsObjectOid() {
        return statisticsObjectOid;
    }

    @Override
    public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation(
            @Nullable AbstractActivityWorkStateType state) {
        return AffectedObjectsInformation.ObjectSet.repository(
                new BasicObjectSetType()
                        .type(objectType));
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "objectType", objectType, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "resourceOid", resourceOid, indent + 1);
        DebugUtil.debugDumpShortWithLabelLn(sb, "typeIdentification", typeIdentification, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "statisticsObjectOid", statisticsObjectOid, indent + 1);
    }
}
