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

package com.evolveum.midpoint.repo.sql.query2;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualQueryParam;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaAnyDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaPropertyDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaReferenceDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaDataNodeDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaLinkDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.VirtualCollectionSpecification;
import com.evolveum.midpoint.repo.sql.query2.hqm.JoinSpecification;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.AndCondition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.xml.namespace.QName;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Various generally useful methods used within the interpretation algorithm.
 *
 * Note that this helper is context-specific. (Probably its name should reflect that.)
 *
 * @author mederly
 */
public class InterpreterHelper {

    private static final Trace LOGGER = TraceManager.getTrace(InterpreterHelper.class);

    private InterpretationContext context;

    public InterpreterHelper(InterpretationContext interpretationContext) {
        this.context = interpretationContext;
    }

    // returns property path that can be used to access the item values
    public String prepareJoins(ItemPath relativePath, String currentHqlPath, JpaEntityDefinition baseEntityDefinition) throws QueryException {

        LOGGER.trace("Updating query context based on path '{}'", relativePath);

        /**
         * We have to do something like this - examples:
         * - activation.administrativeStatus -> (nothing, activation is embedded entity)
         * - assignment.targetRef -> "left join u.assignments a with ..."
         * - assignment.resourceRef -> "left join u.assignments a with ..."
         * - organization -> "left join u.organization o"
         *
         * Or more complex:
         * - assignment.modifyApproverRef -> "left join u.assignments a (...) left join a.modifyApproverRef m (...)"
         * - assignment.target.longs -> "left join u.assignments a (...) left join a.target t left join t.longs (...)"
         */

        JpaDataNodeDefinition currentDefinition = baseEntityDefinition;
        ItemPath itemPathRemainder = relativePath;
        while (!ItemPath.isNullOrEmpty(itemPathRemainder)) {

            LOGGER.trace("currentDefinition = '{}', current HQL path = '{}', itemPathRemainder = '{}'", currentDefinition, currentHqlPath, itemPathRemainder);
            DataSearchResult<JpaDataNodeDefinition> result = currentDefinition.nextLinkDefinition(itemPathRemainder);
            LOGGER.trace("nextLinkDefinition on '{}' returned '{}'", itemPathRemainder, result != null ? result.getLinkDefinition() : "(null)");
            if (result == null) {       // sorry we failed (however, this should be caught before -> so IllegalStateException)
                throw new IllegalStateException("Couldn't find " + itemPathRemainder + " in " + currentDefinition);
            }
            JpaLinkDefinition linkDefinition = result.getLinkDefinition();
            JpaDataNodeDefinition nextNodeDefinition = linkDefinition.getTargetDefinition();

            if (nextNodeDefinition instanceof JpaAnyDefinition) {
                JpaAnyDefinition anyDefinition = (JpaAnyDefinition) nextNodeDefinition;
                if (linkDefinition.getJpaName() != null) {      // there are "invisible" Any definitions - object extension and shadow attributes
                    currentHqlPath = addJoin(linkDefinition, currentHqlPath);
                    LOGGER.trace("Adding join for '{}' to context", anyDefinition);
                }
                break;      // we're done
            } else if (nextNodeDefinition instanceof JpaEntityDefinition) {
                if (!linkDefinition.isEmbedded() || linkDefinition.isMultivalued()) {
                    LOGGER.trace("Adding join for '{}' to context", linkDefinition);
                    currentHqlPath = addJoin(linkDefinition, currentHqlPath);
                } else {
                    currentHqlPath += "." + linkDefinition.getJpaName();
                }
            } else if (nextNodeDefinition instanceof JpaPropertyDefinition || nextNodeDefinition instanceof JpaReferenceDefinition) {
                if (linkDefinition.isMultivalued()) {
                    LOGGER.trace("Adding join for '{}' to context", linkDefinition);
                    currentHqlPath = addJoin(linkDefinition, currentHqlPath);
                } else {
                    currentHqlPath += "." + linkDefinition.getJpaName();
                }
            } else {
                throw new QueryException("Not implemented yet: " + linkDefinition);
            }
            itemPathRemainder = result.getRemainder();
            currentDefinition = nextNodeDefinition;
        }

        LOGGER.trace("prepareJoins({}) returning currentHqlPath of '{}'", relativePath, currentHqlPath);
        return currentHqlPath;
    }

    private String addJoin(JpaLinkDefinition joinedItemDefinition, String currentHqlPath) throws QueryException {
        RootHibernateQuery hibernateQuery = context.getHibernateQuery();
        String joinedItemJpaName = joinedItemDefinition.getJpaName();
        String joinedItemFullPath = currentHqlPath + "." + joinedItemJpaName;
        String joinedItemAlias = hibernateQuery.createAlias(joinedItemDefinition);
        Condition condition = null;
        if (joinedItemDefinition.getCollectionSpecification() instanceof VirtualCollectionSpecification) {
            VirtualCollectionSpecification vcd = (VirtualCollectionSpecification) joinedItemDefinition.getCollectionSpecification();
            List<Condition> conditions = new ArrayList<>(vcd.getAdditionalParams().length);
            for (VirtualQueryParam vqp : vcd.getAdditionalParams()) {
                // e.g. name = "assignmentOwner", type = RAssignmentOwner.class, value = "ABSTRACT_ROLE"
                Object value = createQueryParamValue(vqp);
                Condition c = hibernateQuery.createEq(joinedItemAlias + "." + vqp.name(), value);
                conditions.add(c);
            }
            if (conditions.size() > 1) {
                condition = hibernateQuery.createAnd(conditions);
            } else if (conditions.size() == 1) {
                condition = conditions.iterator().next();
            }
        }
        hibernateQuery.getPrimaryEntity().addJoin(new JoinSpecification(joinedItemAlias, joinedItemFullPath, condition));
        return joinedItemAlias;
    }

    /**
     * This method provides transformation from {@link String} value defined in
     * {@link com.evolveum.midpoint.repo.sql.query.definition.VirtualQueryParam#value()} to real object. Currently only
     * to simple types and enum values.
     */
    private Object createQueryParamValue(VirtualQueryParam param) throws QueryException {
        Class type = param.type();
        String value = param.value();

        try {
            if (type.isPrimitive()) {
                return type.getMethod("valueOf", new Class[]{String.class}).invoke(null, new Object[]{value});
            }

            if (type.isEnum()) {
                return Enum.valueOf(type, value);
            }
        } catch (NoSuchMethodException|IllegalAccessException|InvocationTargetException |RuntimeException ex) {
            throw new QueryException("Couldn't transform virtual query parameter '"
                    + param.name() + "' from String to '" + type + "', reason: " + ex.getMessage(), ex);
        }

        throw new QueryException("Couldn't transform virtual query parameter '"
                + param.name() + "' from String to '" + type + "', it's not yet implemented.");
    }


    public String addJoinAny(String currentHqlPath, String anyAssociationName, QName itemName, RObjectExtensionType ownerType) {
        RootHibernateQuery hibernateQuery = context.getHibernateQuery();
        String joinedItemJpaName = anyAssociationName;
        String joinedItemFullPath = currentHqlPath + "." + joinedItemJpaName;
        String joinedItemAlias = hibernateQuery.createAlias(joinedItemJpaName, false);

        AndCondition conjunction = hibernateQuery.createAnd();
        if (ownerType != null) {        // null for assignment extensions
            conjunction.add(hibernateQuery.createEq(joinedItemAlias + ".ownerType", ownerType));
        }
        conjunction.add(hibernateQuery.createEq(joinedItemAlias + "." + RAnyValue.F_NAME, RUtil.qnameToString(itemName)));

        hibernateQuery.getPrimaryEntity().addJoin(new JoinSpecification(joinedItemAlias, joinedItemFullPath, conjunction));
        return joinedItemAlias;
    }

    /**
     * Finds the proper definition for (possibly abstract) entity.
     * Returns the most abstract entity that can be used.
     * Checks for conflicts, such as user.locality vs org.locality.
     *
     * @param path Path to be found
     * @param clazz Kind of definition to be looked for
     * @return Entity type definition + item definition, or null if nothing was found
     */
    public <T extends JpaDataNodeDefinition>
    ProperDataSearchResult<T> findProperDataDefinition(JpaEntityDefinition baseEntityDefinition,
                                                       ItemPath path, Class<T> clazz) throws QueryException {
        QueryDefinitionRegistry2 registry = QueryDefinitionRegistry2.getInstance();
        ProperDataSearchResult<T> candidateResult = null;

        for (JpaEntityDefinition entityDefinition : findPossibleBaseEntities(baseEntityDefinition, registry)) {
            DataSearchResult<T> result = entityDefinition.findDataNodeDefinition(path, clazz);
            if (result != null) {
                if (candidateResult == null) {
                    candidateResult = new ProperDataSearchResult<>(entityDefinition, result);
                } else {
                    // Check for compatibility. As entities are presented from the more abstract to less abstract,
                    // there is no possibility of false alarm.
                    if (!candidateResult.getEntityDefinition().isAssignableFrom(entityDefinition)) {
                        throw new QueryException("Unable to determine root entity for " + path + ": found incompatible candidates: " +
                                candidateResult.getEntityDefinition() + " and " +
                                entityDefinition);
                    }
                }
            }
        }
        LOGGER.trace("findProperDataDefinition: base='{}', path='{}', class={} -- returning '{}'",
                baseEntityDefinition, path, clazz.getSimpleName(), candidateResult);
        return candidateResult;
    }

    private List<JpaEntityDefinition> findPossibleBaseEntities(JpaEntityDefinition entityDefinition, QueryDefinitionRegistry2 registry) {
        List<JpaEntityDefinition> retval = new ArrayList<>();
        retval.add(entityDefinition);               // (possibly) abstract one has to go first
        if (entityDefinition.isAbstract()) {        // just for efficiency
            retval.addAll(registry.getChildrenOf(entityDefinition));
        }
        return retval;
    }

    /**
     * Given existing entity definition and a request for narrowing it, tries to find refined definition.
     */
    public JpaEntityDefinition findRestrictedEntityDefinition(JpaEntityDefinition baseEntityDefinition, QName specificTypeName) throws QueryException {
        QueryDefinitionRegistry2 registry = QueryDefinitionRegistry2.getInstance();
        JpaEntityDefinition specificEntityDefinition = registry.findEntityDefinition(specificTypeName);
        if (!baseEntityDefinition.isAssignableFrom(specificEntityDefinition)) {
            throw new QueryException("Entity " + baseEntityDefinition + " cannot be restricted to " + specificEntityDefinition);
        }
        return specificEntityDefinition;
    }
}
