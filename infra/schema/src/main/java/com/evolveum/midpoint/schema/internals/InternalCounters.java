/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.schema.internals;

/**
 * @author semancik
 *
 */
public enum InternalCounters {
	RESOURCE_SCHEMA_PARSE_COUNT("resourceSchemaParseCount", "resource schema parse count", InternalOperationClasses.RESOURCE_SCHEMA_OPERATIONS),

	RESOURCE_SCHEMA_FETCH_COUNT("resourceSchemaFetchCount", "resource schema fetch count", InternalOperationClasses.REPOSITORY_OPERATIONS),
	
	RESOURCE_REPOSITORY_READ_COUNT("resourceRepositoryReadCount", "resource repository read count", null),

	CONNECTOR_INSTANCE_INITIALIZATION_COUNT("connectorInstanceInitializationCount", "connector instance initialization count", InternalOperationClasses.CONNECTOR_OPERATIONS),

	CONNECTOR_SCHEMA_PARSE_COUNT("connectorSchemaParseCount", "connector schema parse count", InternalOperationClasses.CONNECTOR_OPERATIONS),

	CONNECTOR_CAPABILITIES_FETCH_COUNT("connectorCapabilitiesFetchCount", "connector capabilities fetchCount", InternalOperationClasses.CONNECTOR_OPERATIONS),

	SCRIPT_COMPILE_COUNT("scriptCompileCount", "script compile count", null),

	SCRIPT_EXECUTION_COUNT("scriptExecutionCount", "script execution count", null),

	CONNECTOR_OPERATION_COUNT("connectorOperationCount", "connector operation count", InternalOperationClasses.CONNECTOR_OPERATIONS),
	
	CONNECTOR_MODIFICATION_COUNT("connectorModificationCount", "connector modification count", InternalOperationClasses.CONNECTOR_OPERATIONS),

	CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT("connectorSimulatedPagingSearchCount", "connector simulated paging search count", InternalOperationClasses.CONNECTOR_OPERATIONS),

	SHADOW_FETCH_OPERATION_COUNT("shadowFetchOperationCount", "shadow fetch operation count", InternalOperationClasses.SHADOW_FETCH_OPERATIONS),

	SHADOW_CHANGE_OPERATION_COUNT("shadowChangeOperationCount", "shadow change operation count", null),

	/**
	 * All provisioning operations that reach out to the resources.
	 */
	PROVISIONING_ALL_EXT_OPERATION_COUNT("provisioningAllExtOperationCount", "provisioning all ext operation count", null),

	REPOSITORY_READ_COUNT("repositoryReadCount", "repository read count", null),

	PRISM_OBJECT_COMPARE_COUNT("prismObjectCompareCount", "prism object compare count", null),

	PRISM_OBJECT_CLONE_COUNT("prismObjectCloneCount", "prism object clone count", null),

	ROLE_EVALUATION_COUNT("roleEvaluationCount", "role evaluation count", InternalOperationClasses.ROLE_EVALUATIONS),

	ROLE_EVALUATION_SKIP_COUNT("roleEvaluationSkipCount", "role evaluation skip count", null),

	PROJECTOR_RUN_COUNT("projectorRunCount", "projector run count", null);

	// Used as localization key
	private String key;

	// Used in logfiles, etc.
	private String label;

	private InternalOperationClasses operationClass;

	private InternalCounters(String key, String label, InternalOperationClasses operationClass) {
		this.key = key;
		this.label = label;
		this.operationClass = operationClass;
	}

	public String getKey() {
		return key;
	}

	public String getLabel() {
		return label;
	}

	public InternalOperationClasses getOperationClass() {
		return operationClass;
	}
}
