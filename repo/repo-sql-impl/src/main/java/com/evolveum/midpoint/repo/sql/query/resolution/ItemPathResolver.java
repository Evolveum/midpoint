/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.query.resolution;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.data.common.any.RExtItem;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.query.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.QueryDefinitionRegistry;
import com.evolveum.midpoint.repo.sql.query.definition.*;
import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query.hqm.JoinSpecification;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.AndCondition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Responsible for resolving item paths - i.e. translating them into JPA paths along with creation of necessary joins.
 * Contains also methods that try to find proper specific entity definition when only general one (e.g. RObject) is provided.
 */
public class ItemPathResolver {

    private static final Trace LOGGER = TraceManager.getTrace(ItemPathResolver.class);

    private final InterpretationContext context;

    public ItemPathResolver(InterpretationContext interpretationContext) {
        this.context = interpretationContext;
    }

    /**
     * Resolves item path by creating a sequence of resolution states and preparing joins that are used to access JPA properties.
     *
     * @param itemDefinition Definition for the (final) item pointed to. Optional - necessary only for extension items.
     * @param reuseMultivaluedJoins Creation of new joins for multivalued properties is forbidden.
     */
    public HqlDataInstance<?> resolveItemPath(ItemPath relativePath, ItemDefinition<?> itemDefinition,
            String currentHqlPath, JpaEntityDefinition baseEntityDefinition,
            boolean reuseMultivaluedJoins) throws QueryException {
        HqlDataInstance<?> baseDataInstance = new HqlDataInstance<>(currentHqlPath, baseEntityDefinition, null);
        return resolveItemPath(relativePath, itemDefinition, baseDataInstance, reuseMultivaluedJoins);
    }

    public HqlDataInstance<?> resolveItemPath(ItemPath relativePath, ItemDefinition<?> itemDefinition,
            HqlDataInstance<?> baseDataInstance, boolean singletonOnly) throws QueryException {

        ItemPathResolutionState currentState = new ItemPathResolutionState(relativePath, baseDataInstance, this);

        LOGGER.trace("Starting resolution and context update for item path '{}', singletonOnly='{}'", relativePath, singletonOnly);

        while (!currentState.isFinal()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Current resolution state:\n{}", currentState.debugDumpNoParent());
            }
            currentState = currentState.nextState(itemDefinition, singletonOnly);
        }

        LOGGER.trace("resolveItemPath({}) ending in resolution state of:\n{}", relativePath, currentState.debugDumpLazily());
        return currentState.getHqlDataInstance();
    }

    String reuseOrAddJoin(JpaLinkDefinition<?> joinedItemDefinition, String currentHqlPath, boolean reuseMultivaluedJoins) throws QueryException {
        HibernateQuery hibernateQuery = context.getHibernateQuery();
        String joinedItemJpaName = joinedItemDefinition.getJpaName();
        String joinedItemFullPath = currentHqlPath + "." + joinedItemJpaName;
        String joinedItemAlias;
        if (joinedItemDefinition.isMultivalued()) {
            if (reuseMultivaluedJoins) {
                String existingAlias = findExistingAlias(hibernateQuery, joinedItemDefinition, joinedItemFullPath);
                if (existingAlias != null) {
                    return existingAlias;
                } else {
                    // TODO better explanation
                    throw new QueryException("Couldn't reuse existing join for " + joinedItemDefinition + " in the context of " + currentHqlPath);
                }
            }
        } else {
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
            String existingAlias = findExistingAlias(hibernateQuery, joinedItemDefinition, joinedItemFullPath);
            if (existingAlias != null) {
                return existingAlias;
            }
        }
        joinedItemAlias = hibernateQuery.createAlias(joinedItemDefinition);
        Condition condition = createJoinCondition(joinedItemAlias, joinedItemDefinition, hibernateQuery);
        String explicitType = joinedItemDefinition.getExplicitJoinedType();
        hibernateQuery.getPrimaryEntity().addJoin(
                new JoinSpecification(explicitType, joinedItemAlias, joinedItemFullPath, condition));
        return joinedItemAlias;
    }

    private String findExistingAlias(HibernateQuery hibernateQuery,
            JpaLinkDefinition<?> joinedItemDefinition, String joinedItemFullPath) throws QueryException {
        for (JoinSpecification existingJoin : hibernateQuery.getPrimaryEntity().getJoinsFor(joinedItemFullPath)) {
            // Are both joins compatible in this regard? In theory, we could combine them even if they are not,
            // but that would be too complex to implement.
            if (!Objects.equals(existingJoin.getExplicitJoinedType(), joinedItemDefinition.getExplicitJoinedType())) {
                continue;
            }
            // but let's check the condition as well
            String existingAlias = existingJoin.getAlias();
            // we have to create condition for existing alias, to be matched to existing condition
            Condition conditionForExistingAlias = createJoinCondition(existingAlias, joinedItemDefinition, hibernateQuery);
            if (Objects.equals(conditionForExistingAlias, existingJoin.getCondition())) {
                LOGGER.trace("Reusing alias '{}' for joined path '{}'", existingAlias, joinedItemFullPath);
                return existingAlias;
            }
        }
        return null;
    }

    public String addTextInfoJoin(String currentHqlPath) {
        HibernateQuery hibernateQuery = context.getHibernateQuery();
        String joinedItemJpaName = RObject.F_TEXT_INFO_ITEMS;
        String joinedItemFullPath = currentHqlPath + "." + joinedItemJpaName;
        String joinedItemAlias = hibernateQuery.createAlias(joinedItemJpaName, false);
        hibernateQuery.getPrimaryEntity().addJoin(
                new JoinSpecification(null, joinedItemAlias, joinedItemFullPath, null));
        return joinedItemAlias;
    }

    private Condition createJoinCondition(String joinedItemAlias, JpaLinkDefinition<?> joinedItemDefinition, HibernateQuery hibernateQuery) throws QueryException {
        Condition condition = null;
        if (joinedItemDefinition instanceof JpaAnyItemLinkDefinition<?> anyLinkDef) {
            AndCondition conjunction = hibernateQuery.createAnd();
            if (anyLinkDef.getOwnerType() != null) {        // null for assignment extensions
                conjunction.add(hibernateQuery.createEq(joinedItemAlias + ".ownerType", anyLinkDef.getOwnerType()));
            }
            ExtItemDictionary dictionary = context.getExtItemDictionary();
            RExtItem extItemDefinition = dictionary.findItemByDefinition(anyLinkDef.getItemDefinition());
            if (extItemDefinition != null) {
                conjunction.add(hibernateQuery.createEq(joinedItemAlias + "." + RAnyValue.F_ITEM_ID,
                        extItemDefinition.getId()));
            } else {
                // there are no rows referencing this item, because it does not exist in RExtItem (yet)
                conjunction.add(hibernateQuery.createFalse());
            }
            condition = conjunction;
        } else if (joinedItemDefinition.getCollectionSpecification() instanceof VirtualCollectionSpecification vcd) {
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
        Class<?> type = param.type();
        String value = param.value();

        try {
            if (type.isPrimitive()) {
                return type.getMethod("valueOf", new Class[] { String.class }).invoke(null, value);
            }

            if (type.isEnum()) {
                //noinspection unchecked,rawtypes
                return Enum.valueOf((Class<Enum>) type, value);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | RuntimeException ex) {
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
     * @return Entity type definition + item definition, or null if nothing was found
     */
    public <D extends JpaDataNodeDefinition>
    RootedDataSearchResult<D> findProperDataDefinition(
            JpaEntityDefinition baseEntityDefinition, ItemPath path, ItemDefinition<?> itemDefinition, Class<D> clazz)
            throws QueryException {
        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();
        RootedDataSearchResult<D> candidateResult = null;

        for (JpaEntityDefinition entityDefinition : findPossibleBaseEntities(baseEntityDefinition, registry)) {
            DataSearchResult<D> result = entityDefinition.findDataNodeDefinition(path, itemDefinition, clazz);
            if (result != null) {
                if (candidateResult == null) {
                    candidateResult = new RootedDataSearchResult<>(entityDefinition, result);
                } else {
                    // Check for compatibility. As entities are presented from the more abstract to less abstract,
                    // there is no possibility of false alarm.
                    if (!candidateResult.getRootEntityDefinition().isAssignableFrom(entityDefinition)) {
                        throw new QueryException(
                                "Unable to determine root entity for %s: found incompatible candidates: %s and %s".formatted(
                                        path, candidateResult.getRootEntityDefinition(), entityDefinition));
                    }
                }
            }
        }
        LOGGER.trace("findProperDataDefinition: base='{}', path='{}', def='{}', class={} -- returning '{}'",
                baseEntityDefinition, path, itemDefinition, clazz.getSimpleName(), candidateResult);
        return candidateResult;
    }

    private List<JpaEntityDefinition> findPossibleBaseEntities(JpaEntityDefinition entityDefinition, QueryDefinitionRegistry registry) {
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
        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();
        JpaEntityDefinition specificEntityDefinition = registry.findEntityDefinition(specificTypeName);
        if (!baseEntityDefinition.isAssignableFrom(specificEntityDefinition)) {
            throw new QueryException("Entity " + baseEntityDefinition + " cannot be restricted to " + specificEntityDefinition);
        }
        return specificEntityDefinition;
    }

}
