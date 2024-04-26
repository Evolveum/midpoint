/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.query.hqm;

import static com.evolveum.midpoint.repo.sqlbase.SupportedDatabase.SQLSERVER;

import java.util.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.JpaLinkDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.*;
import com.evolveum.midpoint.repo.sql.query.restriction.MatchMode;
import com.evolveum.midpoint.repo.sqlbase.SupportedDatabase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Query in HQL that is being created.
 */
public class HibernateQuery {

    private static final Trace LOGGER = TraceManager.getTrace(HibernateQuery.class);
    private static final String INDENT_STRING = "  ";
    private static final int LIMIT = 100;

    /**
     * Primary entity for this query, along with joined entities.
     * For example,
     * RUser u
     * Or,
     * RUser u
     * left join u.assignments a with ...
     * <p>
     * (originally, we thought about cross-joins with other entities, hence "primary entity")
     */
    @NotNull private final EntityReference primaryEntity;

    // projection elements - i.e. select projectionElement1, projectionElement2, ..., projectionElementN from ...
    private final List<ProjectionElement> projectionElements = new ArrayList<>();
    /**
     * List of conditions in the "where" clause. They are to be interpreted as a conjunction.
     */
    private final List<Condition> conditions = new ArrayList<>();
    private final List<Ordering> orderingList = new ArrayList<>();

    private final Map<String, QueryParameterValue> parameters = new HashMap<>();

    private final SupportedDatabase databaseType;

    private final HibernateQuery parentQuery;

    private Integer maxResults;
    private Integer firstResult;
    private ResultTransformer resultTransformer;
    private boolean distinct;

    public HibernateQuery(JpaEntityDefinition primaryEntityDef, SupportedDatabase databaseType) {
        this(primaryEntityDef, databaseType, null);
    }

    public HibernateQuery(JpaEntityDefinition primaryEntityDef, SupportedDatabase databaseType, HibernateQuery parentQuery) {
        this.parentQuery = parentQuery;
        this.primaryEntity = createItemSpecification(primaryEntityDef);
        this.databaseType = databaseType;
    }

    public static void indent(StringBuilder sb, int indent) {
        while (indent-- > 0) {
            sb.append(INDENT_STRING);
        }
    }

    public String addParameter(String prefix, Object value, Type type) {
        if (parentQuery != null) {
            return parentQuery.addParameter(prefix, value, type);
        }

        String name = findFreeName(prefix);
        parameters.put(name, new QueryParameterValue(value, type));
        return name;
    }

    public String addParameter(String prefix, Object value) {
        // No need to delegate to parent, the following method does it too:
        return addParameter(prefix, value, null);
    }

    public void addParametersFrom(Map<String, QueryParameterValue> newParameters) {
        if (parentQuery != null) {
            parentQuery.addParametersFrom(newParameters);
            return;
        }

        for (Map.Entry<String, QueryParameterValue> entry : newParameters.entrySet()) {
            if (parameters.containsKey(entry.getKey())) {
                throw new IllegalArgumentException("Parameter " + entry.getKey() + " already exists.");
            }
            parameters.put(entry.getKey(), entry.getValue());
        }
    }

    public Map<String, QueryParameterValue> getParameters() {
        return parentQuery != null ? parentQuery.getParameters() : parameters;
    }

    private String findFreeName(String prefix) {
        int i = 1;
        for (; ; ) {
            String name = i == 1 ? prefix : prefix + i;
            if (!parameters.containsKey(name)) {
                return name;
            }
            i++;
        }
    }

    @SuppressWarnings("rawtypes")
    public Query getAsHqlQuery(EntityManager em) {
        if (parentQuery != null) {
            throw new IllegalStateException("Generating query from non-root query object!");
        }

        String text = getAsHqlText(0, distinct);
        LOGGER.trace("HQL text generated:\n{}", text);
        Query query = em.createQuery(text);
        for (Map.Entry<String, QueryParameterValue> parameter : parameters.entrySet()) {
            String name = parameter.getKey();
            QueryParameterValue parameterValue = parameter.getValue();
            LOGGER.trace("Parameter {} = {}", name, parameterValue.debugDump());

            query.setParameter(name, parameterValue.getValue());
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }
        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }
        if (resultTransformer != null) {
            //noinspection deprecation
            query.unwrap(org.hibernate.query.Query.class)
                    .setResultTransformer(resultTransformer);
        }
        return query;
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

    public boolean isDistinct() {
        return distinct;
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

    public static final char LIKE_ESCAPE_CHAR = '!';

    public Condition createLike(String propertyPath, String value, MatchMode matchMode, boolean ignoreCase) {
        // []^ to cover also SQL Server, see https://stackoverflow.com/q/712580/658826
        String charsToEscape = databaseType == SQLSERVER
                ? "_%[]^" + LIKE_ESCAPE_CHAR
                : "_%" + LIKE_ESCAPE_CHAR;
        if (StringUtils.containsAny(value, charsToEscape)) {
            value = escapeLikeValue(value, charsToEscape);
        }
        switch (matchMode) {
            case ANYWHERE:
                value = "%" + value + "%";
                break;
            case START:
                value = value + "%";
                break;
            case END:
                value = "%" + value;
                break;
            default:
                throw new IllegalStateException("Unsupported match mode: " + matchMode);
        }
        return new SimpleComparisonCondition(this, propertyPath, value, "like", ignoreCase);
    }

    private String escapeLikeValue(String value, String charsToEscape) {
        StringBuilder sb = new StringBuilder(value);
        for (int i = 0; i < sb.length(); i++) {
            if (charsToEscape.indexOf(sb.charAt(i)) == -1) {
                continue;
            }

            sb.insert(i, LIKE_ESCAPE_CHAR);
            i += 1;
        }
        return sb.toString();
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
        return new InlineExistsCondition(this, subqueryText, linkingCondition);
    }

    public Condition createCompareXY(String leftSidePropertyPath, String rightSidePropertyPath, String operator, boolean ignoreCase) {
        return new PropertyPropertyComparisonCondition(this, leftSidePropertyPath, rightSidePropertyPath, operator, ignoreCase);
    }

    public Condition createFalse() {
        return new ConstantCondition(this, false);
    }

    public boolean isDistinctNotNecessary() {
        return getPrimaryEntity().getJoins().isEmpty();
    }

    public void addProjectionElement(ProjectionElement element) {
        projectionElements.add(element);
    }

    public void addProjectionElementsFor(List<String> items) {
        for (String item : items) {
            addProjectionElement(new GenericProjectionElement(item));
        }
    }

    public @NotNull EntityReference getPrimaryEntity() {
        return primaryEntity;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void addCondition(Condition condition) {
        conditions.add(condition);
    }

    // Seems to have some side effects. Do not call twice!
    public String getAsHqlText(int indent, boolean distinct) {
        StringBuilder sb = new StringBuilder();

        dumpToHql(sb, indent, distinct);

        return sb.toString();
    }

    public void dumpToHql(StringBuilder sb, int indent, boolean distinct) {
        indent(sb, indent);
        sb.append("select");
        if (distinct) {
            sb.append(" distinct");
        }
        sb.append("\n");
        // we finish at the end of the last line (not at the new line)
        ProjectionElement.dumpToHql(sb, projectionElements, indent + 1);
        sb.append("\n");

        indent(sb, indent);
        sb.append("from\n");
        primaryEntity.dumpToHql(sb, indent + 1);

        if (!conditions.isEmpty()) {
            sb.append("\n");
            indent(sb, indent);
            sb.append("where\n");
            Condition.dumpToHql(sb, conditions, indent + 1);
        }
        if (!orderingList.isEmpty()) {
            sb.append("\n");
            indent(sb, indent);
            sb.append("order by ");
            boolean first = true;
            for (Ordering ordering : orderingList) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(ordering.byProperty);
                if (ordering.direction != null) {
                    switch (ordering.direction) {
                        case DESCENDING:
                            sb.append(" desc");
                            break;
                        case ASCENDING:
                            sb.append(" asc");
                            break;
                        default:
                            throw new IllegalStateException("Unknown ordering: " + ordering.direction);
                    }
                }
            }
        }
    }

    public EntityReference createItemSpecification(JpaEntityDefinition entityDef) {
        String alias = createAlias(entityDef);
        return new EntityReference(alias, entityDef.getJpaClassName());
    }

    public String createAlias(JpaEntityDefinition def) {
        return createAlias(def.getJpaClassName(), true);
    }

    public String createAlias(JpaLinkDefinition<?> linkDefinition) {
        Objects.requireNonNull(linkDefinition.getJpaName(), "Got unnamed transition");
        return createAlias(linkDefinition.getJpaName(), false);
    }

    public String createAlias(String name, boolean entity) {
        // Prefixing aliases with parent query info to keep them unique.
        // All aliases are prefixed with _ to avoid any accidental similarity to keywords like "or" for subqueries.
        String aliasPrefix = parentQuery != null ? parentQuery.getPrimaryEntityAlias() : "_";
        // We want to skip 'R' prefix for entity definition names.
        int prefixIndex = entity ? 1 : 0;
        String aliasBase = aliasPrefix + Character.toString(name.charAt(prefixIndex)).toLowerCase();

        int index = 2;
        String alias = aliasBase;
        while (hasAlias(alias)) {
            alias = aliasBase + index;
            index++;

            if (index > LIMIT) {
                throw new IllegalStateException("Alias index for '" + name
                        + "' is more than " + LIMIT + "? This probably should not happen.");
            }
        }

        return alias;
    }

    private boolean hasAlias(@NotNull String alias) {
        //noinspection ConstantConditions - happens when called from constructor
        if (primaryEntity == null) {
            return false;
        }
        return primaryEntity.containsAlias(alias);
    }

    public String getPrimaryEntityAlias() {
        return getPrimaryEntity().getAlias();
    }

    // use with care!
    public void setPrimaryEntityAlias(String alias) {
        getPrimaryEntity().setAlias(alias);
    }

    public void addOrdering(String propertyPath, OrderDirection direction) {
        orderingList.add(new Ordering(propertyPath, direction));
    }

    public List<Ordering> getOrderingList() {
        return orderingList;
    }

    /**
     * Creates subquery for the specified type.
     * The subquery is not inserted into the query yet.
     * Subquery has the same database type and all its aliases will be prefixed with parent's primary alias.
     */
    public HibernateQuery createSubquery(JpaEntityDefinition rootEntityDefinition) {
        return new HibernateQuery(rootEntityDefinition, databaseType, this);
    }

    public static class Ordering {
        @NotNull private final String byProperty;
        private final OrderDirection direction;

        Ordering(@NotNull String byProperty, OrderDirection direction) {
            this.byProperty = byProperty;
            this.direction = direction;
        }

        @NotNull
        public String getByProperty() {
            return byProperty;
        }

        public OrderDirection getDirection() {
            return direction;
        }
    }
}
