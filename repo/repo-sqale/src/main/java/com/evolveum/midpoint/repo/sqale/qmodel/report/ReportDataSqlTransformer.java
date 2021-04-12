/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.report;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

public class ReportDataSqlTransformer
        extends ObjectSqlTransformer<ReportDataType, QReportData, MReportData> {

    public ReportDataSqlTransformer(
            SqlTransformerSupport transformerSupport, QReportDataMapping mapping) {
        super(transformerSupport, mapping);
    }

    @Override
    public @NotNull MReportData toRowObjectWithoutFullObject(
            ReportDataType reportData, JdbcSession jdbcSession) {
        MReportData row = super.toRowObjectWithoutFullObject(reportData, jdbcSession);

        setReference(reportData.getReportRef(), jdbcSession,
                o -> row.reportRefTargetOid = o,
                t -> row.reportRefTargetType = t,
                r -> row.reportRefRelationId = r);

        return row;
    }
}
