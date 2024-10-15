/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.mining.cluster;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisDetectionPatternType.*;

import java.util.*;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.ResultListRowTransformer;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.mining.outlier.MOutlier;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.util.exception.SchemaException;

public class QClusterDetectedPatternMapping extends QContainerMapping<RoleAnalysisDetectionPatternType,
        QClusterDetectedPattern, MClusterDetectedPattern, MClusterObject> {

    private static QClusterDetectedPatternMapping instance;

    @NotNull
    public static QClusterDetectedPatternMapping initMapping(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QClusterDetectedPatternMapping(repositoryContext);
        }
        return get();
    }

    public static @NotNull QClusterDetectedPatternMapping get() {
        return Objects.requireNonNull(instance);
    }

    protected QClusterDetectedPatternMapping(SqaleRepoContext repositoryContext) {
        super(QClusterDetectedPattern.TABLE_NAME, QClusterDetectedPattern.ALIAS, RoleAnalysisDetectionPatternType.class, QClusterDetectedPattern.class, repositoryContext);

        addRelationResolver(PrismConstants.T_PARENT,
                // mapping supplier is used to avoid cycles in the initialization code
                TableRelationResolver.usingJoin(
                        QAssignmentHolderMapping::getAssignmentHolderMapping,
                        // Adding and(q.ownerType.eq(p.objectType) doesn't help the planner.
                        (q, p) -> q.ownerOid.eq(p.oid)));
        addItemMapping(F_REDUCTION_COUNT, doubleMapper(v -> v.reductionCount));
    }

    @Override
    protected QClusterDetectedPattern newAliasInstance(String alias) {
        return new QClusterDetectedPattern(alias);
    }

    @Override
    public MClusterDetectedPattern newRowObject() {
        return new MClusterDetectedPattern();
    }

    @Override
    public MClusterDetectedPattern newRowObject(MClusterObject ownerRow) {
        var ret = newRowObject();
        ret.ownerOid = ownerRow.oid;
        return ret;
    }

    @Override
    public MClusterDetectedPattern insert(RoleAnalysisDetectionPatternType pattern, MClusterObject ownerRow, JdbcSession jdbcSession) throws SchemaException {
        var row  = initRowObject(pattern, ownerRow);
        row.reductionCount = pattern.getReductionCount();
        insert(row, jdbcSession);
        return row;
    }

    @Override
    public RoleAnalysisDetectionPatternType toSchemaObject(MClusterDetectedPattern row) throws SchemaException {
        return new RoleAnalysisDetectionPatternType().id(row.cid)
                .reductionCount(row.reductionCount);
    }

    @Override
    public ResultListRowTransformer<RoleAnalysisDetectionPatternType, QClusterDetectedPattern, MClusterDetectedPattern> createRowTransformer(SqlQueryContext<RoleAnalysisDetectionPatternType, QClusterDetectedPattern, MClusterDetectedPattern> sqlQueryContext, JdbcSession jdbcSession, Collection<SelectorOptions<GetOperationOptions>> options) {
        Map<UUID, PrismObject<RoleAnalysisClusterType>> casesCache = new HashMap<>();

        return (tuple, entityPath) -> {
            MClusterDetectedPattern row = Objects.requireNonNull(tuple.get(entityPath));
            UUID caseOid = row.ownerOid;
            PrismObject<RoleAnalysisClusterType> aCase = casesCache.get(caseOid);
            if (aCase == null) {
                aCase = ((SqaleQueryContext<?, ?, ?>) sqlQueryContext)
                        .loadObject(jdbcSession, RoleAnalysisClusterType.class, caseOid, options);
                casesCache.put(caseOid, aCase);
            }

            PrismContainer<RoleAnalysisDetectionPatternType> workItemContainer = aCase.findContainer(RoleAnalysisClusterType.F_DETECTED_PATTERN);
            if (workItemContainer == null) {
                throw new SystemException("Cluster " + aCase + " has no detected patterns even if it should have " + tuple);
            }
            PrismContainerValue<RoleAnalysisDetectionPatternType> workItemPcv = workItemContainer.findValue(row.cid);
            if (workItemPcv == null) {
                throw new SystemException("Cluster " + aCase + " has no detected pattern with ID " + row.cid);
            }
            var containerable = workItemPcv.asContainerable();
            attachContainerIdPath(containerable, tuple, entityPath);
            return containerable;
        };
    }
}
