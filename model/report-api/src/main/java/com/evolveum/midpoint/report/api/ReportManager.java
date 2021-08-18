/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.api;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
            ConfigurationException, ExpressionEvaluationException, IOException, CommonException;


    void deleteReportData(ReportDataType reportData, OperationResult parentResult) throws Exception;

    CompiledObjectCollectionView createCompiledView(ObjectCollectionReportEngineConfigurationType collectionConfig, boolean useDefaultView, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException;

    Object evaluateScript(PrismObject<ReportType> report, ExpressionType expression, VariablesMap variables, String shortDesc, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException;

    VariablesMap evaluateSubreportParameters(PrismObject<ReportType> report, VariablesMap variables, Task task, OperationResult result);
}
