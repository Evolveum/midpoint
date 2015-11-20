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

import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.repo.sql.query2.definition.Definition;
import com.evolveum.midpoint.repo.sql.query2.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import org.hibernate.Query;
import org.hibernate.transform.ResultTransformer;

import java.util.ArrayList;
import java.util.List;

/**
 * Query in HQL that is being created.
 *
 * @author mederly
 */
public class HibernateQuery {

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
     */
    private EntityReference primaryEntity;

    /**
     * All other entities for this query, again along with joined entities.
     * For example, if we want to look for a name of assigned entity, i.e.
     * UserType: Equal (assignment/target/name, 'Role1'), primary entity is:
     *   RUser u
     *     left join u.assignments a with ...
     * and secondary one is:
     *   RObject o
     * (with associated condition of o.oid = a.targetRef.targetOid)
     */
    private List<EntityReference> otherEntities = new ArrayList<>();

    /**
     * List of conditions in the "where" clause. They are to be interpreted as a conjunction.
     */
    private List<Condition> conditions = new ArrayList<>();

    public HibernateQuery(EntityDefinition primaryEntityDef) {
        EntityReference primaryEntity = createItemSpecification(primaryEntityDef);
        setPrimaryEntity(primaryEntity);
    }

    public List<ProjectionElement> getProjectionElements() {
        return projectionElements;
    }

    public void addProjectionElement(ProjectionElement element) {
        projectionElements.add(element);
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

    public Query getAsHql() {
        StringBuilder sb = new StringBuilder();

        sb.append("select\n");
        ProjectionElement.dumpToHql(sb, projectionElements, 1);     // we finish at the end of the last line (not at the new line)
        sb.append("\n");

        sb.append("from\n");
        primaryEntity.dumpToHql(sb, 1);
        for (EntityReference otherEntity : otherEntities) {
            sb.append(",\n");
            otherEntity.dumpToHql(sb, 1);
        }
        sb.append("\n");

        if (!conditions.isEmpty()) {
            sb.append("where\n");
            Condition.dumpToHql(sb, conditions, 1);
        }
        return sb.toString();
    }

    public static void indent(StringBuilder sb, int indent) {
        while (indent-- > 0) {
            sb.append(INDENT_STRING);
        }
    }

    public EntityReference createItemSpecification(EntityDefinition entityDef) {
        String alias = createAlias(entityDef);
        return new EntityReference(alias, entityDef.getJpaName());
    }

    public String createAlias(Definition def) {
        return createAlias(def.getJpaName(), def instanceof EntityDefinition);
    }

    private static final int LIMIT = 100;

    public String createAlias(String name, boolean isEntity) {
        String prefix;

        //we want to skip 'R' prefix for entity definition names
        int prefixIndex = isEntity ? 1 : 0;
        prefix = Character.toString(name.charAt(prefixIndex)).toLowerCase();

        int index = 1;
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
        if (primaryEntity.containsAlias(alias)) {
            return true;
        }
        for (EntityReference other : otherEntities) {
            if (other.containsAlias(alias)) {
                return true;
            }
        }
        return false;
    }

    public String getPrimaryEntityAlias() {
        return getPrimaryEntity().getAlias();
    }

    public void addOrder(String propertyPath, OrderDirection direction) {
        // TODO
    }

    public void setMaxResults(Integer size) {

    }

    public void setFirstResult(Integer offset) {

    }

    public void setResultTransformer(ResultTransformer resultTransformer) {

    }
}
