/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.report;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType.F_REPORT_REF;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

/**
 * Mapping between {@link QReportData} and {@link ReportDataType}.
 */
public class QReportDataMapping
        extends QAssignmentHolderMapping<ReportDataType, QReportData, MReportData> {

    public static final String DEFAULT_ALIAS_NAME = "repout";

    public static QReportDataMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QReportDataMapping(repositoryContext);
    }

    private QReportDataMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QReportData.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ReportDataType.class, QReportData.class, repositoryContext);

        addRefMapping(F_REPORT_REF,
                q -> q.reportRefTargetOid,
                q -> q.reportRefTargetType,
                q -> q.reportRefRelationId,
                QReportMapping::get);
    }

    @Override
    protected QReportData newAliasInstance(String alias) {
        return new QReportData(alias);
    }

    @Override
    public MReportData newRowObject() {
        return new MReportData();
    }

    @Override
    public @NotNull MReportData toRowObjectWithoutFullObject(
            ReportDataType reportData, JdbcSession jdbcSession) {
        MReportData row = super.toRowObjectWithoutFullObject(reportData, jdbcSession);

        setReference(reportData.getReportRef(),
                o -> row.reportRefTargetOid = o,
                t -> row.reportRefTargetType = t,
                r -> row.reportRefRelationId = r);

        return row;
    }
}
