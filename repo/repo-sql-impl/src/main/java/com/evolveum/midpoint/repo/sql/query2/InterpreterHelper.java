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
import com.evolveum.midpoint.repo.sql.query2.definition.AnyDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.CollectionSpecification;
import com.evolveum.midpoint.repo.sql.query2.definition.Definition;
import com.evolveum.midpoint.repo.sql.query2.definition.DefinitionPath;
import com.evolveum.midpoint.repo.sql.query2.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.PropertyDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.ReferenceDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.VirtualCollectionSpecification;
import com.evolveum.midpoint.repo.sql.query2.hqm.EntityReference;
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
    public String prepareJoins(ItemPath relativePath, String currentHqlPath, EntityDefinition baseEntityDefinition) throws QueryException {

        LOGGER.trace("Updating query context based on path {}", relativePath);

        /**
         * We have to do something like this - examples:
         * - activation.administrativeStatus -> (nothing, activation is embedded entity)
         * - assignment.targetRef -> "left join u.assignments a with ..."
         * - assignment.resourceRef -> "left join u.assignments a with ..."
         * - organization -> "left join u.organization o"
         *
         * Or more complex:
         * - assignment.modifyApproverRef -> "left join u.assignments a (...) left join a.modifyApproverRef m (...)"
         * - assignment.target.longs -> "left join u.assignments a (...), RObject o left join o.longs (...)"
         */
        DefinitionPath definitionPath = baseEntityDefinition.translatePath(relativePath);

        List<Definition> definitions = definitionPath.getDefinitions();
        for (int i = 0; i < definitions.size(); i++) {
            Definition definition = definitions.get(i);
            if (definition instanceof EntityDefinition) {
                EntityDefinition entityDef = (EntityDefinition) definition;
                if (!entityDef.isEmbedded() || entityDef.isCollection()) {
                    LOGGER.trace("Adding join for '{}' to context", entityDef.getJpaName());
                    currentHqlPath = addJoin(entityDef, currentHqlPath);
                } else {
                    currentHqlPath += "." + entityDef.getJpaName();
                }
            } else if (definition instanceof AnyDefinition) {
                if (definition.getJpaName() != null) {      // there are "invisible" Any definitions - object extension and shadow attributes
                    LOGGER.trace("Adding join for '{}' to context", definition.getJpaName());
                    currentHqlPath = addJoin(definition, currentHqlPath);
                }
                break;
            } else if (definition instanceof PropertyDefinition || definition instanceof ReferenceDefinition) {
                if (definition.isCollection()) {
                    LOGGER.trace("Adding join for '{}' to context", definition.getJpaName());
                    currentHqlPath = addJoin(definition, currentHqlPath);
                }
                break;      // quite redundant, as this is the last item in the chain
            } else {
                throw new QueryException("Not implemented yet: " + definition);
            }
            // TODO entity crossjoin references (when crossing object boundaries)
        }

        LOGGER.trace("prepareJoins({}) returning currentHqlPath of {}", relativePath, currentHqlPath);
        return currentHqlPath;
    }

    protected String addJoin(Definition joinedItemDefinition, String currentHqlPath) throws QueryException {
        RootHibernateQuery hibernateQuery = context.getHibernateQuery();
        EntityReference entityReference = hibernateQuery.getPrimaryEntity();                    // TODO other references (in the future)
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
        entityReference.addJoin(new JoinSpecification(joinedItemAlias, joinedItemFullPath, condition));
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
        EntityReference entityReference = hibernateQuery.getPrimaryEntity();                    // TODO other references (in the future)
        String joinedItemJpaName = anyAssociationName;
        String joinedItemFullPath = currentHqlPath + "." + joinedItemJpaName;
        String joinedItemAlias = hibernateQuery.createAlias(joinedItemJpaName);

        AndCondition conjunction = hibernateQuery.createAnd();
        if (ownerType != null) {        // null for assignment extensions
            conjunction.add(hibernateQuery.createEq(joinedItemAlias + ".ownerType", ownerType));
        }
        conjunction.add(hibernateQuery.createEq(joinedItemAlias + "." + RAnyValue.F_NAME, RUtil.qnameToString(itemName)));

        entityReference.addJoin(new JoinSpecification(joinedItemAlias, joinedItemFullPath, conjunction));
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
    public <T extends Definition> ProperDefinitionSearchResult<T> findProperDefinition(EntityDefinition baseEntityDefinition,
                                                                                       ItemPath path, Class<T> clazz)
            throws QueryException {
        QueryDefinitionRegistry2 registry = QueryDefinitionRegistry2.getInstance();
        ProperDefinitionSearchResult<T> candidateResult = null;

        for (EntityDefinition entityDefinition : findPossibleBaseEntities(baseEntityDefinition, registry)) {
            DefinitionSearchResult<T> result = entityDefinition.findDefinition(path, clazz);
            if (result != null) {
                if (candidateResult == null) {
                    candidateResult = new ProperDefinitionSearchResult<>(entityDefinition, result);
                } else {
                    // Check for compatibility. As entities are presented from the more abstract to less abstract,
                    // there is no possibility of false alarm.
                    if (!candidateResult.getEntityDefinition().isAssignableFrom(entityDefinition)) {
                        throw new QueryException("Unable to determine root entity for " + path + ": found incompatible candidates: " +
                                candidateResult.getEntityDefinition().getJpaName() + " and " +
                                entityDefinition.getJpaName());
                    }
                }
            }
        }
        LOGGER.trace("findProperDefinition: base={}, path={}, class={} -- returning {}",
                baseEntityDefinition.getShortInfo(), path, clazz.getSimpleName(), candidateResult);
        return candidateResult;
    }

    private List<EntityDefinition> findPossibleBaseEntities(EntityDefinition entityDefinition, QueryDefinitionRegistry2 registry) {
        List<EntityDefinition> retval = new ArrayList<>();
        retval.add(entityDefinition);               // (possibly) abstract one has to go first
        if (entityDefinition.isAbstract()) {        // just for efficiency
            retval.addAll(registry.getChildrenOf(entityDefinition));
        }
        return retval;
    }

    /**
     * Given existing entity definition and a request for narrowing it, tries to find refined definition.
     */
    public EntityDefinition findRestrictedEntityDefinition(EntityDefinition baseEntityDefinition, QName specificTypeName) throws QueryException {
        QueryDefinitionRegistry2 registry = QueryDefinitionRegistry2.getInstance();
        EntityDefinition specificEntityDefinition = registry.findEntityDefinition(specificTypeName);
        if (!baseEntityDefinition.isAssignableFrom(specificEntityDefinition)) {
            throw new QueryException("Entity " + baseEntityDefinition.getJpaName() + " cannot be restricted to " + specificEntityDefinition.getJpaName());
        }
        return specificEntityDefinition;
    }
}
