/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl;

import java.util.Map;

import net.sf.jasperreports.engine.JRDataset;
import net.sf.jasperreports.engine.JRValueParameter;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.query.AbstractQueryExecuterFactory;
import net.sf.jasperreports.engine.query.JRQueryExecuter;

import com.evolveum.midpoint.report.api.ReportService;
import com.evolveum.midpoint.schema.util.ReportTypeUtil;

public class MidPointQueryExecutorFactory extends AbstractQueryExecuterFactory {

    private static final Object[] MIDPOINT_BUILTIN_PARAMETERS = {
            ReportService.PARAMETER_REPORT_SERVICE, ReportTypeUtil.PARAMETER_OPERATION_RESULT,
            ReportTypeUtil.PARAMETER_REPORT_OBJECT, ReportTypeUtil.PARAMETER_REPORT_OID,
            ReportTypeUtil.PARAMETER_TASK, "midpoint.connection"
    };

    @Override
    public Object[] getBuiltinParameters() {
        return MIDPOINT_BUILTIN_PARAMETERS;
    }

    @Override
    public JRQueryExecuter createQueryExecuter(JasperReportsContext jasperReportsContext, JRDataset dataset,
            Map<String, ? extends JRValueParameter> parameters) {

        return new MidPointLocalQueryExecutor(jasperReportsContext, dataset, parameters);
    }

    @Override
    public boolean supportsQueryParameterType(String className) {
        return true;
    }
}
