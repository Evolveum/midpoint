/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.util.annotation.Experimental;

import java.util.List;
import java.util.Map;

/**
 * Response from the "diagnose query" operation.
 *
 * EXPERIMENTAL, will probably change
 *
 * @author mederly
 */
@Experimental
public class RepositoryQueryDiagResponse {

    public static class ParameterValue {
        public final Object value;
        public final String displayValue;

        public ParameterValue(Object value, String displayValue) {
            this.value = value;
            this.displayValue = displayValue;
        }
    }

    private final List<?> queryResult;            // contains either list of prism objects (in case of midPoint query)
                                                // or a list of lower-level, e.g. java objects (in case of implementation-level query)

    private final Object implementationLevelQuery;
    private final Map<String,ParameterValue> implementationLevelQueryParameters;        // values are non-null

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
