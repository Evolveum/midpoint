/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.api;

import java.io.IOException;
import java.util.Collection;

import com.evolveum.midpoint.schema.ProvisioningDiag;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.RepositoryQueryDiagRequest;
import com.evolveum.midpoint.schema.RepositoryQueryDiagResponse;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LogFileContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingEvaluationRequestType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingEvaluationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * A service provided by the IDM Model focused on system diagnostic. It allows to retrieve diagnostic data
 * that are not exactly part of system configuration (such as repository configuration). It can also be used
 * to initiate self-tests and similar diagnostic routines.
 *
 * UNSTABLE: This is likely to change
 * PRIVATE: This interface is not supposed to be used outside of midPoint
 *
 * @author Radovan Semancik
 *
 */
public interface ModelDiagnosticService {

	String CLASS_NAME_WITH_DOT = ModelDiagnosticService.class.getName() + ".";
	String REPOSITORY_SELF_TEST = CLASS_NAME_WITH_DOT + "repositorySelfTest";
    String REPOSITORY_TEST_ORG_CLOSURE_CONSISTENCY = CLASS_NAME_WITH_DOT + "repositoryTestOrgClosureConsistency";
	String EXECUTE_REPOSITORY_QUERY = CLASS_NAME_WITH_DOT + "executeRepositoryQuery";
	String EVALUATE_MAPPING = CLASS_NAME_WITH_DOT + "evaluateMapping";
	String PROVISIONING_SELF_TEST = CLASS_NAME_WITH_DOT + "provisioningSelfTest";
	String GET_LOG_FILE_CONTENT = CLASS_NAME_WITH_DOT + "getLogFileContent";
	String GET_LOG_FILE_SIZE = CLASS_NAME_WITH_DOT + "getLogFileSize";

	/**
	 * Provide repository run-time configuration and diagnostic information.
	 */
	RepositoryDiag getRepositoryDiag(Task task, OperationResult parentResult);

	/**
	 * Runs a short, non-destructive repository self test.
	 * This methods should never throw a (checked) exception. All the results
	 * should be in the returned result structure (including fatal errors).
	 */
	OperationResult repositorySelfTest(Task task);

    /**
     * Checks a org closure table for consistency, repairing any problems found.
     * This methods should never throw a (checked) exception. All the results
     * should be in the returned result structure (including fatal errors).
     *
     * The current implementation expects closure to be of reasonable size - so
     * it could be fetched into main memory as well as recomputed online
     * (perhaps up to ~250K entries). In future, this method will be reimplemented.
     *
     * BEWARE, this method locks out the M_ORG_CLOSURE table, so org-related operations
     * would wait until it completes.
     *
     * TODO this method is SQL service specific; it should be generalized/fixed somehow.
     */
	void repositoryTestOrgClosureConsistency(Task task, boolean repairIfNecessary, OperationResult result) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException;

    /**
	 * Runs a short, non-destructive internal provisioning test. It tests provisioning framework and
	 * general setup. Use ModelService.testResource for testing individual resource configurations.
	 */
	OperationResult provisioningSelfTest(Task task);

    /**
     * Provide provisioning run-time configuration and diagnostic information.
     */
	ProvisioningDiag getProvisioningDiag(Task task, OperationResult parentResult);

	/**
	 * Execute arbitrary implementation-specific query. In current implementation this means hibernate query.
	 *
	 * EXPERIMENTAL.
	 */
	RepositoryQueryDiagResponse executeRepositoryQuery(RepositoryQueryDiagRequest request, Task task, OperationResult parentResult)
			throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException;

	/**
	 * Execute arbitrary mapping.
	 *
	 * EXPERIMENTAL 
	 */
	MappingEvaluationResponseType evaluateMapping(MappingEvaluationRequestType request, Task task, OperationResult parentResult)
			throws SchemaException, SecurityViolationException, ExpressionEvaluationException,
			ObjectNotFoundException, CommunicationException, ConfigurationException;
	/**
	 * Exports data model
	 *
	 * EXPERIMENTAL. (TODO find a better place)
	 */
	String exportDataModel(Collection<String> resourceOids, DataModelVisualizer.Target target,
			Task task, OperationResult parentResult)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException;

	String exportDataModel(ResourceType resource, DataModelVisualizer.Target target, Task task,
			OperationResult parentResult)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException;

	// EXPERIMENTAL

	/**
	 * Returns the contents of the log file.
	 *
	 * @param fromPosition From absolute log file position (if non-negative); or counted from the end (if negative).
	 * @param maxSize Max number of bytes to return.
	 * @param task
	 * @param parentResult
	 */
	LogFileContentType getLogFileContent(Long fromPosition, Long maxSize, Task task, OperationResult parentResult)
			throws SecurityViolationException, IOException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException;

	long getLogFileSize(Task task, OperationResult parentResult) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException;
}
