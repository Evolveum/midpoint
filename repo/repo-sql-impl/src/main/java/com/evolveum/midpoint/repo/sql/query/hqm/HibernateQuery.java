/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.JpaLinkDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;

/**
 * Query in HQL that is being created.
 *
 * @author mederly
 */
public abstract class HibernateQuery {

    private static final String INDENT_STRING = "  ";

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

    private final List<Grouping> groupingList = new ArrayList<>();

    public HibernateQuery(@NotNull JpaEntityDefinition primaryEntityDef) {
        primaryEntity = createItemSpecification(primaryEntityDef);
    }

    protected HibernateQuery(@NotNull EntityReference primaryEntity) {
        this.primaryEntity = primaryEntity;
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
        Objects.requireNonNull(linkDefinition.getJpaName(), "Got unnamed transition");
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
            alias = prefix + index;
            index++;

            if (index > LIMIT) {
                throw new IllegalStateException("Alias index for '" + name
                        + "' is more than " + LIMIT + "? This probably should not happen.");
            }
        }

        return alias;
    }

    private boolean hasAlias(String alias) {
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

    public void addGrouping(String propertyPath) {
        groupingList.add(new Grouping(propertyPath));
    }

    public List<Grouping> getGroupingList() {
        return groupingList;
    }

    public abstract RootHibernateQuery getRootQuery();
}
