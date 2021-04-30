/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.report;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType.F_REPORT_REF;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

/**
 * Mapping between {@link QReportData} and {@link ReportDataType}.
 */
public class QReportDataMapping
        extends QObjectMapping<ReportDataType, QReportData, MReportData> {

    public static final String DEFAULT_ALIAS_NAME = "repout";

    public static final QReportDataMapping INSTANCE = new QReportDataMapping();

    private QReportDataMapping() {
        super(QReportData.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ReportDataType.class, QReportData.class);

        addItemMapping(F_REPORT_REF, refMapper(
                q -> q.reportRefTargetOid,
                q -> q.reportRefTargetType,
                q -> q.reportRefRelationId));
    }

    @Override
    protected QReportData newAliasInstance(String alias) {
        return new QReportData(alias);
    }

    @Override
    public QReportDataMapping createTransformer(SqlTransformerSupport transformerSupport) {
        return this;
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
