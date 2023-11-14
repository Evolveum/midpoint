/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.api;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
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
     * Creates and submits a simple (classic export) task that will execute the "export" report.
     * Requires {@link ModelAuthorizationAction#RUN_REPORT} authorization related to given report.
     *
     * @param report The report object; it must reside in repository. Actually, only its OID is used from the parameter.
     */
    void runReport(PrismObject<ReportType> report, PrismContainer<ReportParameterType> params, Task task, OperationResult result)
            throws CommonException;

    /**
     * Creates and submits a task that will execute the "import" report.
     * Requires {@link ModelAuthorizationAction#IMPORT_REPORT} authorization related to given report.
     *
     * @param report The report object; it must reside in repository. Actually, only its OID is used from the parameter.
     * @param reportData Data to be imported. It must reside in repository. Actually, only its OID is used from the parameter.
     */
    void importReport(PrismObject<ReportType> report, PrismObject<ReportDataType> reportData, Task task, OperationResult result)
            throws CommonException;

    /**
     * todo comments [lazyman]
     * todo how to return progress
     */
    void cleanupReports(CleanupPolicyType cleanupPolicy, RunningTask task, OperationResult parentResult);

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


    void deleteReportData(ReportDataType reportData, Task task, OperationResult parentResult) throws Exception;

    CompiledObjectCollectionView createCompiledView(ObjectCollectionReportEngineConfigurationType collectionConfig, boolean useDefaultView, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException;

    Object evaluateScript(PrismObject<ReportType> report, ExpressionType expression, VariablesMap variables, String shortDesc, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException;

    VariablesMap evaluateSubreportParameters(PrismObject<ReportType> report, VariablesMap variables, Task task, OperationResult result);
}
