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

package com.evolveum.midpoint.schema;

import org.apache.commons.lang.Validate;

import java.util.List;
import java.util.Map;

/**
 * Response from the "diagnose query" operation.
 *
 * EXPERIMENTAL, will probably change
 *
 * @author mederly
 */
public class RepositoryQueryDiagResponse {

	public static class ParameterValue {
		public final Object value;
		public final String displayValue;

		public ParameterValue(Object value, String displayValue) {
			this.value = value;
			this.displayValue = displayValue;
		}
	}

	private final List<?> queryResult;			// contains either list of prism objects (in case of midPoint query)
												// or a list of lower-level, e.g. java objects (in case of implementation-level query)

	private final Object implementationLevelQuery;
	private final Map<String,ParameterValue> implementationLevelQueryParameters;		// values are non-null

	public RepositoryQueryDiagResponse(List<?> queryResult, Object implementationLevelQuery, Map<String, ParameterValue> implementationLevelQueryParameters) {
		if (implementationLevelQuery != null) {
			Validate.notNull(implementationLevelQueryParameters);
		}
		this.queryResult = queryResult;
		this.implementationLevelQuery = implementationLevelQuery;
		this.implementationLevelQueryParameters = implementationLevelQueryParameters;
	}

	public List<?> getQueryResult() {
		return queryResult;
	}

	public Object getImplementationLevelQuery() {
		return implementationLevelQuery;
	}

	public Map<String, ParameterValue> getImplementationLevelQueryParameters() {
		return implementationLevelQueryParameters;
	}
}
