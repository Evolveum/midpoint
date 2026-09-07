/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.other;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.QResourceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeIdentificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SmartIntegrationArtifactScopeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SmartIntegrationArtifactType;

/**
 * Mapping between {@link QSmartIntegrationArtifact} and {@link SmartIntegrationArtifactType}.
 */
public class QSmartIntegrationArtifactMapping
        extends QAssignmentHolderMapping<SmartIntegrationArtifactType, QSmartIntegrationArtifact, MSmartIntegrationArtifact> {

    public static final String DEFAULT_ALIAS_NAME = "sia";

    public static QSmartIntegrationArtifactMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QSmartIntegrationArtifactMapping(repositoryContext);
    }

    private QSmartIntegrationArtifactMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QSmartIntegrationArtifact.TABLE_NAME, DEFAULT_ALIAS_NAME,
                SmartIntegrationArtifactType.class, QSmartIntegrationArtifact.class, repositoryContext);

        // @formatter:off
        addNestedMapping(SmartIntegrationArtifactType.F_SCOPE, SmartIntegrationArtifactScopeType.class)
                .addRefMapping(SmartIntegrationArtifactScopeType.F_RESOURCE_REF,
                        q -> q.resourceRefTargetOid,
                        q -> q.resourceRefTargetType,
                        q -> q.resourceRefRelationId,
                        QResourceMapping::get)
                .addItemMapping(SmartIntegrationArtifactScopeType.F_OBJECT_CLASS, uriMapper(q -> q.objectClassId))
                .addItemMapping(SmartIntegrationArtifactScopeType.F_FOCUS_TYPE, uriMapper(q -> q.focusTypeId))
                .addNestedMapping(SmartIntegrationArtifactScopeType.F_OBJECT_TYPE, ResourceObjectTypeIdentificationType.class)
                    .addItemMapping(ResourceObjectTypeIdentificationType.F_KIND, enumMapper(q -> q.kind))
                    .addItemMapping(ResourceObjectTypeIdentificationType.F_INTENT, stringMapper(q -> q.intent));
        // @formatter:on
    }

    @Override
    protected QSmartIntegrationArtifact newAliasInstance(String alias) {
        return new QSmartIntegrationArtifact(alias);
    }

    @Override
    public MSmartIntegrationArtifact newRowObject() {
        return new MSmartIntegrationArtifact();
    }

    @Override
    public @NotNull MSmartIntegrationArtifact toRowObjectWithoutFullObject(
            SmartIntegrationArtifactType artifact, JdbcSession jdbcSession) {
        MSmartIntegrationArtifact row = super.toRowObjectWithoutFullObject(artifact, jdbcSession);

        var scope = artifact.getScope();
        if (scope != null) {
            setReference(scope.getResourceRef(),
                    o -> row.resourceRefTargetOid = o,
                    t -> row.resourceRefTargetType = t,
                    r -> row.resourceRefRelationId = r);
            row.objectClassId = processCacheableUri(scope.getObjectClass());
            var objectType = scope.getObjectType();
            if (objectType != null) {
                row.kind = objectType.getKind();
                row.intent = objectType.getIntent();
            }
            row.focusTypeId = processCacheableUri(scope.getFocusType());
        }
        return row;
    }
}
