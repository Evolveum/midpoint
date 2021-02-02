/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmapping;

import com.evolveum.midpoint.repo.sqale.qbean.MReportOutput;
import com.evolveum.midpoint.repo.sqale.qmodel.QReportOutput;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
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

        // TODO mapping
    }

    @Override
    protected QReportOutput newAliasInstance(String alias) {
        return new QReportOutput(alias);
    }

    @Override
    public ObjectSqlTransformer<ReportDataType, QReportOutput, MReportOutput>
    createTransformer(SqlTransformerContext transformerContext, SqlRepoContext sqlRepoContext) {
        // no special class needed, no additional columns
        return new ObjectSqlTransformer<>(transformerContext, this);
    }

    @Override
    public MReportOutput newRowObject() {
        return new MReportOutput();
    }
}
