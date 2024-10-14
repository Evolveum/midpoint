/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.mining.cluster;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType.F_DETECTED_PATTERN;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType.F_ROLE_ANALYSIS_SESSION_REF;

import com.evolveum.midpoint.repo.sqale.qmodel.mining.outlier.QOutlierMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.mining.session.QSessionObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

import java.util.Objects;

public class QClusterObjectMapping
        extends QAssignmentHolderMapping<RoleAnalysisClusterType, QClusterObject, MClusterObject> {

    public static final String DEFAULT_ALIAS_NAME = "rac";
    private static QClusterObjectMapping instance;

    public static QClusterObjectMapping getInstance() {
        return Objects.requireNonNull(instance);
    }

    public static QClusterObjectMapping init(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance =  new QClusterObjectMapping(repositoryContext);
        }
        return getInstance();
    }

    private QClusterObjectMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QClusterObject.TABLE_NAME, DEFAULT_ALIAS_NAME,
                RoleAnalysisClusterType.class, QClusterObject.class, repositoryContext);

        addRefMapping(F_ROLE_ANALYSIS_SESSION_REF,
                q -> q.parentRefTargetOid,
                q -> q.parentRefTargetType,
                q -> q.parentRefRelationId,
                QSessionObjectMapping::getInstance);

        addContainerTableMapping(F_DETECTED_PATTERN, QClusterDetectedPatternMapping.initMapping(repositoryContext),
            joinOn( (cluster,pattern) -> cluster.oid.eq(pattern.ownerOid))
        );
    }

    @Override
    protected QClusterObject newAliasInstance(String alias) {
        return new QClusterObject(alias);
    }

    @Override
    public MClusterObject newRowObject() {
        return new MClusterObject();
    }

    @Override
    public @NotNull MClusterObject toRowObjectWithoutFullObject(
            RoleAnalysisClusterType clusterObject, JdbcSession jdbcSession) {
        MClusterObject row = super.toRowObjectWithoutFullObject(clusterObject, jdbcSession);

        setReference(clusterObject.getRoleAnalysisSessionRef(),
                o -> row.parentRefTargetOid = o,
                t -> row.parentRefTargetType = t,
                r -> row.parentRefRelationId = r);

        return row;
    }

    @Override
    public void storeRelatedEntities(@NotNull MClusterObject row, @NotNull RoleAnalysisClusterType schemaObject, @NotNull JdbcSession jdbcSession) throws SchemaException {
        super.storeRelatedEntities(row, schemaObject, jdbcSession);
        for (var detectedPattern :schemaObject.getDetectedPattern()) {
            QClusterDetectedPatternMapping.get().insert(detectedPattern, row, jdbcSession);
        }
    }
}
