/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import java.util.Collection;

public interface ReportService {

    PrismObject<ReportType> getReportDefinition(String reportOid, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

    Object evaluateScript(PrismObject<ReportType> report, ExpressionType expression, VariablesMap variables, String shortDesc, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException;

    /**
     * Result "full information", i.e. a collection of non-negative prism values stemming from the expression evaluation.
     *
     * TODO better name
     */
    Collection<? extends PrismValue> evaluateToCollection(PrismObject<ReportType> report, ExpressionType expression, VariablesMap variables, String shortDesc, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException;

    boolean isAuthorizedToRunReport(PrismObject<ReportType> report, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    boolean isAuthorizedToImportReport(PrismObject<ReportType> report, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    @Experimental
    VariablesMap getParameters(Task task);
}
