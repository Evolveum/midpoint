/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.api;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author lazyman
 * @author katkav
 */
public interface ReportManager {
    /**
     * todo comments [lazyman]
     *
     * @param report
     * @param parentResult describes report which has to be created
     */
    void runReport(PrismObject<ReportType> report, PrismContainer<ReportParameterType> params, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException;

    /**
     * todo comments [lazyman]
     *
     * @param report
     * @param parentResult describes report which has to be created
     */
    void importReport(PrismObject<ReportType> report, PrismObject<ReportDataType> reportData, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException;

    /**
     * todo comments [lazyman]
     * todo how to return progress
     *
     * @param cleanupPolicy
     * @param parentResult
     */
    void cleanupReports(CleanupPolicyType cleanupPolicy, OperationResult parentResult);

    /**
     * todo comments [lazyman]
     *
     * @param reportDataOid
     * @param parentResult
     * @return
     */
    InputStream getReportDataStream(String reportDataOid, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, IOException;


    void deleteReportData(ReportDataType reportData, OperationResult parentResult) throws Exception;
}
