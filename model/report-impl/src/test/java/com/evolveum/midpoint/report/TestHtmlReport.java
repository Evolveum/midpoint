/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author skublik
 */

public class TestHtmlReport extends BasicNewReportTest {

    @Override
    protected ExportConfigurationType getExportConfiguration() {
        ExportConfigurationType config = new ExportConfigurationType();
        config.setType(ExportType.HTML);
        return config;
    }

    protected List<String> basicCheckOutputFile(PrismObject<ReportType> report) throws IOException, SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        List<String> lines = super.basicCheckOutputFile(report);

        if (lines.size() < 10) {
            fail("Html report too short ("+lines.size()+" lines)");
        }
        return lines;
    }
}
