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
public enum InternalOperationClasses {
	RESOURCE_SCHEMA_OPERATIONS("resourceSchemaOperations", "resource schema operations"),
	
	CONNECTOR_OPERATIONS("connectorOperations", "connector operations"),
	
	SHADOW_FETCH_OPERATIONS("shadowFetchOperations", "shadow fetch operations"),
	
	REPOSITORY_OPERATIONS("repositoryOperations", "repository operations"),
	
	PRISM_OBJECT_CLONES("prismObjectClone", "prism object clones"),
	
	ROLE_EVALUATIONS("roleEvaluations", "role evaluations");
		
	// Used as localization key
	private String key;
	
	// Used in logfiles, etc.
	private String label;
	
	private InternalOperationClasses(String key, String label) {
		this.key = key;
		this.label = label;
	}

	public String getKey() {
		return key;
	}

	public String getLabel() {
		return label;
	}
}
