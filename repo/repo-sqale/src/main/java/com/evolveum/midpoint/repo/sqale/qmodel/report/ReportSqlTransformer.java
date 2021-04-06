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
import com.evolveum.midpoint.xml.ns._public.common.common_3.JasperReportEngineConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

public class ReportSqlTransformer
        extends ObjectSqlTransformer<ReportType, QReport, MReport> {

    public ReportSqlTransformer(
            SqlTransformerSupport transformerSupport, QReportMapping mapping) {
        super(transformerSupport, mapping);
    }

    @Override
    public @NotNull MReport toRowObjectWithoutFullObject(
            ReportType schemaObject, JdbcSession jdbcSession) {
        MReport row = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        JasperReportEngineConfigurationType jasper = schemaObject.getJasper();
        if (jasper != null) {
            row.orientation = jasper.getOrientation();
            row.parent = jasper.isParent();
        }

        return row;
    }
}
