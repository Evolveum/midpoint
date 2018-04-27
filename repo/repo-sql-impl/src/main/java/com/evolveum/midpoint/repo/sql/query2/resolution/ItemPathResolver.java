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

package com.evolveum.midpoint.repo.sql.query2.resolution;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.data.common.any.RExtItem;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualQueryParam;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.QueryDefinitionRegistry2;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaAnyItemLinkDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaDataNodeDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaLinkDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.VirtualCollectionSpecification;
import com.evolveum.midpoint.repo.sql.query2.hqm.JoinSpecification;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.AndCondition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.ObjectUtils;

import javax.xml.namespace.QName;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for resolving item paths - i.e. translating them into JPA paths along with creation of necessary joins.
 * Contains also methods that try to find proper specific entity definition when only general one (e.g. RObject) is provided.
 *
 * @author mederly
 */
public class ItemPathResolver {

    private static final Trace LOGGER = TraceManager.getTrace(ItemPathResolver.class);

    private InterpretationContext context;

    public ItemPathResolver(InterpretationContext interpretationContext) {
        this.context = interpretationContext;
    }

    /**
     * Resolves item path by creating a sequence of resolution states and preparing joins that are used to access JPA properties.
     * @param itemDefinition Definition for the (final) item pointed to. Optional - necessary only for extension items.
     * @param singletonOnly Means no collections are allowed (used for right-side path resolution and order criteria).
     */
    public HqlDataInstance resolveItemPath(ItemPath relativePath, ItemDefinition itemDefinition,
                                           String currentHqlPath, JpaEntityDefinition baseEntityDefinition,
                                           boolean singletonOnly) throws QueryException {
        HqlDataInstance baseDataInstance = new HqlDataInstance(currentHqlPath, baseEntityDefinition, null);
        return resolveItemPath(relativePath, itemDefinition, baseDataInstance, singletonOnly);
    }

    public HqlDataInstance resolveItemPath(ItemPath relativePath, ItemDefinition itemDefinition,
                                           HqlDataInstance baseDataInstance,
                                           boolean singletonOnly) throws QueryException {

        ItemPathResolutionState currentState = new ItemPathResolutionState(relativePath, baseDataInstance, this);

        LOGGER.trace("Starting resolution and context update for item path '{}', singletonOnly='{}'", relativePath, singletonOnly);

        while (!currentState.isFinal()) {
            LOGGER.trace("Current resolution state:\n{}", currentState.debugDumpNoParent());
            currentState = currentState.nextState(itemDefinition, singletonOnly, context.getPrismContext());
        }

        LOGGER.trace("resolveItemPath({}) ending in resolution state of:\n{}", relativePath, currentState.debugDump());
        return currentState.getHqlDataInstance();
    }

    String addJoin(JpaLinkDefinition joinedItemDefinition, String currentHqlPath) throws QueryException {
        RootHibernateQuery hibernateQuery = context.getHibernateQuery();
        String joinedItemJpaName = joinedItemDefinition.getJpaName();
        String joinedItemFullPath = currentHqlPath + "." + joinedItemJpaName;
        String joinedItemAlias;
        if (!joinedItemDefinition.isMultivalued()) {
            /*
             * Let's check if we were already here i.e. if we had already created this join.
             * This is to avoid useless creation of redundant joins for singleton items.
             *
             * But how can we be sure that item is singleton if we look only at the last segment (which is single-valued)?
             * Imagine we are creating join for single-valued entity of u.abc.(...).xyz.ent where
             * ent is single-valued and not embedded (so we are to create something like "left join u.abc.(...).xyz.ent e").
             * Then "u" is certainly a single value: either the root object, or some value pointed to by Exists restriction.
             * Also, abc, ..., xyz are surely single-valued: otherwise they would be connected by a join. So,
             * u.abc.(...).xyz.ent is a singleton.
             *
             * Look at it in other way: if e.g. xyz was multivalued, then we would have something like:
             * left join u.abc.(...).uvw.xyz x
             * left join x.ent e
             * And, therefore we would not be looking for u.abc.(...).xyz.ent.
             */

            JoinSpecification existingJoin = hibernateQuery.getPrimaryEntity().findJoinFor(joinedItemFullPath);
            if (existingJoin != null) {
                // but let's check the condition as well
                String existingAlias = existingJoin.getAlias();
                // we have to create condition for existing alias, to be matched to existing condition
                Condition conditionForExistingAlias = createJoinCondition(existingAlias, joinedItemDefinition, hibernateQuery);
                if (ObjectUtils.equals(conditionForExistingAlias, existingJoin.getCondition())) {
                    LOGGER.trace("Reusing alias '{}' for joined path '{}'", existingAlias, joinedItemFullPath);
                    return existingAlias;
                }
            }
        }
        joinedItemAlias = hibernateQuery.createAlias(joinedItemDefinition);
        Condition condition = createJoinCondition(joinedItemAlias, joinedItemDefinition, hibernateQuery);
        hibernateQuery.getPrimaryEntity().addJoin(new JoinSpecification(joinedItemAlias, joinedItemFullPath, condition));
        return joinedItemAlias;
    }

    public String addTextInfoJoin(String currentHqlPath) throws QueryException {
        RootHibernateQuery hibernateQuery = context.getHibernateQuery();
        String joinedItemJpaName = RObject.F_TEXT_INFO_ITEMS;
        String joinedItemFullPath = currentHqlPath + "." + joinedItemJpaName;
        String joinedItemAlias = hibernateQuery.createAlias(joinedItemJpaName, false);
        hibernateQuery.getPrimaryEntity().addJoin(new JoinSpecification(joinedItemAlias, joinedItemFullPath, null));
        return joinedItemAlias;
    }

    private Condition createJoinCondition(String joinedItemAlias, JpaLinkDefinition joinedItemDefinition, RootHibernateQuery hibernateQuery) throws QueryException {
        Condition condition = null;
        if (joinedItemDefinition instanceof JpaAnyItemLinkDefinition) {
            JpaAnyItemLinkDefinition anyLinkDef = (JpaAnyItemLinkDefinition) joinedItemDefinition;
            AndCondition conjunction = hibernateQuery.createAnd();
            if (anyLinkDef.getOwnerType() != null) {        // null for assignment extensions
                conjunction.add(hibernateQuery.createEq(joinedItemAlias + ".ownerType", anyLinkDef.getOwnerType()));
            }
            RExtItem extItemDefinition = ExtItemDictionary.getInstance().findItemByDefinition(anyLinkDef.getItemDefinition(), context.getSession());
            if (extItemDefinition != null) {
                conjunction.add(hibernateQuery.createEq(joinedItemAlias + "." + RAnyValue.F_ITEM + "." + RExtItem.F_ID,
                        extItemDefinition.getId()));
            } else {
                // there are no rows referencing this item, because it does not exist in RExtItem (yet)
                conjunction.add(hibernateQuery.createFalse());
            }
            condition = conjunction;
        }
        else if (joinedItemDefinition.getCollectionSpecification() instanceof VirtualCollectionSpecification) {
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
        return condition;
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

    /**
     * Finds the proper definition for (possibly abstract) entity.
     * Returns the most abstract entity that can be used.
     * Checks for conflicts, such as user.locality vs org.locality.
     *
     * @param path Path to be found (non-empty!)
     * @param itemDefinition Definition of target property, required/used only for "any" properties
     * @param clazz Kind of definition to be looked for
     * @param prismContext
     * @return Entity type definition + item definition, or null if nothing was found
     */
    public <T extends JpaDataNodeDefinition>
    ProperDataSearchResult<T> findProperDataDefinition(JpaEntityDefinition baseEntityDefinition,
            ItemPath path, ItemDefinition itemDefinition,
            Class<T> clazz, PrismContext prismContext) throws QueryException {
        QueryDefinitionRegistry2 registry = QueryDefinitionRegistry2.getInstance();
        ProperDataSearchResult<T> candidateResult = null;

        for (JpaEntityDefinition entityDefinition : findPossibleBaseEntities(baseEntityDefinition, registry)) {
            DataSearchResult<T> result = entityDefinition.findDataNodeDefinition(path, itemDefinition, clazz, prismContext);
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
        LOGGER.trace("findProperDataDefinition: base='{}', path='{}', def='{}', class={} -- returning '{}'",
                baseEntityDefinition, path, itemDefinition, clazz.getSimpleName(), candidateResult);
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
