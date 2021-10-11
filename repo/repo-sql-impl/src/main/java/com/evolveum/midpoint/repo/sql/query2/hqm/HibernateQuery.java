/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query2.hqm;

import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaLinkDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Query in HQL that is being created.
 *
 * @author mederly
 */
public abstract class HibernateQuery {

    private static final String INDENT_STRING = "  ";

    // projection elements - i.e. select projectionElement1, projectionElement2, ..., projectionElementN from ...
    private List<ProjectionElement> projectionElements = new ArrayList<>();

    /**
     * Primary entity for this query, along with joined entities.
     * For example,
     *   RUser u
     * Or,
     *   RUser u
     *     left join u.assignments a with ...
     *
     * (originally, we thought about cross-joins with other entities, hence "primary entity")
     */
    private EntityReference primaryEntity;          // not null

    /**
     * List of conditions in the "where" clause. They are to be interpreted as a conjunction.
     */
    private List<Condition> conditions = new ArrayList<>();

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

    private List<Ordering> orderingList = new ArrayList<>();


    public static class Grouping {
        @NotNull private final String byProperty;

        Grouping(@NotNull String byProperty) {
            this.byProperty = byProperty;
        }

        @NotNull
        public String getByProperty() {
            return byProperty;
        }
    }

    private List<Grouping> groupingList = new ArrayList<>();


    public HibernateQuery(@NotNull JpaEntityDefinition primaryEntityDef) {
        primaryEntity = createItemSpecification(primaryEntityDef);
    }

    protected HibernateQuery(EntityReference primaryEntity) {
        this.primaryEntity = primaryEntity;
    }

    public List<ProjectionElement> getProjectionElements() {
        return projectionElements;
    }

    public void addProjectionElement(ProjectionElement element) {
        projectionElements.add(element);
    }

    public void addProjectionElementsFor(List<String> items) {
        for (String item : items) {
            addProjectionElement(new GenericProjectionElement(item));
        }
    }

    public EntityReference getPrimaryEntity() {
        return primaryEntity;
    }

    public void setPrimaryEntity(EntityReference primaryEntity) {
        this.primaryEntity = primaryEntity;
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

        indent(sb, indent);
        sb.append("select");
        if (distinct) {
            sb.append(" distinct");
        }
        sb.append("\n");
        ProjectionElement.dumpToHql(sb, projectionElements, indent + 1);     // we finish at the end of the last line (not at the new line)
        sb.append("\n");

        indent(sb, indent);
        sb.append("from\n");
        primaryEntity.dumpToHql(sb, indent + 1);

        if (!conditions.isEmpty()) {
            sb.append("\n");
            indent(sb, indent);
            sb.append("where\n");
            Condition.dumpToHql(sb, conditions, indent+1);
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
                        case DESCENDING: sb.append(" desc"); break;
                        case ASCENDING: sb.append(" asc"); break;
                        default: throw new IllegalStateException("Unknown ordering: " + ordering.direction);
                    }
                }
            }
        }

        if (!groupingList.isEmpty()) {
            sb.append("\n");
            indent(sb, indent);
            sb.append("group by ");
            boolean first = true;
            for (Grouping grouping : groupingList) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(grouping.byProperty);
            }
        }

        return sb.toString();
    }

    public static void indent(StringBuilder sb, int indent) {
        while (indent-- > 0) {
            sb.append(INDENT_STRING);
        }
    }

    public EntityReference createItemSpecification(JpaEntityDefinition entityDef) {
        String alias = createAlias(entityDef);
        return new EntityReference(alias, entityDef.getJpaClassName());
    }

    public String createAlias(JpaEntityDefinition def) {
        return createAlias(def.getJpaClassName(), true);
    }

    public String createAlias(JpaLinkDefinition linkDefinition) {
        Validate.notNull(linkDefinition.getJpaName(), "Got unnamed transition");
        return createAlias(linkDefinition.getJpaName(), false);
    }

    private static final int LIMIT = 100;

    public String createAlias(String name, boolean entity) {
        String prefix;

        //we want to skip 'R' prefix for entity definition names (a bit of hack)
        int prefixIndex = entity ? 1 : 0;
        prefix = Character.toString(name.charAt(prefixIndex)).toLowerCase();

        int index = 2;
        String alias = prefix;
        while (hasAlias(alias)) {
            alias = prefix + Integer.toString(index);
            index++;

            if (index > LIMIT) {
                throw new IllegalStateException("Alias index for '" + name
                        + "' is more than " + LIMIT + "? This probably should not happen.");
            }
        }

        return alias;
    }

    private boolean hasAlias(String alias) {
        if (primaryEntity != null && primaryEntity.containsAlias(alias)) {
            return true;
        }
        return false;
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

    public void addGrouping(String propertyPath) {
        groupingList.add(new Grouping(propertyPath));
    }

    public List<Grouping> getGroupingList() {
        return groupingList;
    }


    public abstract RootHibernateQuery getRootQuery();

    // used to narrow the primary entity e.g. from RObject to RUser (e.g. during ItemValueRestriction processing)
    public void narrowPrimaryEntity(JpaEntityDefinition newDefinition) throws QueryException {
        String oldEntityName = getPrimaryEntity().getName();
        Class<? extends RObject> oldEntityClass = ClassMapper.getHqlClassForHqlName(oldEntityName);
        Class<? extends RObject> newEntityClass = newDefinition.getJpaClass();
        if (!(oldEntityClass.isAssignableFrom(newEntityClass))) {
            throw new QueryException("Cannot narrow primary entity definition from " + oldEntityClass + " to " + newEntityClass);
        }
        getPrimaryEntity().setName(newDefinition.getJpaClassName());     // alias stays the same
    }
}
