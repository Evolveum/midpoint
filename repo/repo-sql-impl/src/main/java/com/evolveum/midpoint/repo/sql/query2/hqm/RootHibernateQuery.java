/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query2.hqm;

import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
public class RootHibernateQuery extends HibernateQuery {

    private static final Trace LOGGER = TraceManager.getTrace(RootHibernateQuery.class);

    private Map<String,QueryParameterValue> parameters = new HashMap<>();
    private Integer maxResults;
    private Integer firstResult;
    private ResultTransformer resultTransformer;
    private boolean distinct;

    public RootHibernateQuery(JpaEntityDefinition primaryEntityDef) {
        super(primaryEntityDef);
    }

    private RootHibernateQuery(EntityReference primaryEntity) {
        super(primaryEntity);
    }

    public RootHibernateQuery createWrapperQuery() {
        return new RootHibernateQuery(getPrimaryEntity());
    }

    public String addParameter(String prefix, Object value, Type type) {
        String name = findFreeName(prefix);
        parameters.put(name, new QueryParameterValue(value, type));
        return name;
    }

    public String addParameter(String prefix, Object value) {
        return addParameter(prefix, value, null);
    }

    public void addParametersFrom(Map<String, QueryParameterValue> newParameters) {
        for (Map.Entry<String, QueryParameterValue> entry : newParameters.entrySet()) {
            if (parameters.containsKey(entry.getKey())) {
                throw new IllegalArgumentException("Parameter " + entry.getKey() + " already exists.");
            }
            parameters.put(entry.getKey(), entry.getValue());
        }
    }

    public Map<String, QueryParameterValue> getParameters() {
		return parameters;
	}

	private String findFreeName(String prefix) {
        int i = 1;
        for (;;) {
            String name = i == 1 ? prefix : prefix+i;
            if (!parameters.containsKey(name)) {
                return name;
            }
            i++;
        }
    }

    public Query getAsHqlQuery(Session session) {
        String text = getAsHqlText(0, distinct);
        LOGGER.trace("HQL text generated:\n{}", text);
        Query query = session.createQuery(text);
        for (Map.Entry<String,QueryParameterValue> parameter : parameters.entrySet()) {
            String name = parameter.getKey();
            QueryParameterValue parameterValue = parameter.getValue();
            LOGGER.trace("Parameter {} = {}", name, parameterValue.debugDump());

            if (parameterValue.getValue() instanceof Collection) {
                if (parameterValue.getType() != null) {
                    query.setParameterList(name, (Collection) parameterValue.getValue(), parameterValue.getType());
                } else {
                    query.setParameterList(name, (Collection) parameterValue.getValue());
                }
            } else {
                if (parameterValue.getType() != null) {
                    query.setParameter(name, parameterValue.getValue(), parameterValue.getType());
                } else {
                    query.setParameter(name, parameterValue.getValue());
                }
            }
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }
        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }
        if (resultTransformer != null) {
            query.setResultTransformer(resultTransformer);
        }
        return query;
    }

    @Override
    public RootHibernateQuery getRootQuery() {
        return this;
    }

    public void setMaxResults(Integer size) {
        this.maxResults = size;
    }

    public void setFirstResult(Integer offset) {
        this.firstResult = offset;
    }

    public void setResultTransformer(ResultTransformer resultTransformer) {
        this.resultTransformer = resultTransformer;
    }

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public Condition createIsNull(String propertyPath) {
        return new IsNullCondition(this, propertyPath);
    }

    public Condition createIsNotNull(String propertyPath) {
        return new IsNotNullCondition(this, propertyPath);
    }

    public Condition createEq(String propertyPath, Object value, boolean ignoreCase) {
        return createSimpleComparisonCondition(propertyPath, value, "=", ignoreCase);
    }

    public Condition createEq(String propertyPath, Object value) {
        return createEq(propertyPath, value, false);
    }

    public Condition createEqOrInOrNull(String propertyPath, Collection<?> values) {
        if (values.isEmpty()) {
        	return createIsNull(propertyPath);
		} else if (values.size() == 1) {
			return createEq(propertyPath, values.iterator().next(), false);
		} else {
        	return createIn(propertyPath, values);
		}
    }

    public Condition createSimpleComparisonCondition(String propertyPath, Object value, String comparatorSymbol) {
        return new SimpleComparisonCondition(this, propertyPath, value, comparatorSymbol, false);
    }

    public Condition createSimpleComparisonCondition(String propertyPath, Object value, String comparatorSymbol, boolean ignoreCase) {
        return new SimpleComparisonCondition(this, propertyPath, value, comparatorSymbol, ignoreCase);
    }

    public Condition createLike(String propertyPath, String value, MatchMode matchMode, boolean ignoreCase) {
        switch (matchMode) {
            case ANYWHERE: value = "%" + value + "%"; break;
            case START: value = value + "%"; break;
            case END: value = "%" + value; break;
            default: throw new IllegalStateException("Unsupported match mode: " + matchMode);
        }
        return new SimpleComparisonCondition(this, propertyPath, value, "like", ignoreCase);
    }

    public AndCondition createAnd(Condition... conditions) {
        return new AndCondition(this, conditions);
    }

    public Condition createAnd(List<Condition> conditions) {
        return new AndCondition(this, conditions);
    }

    public OrCondition createOr(Condition... conditions) {
        return new OrCondition(this, conditions);
    }

    public Condition createNot(Condition condition) {
        return new NotCondition(this, condition);
    }

    public Condition createIn(String propertyPath, Collection<?> values) {
        return new InCondition(this, propertyPath, values);
    }

    public Condition createIn(String propertyPath, String subqueryText) {
        return new InCondition(this, propertyPath, subqueryText);
    }

    public Condition createExists(String subqueryText, String linkingCondition) {
        return new ExistsCondition(this, subqueryText, linkingCondition);
    }

    public Condition createCompareXY(String leftSidePropertyPath, String rightSidePropertyPath, String operator, boolean ignoreCase) {
        return new PropertyPropertyComparisonCondition(this, leftSidePropertyPath, rightSidePropertyPath, operator, ignoreCase);
    }

    public Condition createFalse() {
        return new ConstantCondition(this, false);
    }
}
