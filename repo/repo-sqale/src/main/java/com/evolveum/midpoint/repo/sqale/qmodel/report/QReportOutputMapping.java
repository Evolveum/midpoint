/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.report;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType.F_REPORT_REF;

import com.evolveum.midpoint.repo.sqale.RefItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

/**
 * Mapping between {@link QReportOutput} and {@link ReportDataType}.
 */
public class QReportOutputMapping
        extends QObjectMapping<ReportDataType, QReportOutput, MReportOutput> {

    public static final String DEFAULT_ALIAS_NAME = "repout";

    public static final QReportOutputMapping INSTANCE = new QReportOutputMapping();

    private QReportOutputMapping() {
        super(QReportOutput.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ReportDataType.class, QReportOutput.class);

        addItemMapping(F_REPORT_REF, RefItemFilterProcessor.mapper(
                path(q -> q.reportRefTargetOid),
                path(q -> q.reportRefTargetType),
                path(q -> q.reportRefRelationId)));
    }

    @Override
    protected QReportOutput newAliasInstance(String alias) {
        return new QReportOutput(alias);
    }

    @Override
    public ObjectSqlTransformer<ReportDataType, QReportOutput, MReportOutput>
    createTransformer(SqlTransformerContext transformerContext) {
        // no special class needed, no additional columns
        return new ObjectSqlTransformer<>(transformerContext, this);
    }

    @Override
    public MReportOutput newRowObject() {
        return new MReportOutput();
    }
}
