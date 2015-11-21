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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.prism.query.UndefinedFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.definition.CollectionDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.Definition;
import com.evolveum.midpoint.repo.sql.query2.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaDefinitionPath;
import com.evolveum.midpoint.repo.sql.query2.definition.PropertyDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.ProjectionElement;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query2.matcher.DefaultMatcher;
import com.evolveum.midpoint.repo.sql.query2.matcher.Matcher;
import com.evolveum.midpoint.repo.sql.query2.matcher.PolyStringMatcher;
import com.evolveum.midpoint.repo.sql.query2.matcher.StringMatcher;
import com.evolveum.midpoint.repo.sql.query2.restriction.AndRestriction;
import com.evolveum.midpoint.repo.sql.query2.restriction.AnyPropertyRestriction;
import com.evolveum.midpoint.repo.sql.query2.restriction.CollectionRestriction;
import com.evolveum.midpoint.repo.sql.query2.restriction.InOidRestriction;
import com.evolveum.midpoint.repo.sql.query2.restriction.ItemRestrictionOperation;
import com.evolveum.midpoint.repo.sql.query2.restriction.NotRestriction;
import com.evolveum.midpoint.repo.sql.query2.restriction.OrRestriction;
import com.evolveum.midpoint.repo.sql.query2.restriction.OrgRestriction;
import com.evolveum.midpoint.repo.sql.query2.restriction.PropertyRestriction;
import com.evolveum.midpoint.repo.sql.query2.restriction.ReferenceRestriction;
import com.evolveum.midpoint.repo.sql.query2.restriction.Restriction;
import com.evolveum.midpoint.repo.sql.query2.restriction.TypeRestriction;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.commons.lang.Validate;
import org.hibernate.Session;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl.ObjectPagingAfterOid;

/**
 * Interprets midPoint queries by translating them to hibernate (HQL) ones.
 *
 * There are two parts:
 *  - filter translation,
 *  - paging translation.
 *
 * As for filter translation, we traverse the filter depth-first, creating an isomorphic structure of Restrictions.
 * While creating them, we continually build a set of entity references that are necessary to evaluate the query;
 * these references are in the form of cartesian join of entities from which each one can have a set of entities
 * connected to it via left outer join. An example:
 *
 * from
 *   RUser u
 *     left join u.assignments a with ...
 *     left join u.organization o,
 *   RRole r
 *     left join r.assignments a2 with ...
 *
 * This structure is maintained in InterpretationContext, namely in the HibernateQuery being prepared. (In order to
 * produce HQL, we use ad-hoc "hibernate query model" in hqm package, rooted in HibernateQuery class.)
 *
 * Paging translation is done after filters are translated. It may add some entity references as well, if they are not
 * already present.
 *
 * @author lazyman
 * @author mederly
 */
public class QueryInterpreter2 {

    private static final Trace LOGGER = TraceManager.getTrace(QueryInterpreter2.class);
    private static final Map<Class, Matcher> AVAILABLE_MATCHERS;

    static {
        Map<Class, Matcher> matchers = new HashMap<Class, Matcher>();
        //default matcher with null key
        matchers.put(null, new DefaultMatcher());
        matchers.put(PolyString.class, new PolyStringMatcher());
        matchers.put(String.class, new StringMatcher());

        AVAILABLE_MATCHERS = Collections.unmodifiableMap(matchers);
    }

    private SqlRepositoryConfiguration repoConfiguration;

    public QueryInterpreter2(SqlRepositoryConfiguration repoConfiguration) {
        this.repoConfiguration = repoConfiguration;
    }

    public SqlRepositoryConfiguration getRepoConfiguration() {
        return repoConfiguration;
    }

    public RootHibernateQuery interpret(ObjectQuery query, Class<? extends ObjectType> type,
                                        Collection<SelectorOptions<GetOperationOptions>> options, PrismContext prismContext,
                                        boolean countingObjects, Session session) throws QueryException {
        Validate.notNull(type, "Type must not be null.");
        Validate.notNull(session, "Session must not be null.");
        Validate.notNull(prismContext, "Prism context must not be null.");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Interpreting query for type '{}', query:\n{}", new Object[]{type, query});
        }

        InterpretationContext context = new InterpretationContext(this, type, prismContext, session);

        interpretQueryFilter(query, context);
        interpretPagingAndSorting(query, context, countingObjects);

        RootHibernateQuery hibernateQuery = context.getHibernateQuery();

        if (!countingObjects) {
            String rootAlias = hibernateQuery.getPrimaryEntityAlias();
            hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".fullObject"));
            // TODO other objects if parent is requested?
            hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".stringsCount"));
            hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".longsCount"));
            hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".datesCount"));
            hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".referencesCount"));
            hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".polysCount"));
            hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".booleansCount"));
        }

        return hibernateQuery;
    }

    private void interpretQueryFilter(ObjectQuery query, InterpretationContext context) throws QueryException {
        try {
            if (query != null && query.getFilter() != null) {
                Condition c = interpretFilter(query.getFilter(), context, null);
                HibernateQuery hibernateQuery = context.getHibernateQuery();
                hibernateQuery.addCondition(c);
            }
        } catch (QueryException ex) {
            throw ex;
        } catch (Exception ex) {
            LOGGER.trace(ex.getMessage(), ex);
            throw new QueryException(ex.getMessage(), ex);
        }
    }

    public Condition interpretFilter(ObjectFilter filter, InterpretationContext context, Restriction parent) throws QueryException {
        context.pushFilter(filter);
        Restriction restriction = findAndCreateRestriction(filter, context, parent);
        Condition condition = restriction.interpret();
        context.popFilter();
        return condition;
    }

    private <T extends ObjectFilter> Restriction findAndCreateRestriction(T filter, InterpretationContext context,
                                                                          Restriction parent) throws QueryException {

        Validate.notNull(filter);
        Validate.notNull(context);

        String alias = context.getHibernateQuery().getPrimaryEntityAlias();

        // the order of processing restrictions can be important, so we do the selection via handwritten code
        Restriction restriction;
        if (filter instanceof AndFilter) {
            restriction = new AndRestriction();
        } else if (filter instanceof OrFilter) {
            restriction = new OrRestriction();
        } else if (filter instanceof NotFilter) {
            restriction = new NotRestriction();
        } else if (filter instanceof InOidFilter) {
            restriction = new InOidRestriction();
        } else if (filter instanceof OrgFilter) {
            restriction = new OrgRestriction();
        } else if (filter instanceof TypeFilter) {
            restriction = new TypeRestriction();
        } else if (filter instanceof RefFilter) {
            ItemPath fullPath = new ItemPath(parent.getItemPathForChildren(), ((RefFilter) filter).getFullPath());
            EntityDefinition entityDefinition = context.findProperEntityDefinition(fullPath);
            restriction = new ReferenceRestriction(entityDefinition, alias, entityDefinition);
        } else if (filter instanceof ValueFilter) {
            ValueFilter valFilter = (ValueFilter) filter;
            ItemPath fullPath = new ItemPath(parent.getItemPathForChildren(), valFilter.getFullPath());

            ProperDefinitionSearchResult<PropertyDefinition> propDef = context.findProperDefinition(fullPath, PropertyDefinition.class);
            if (propDef != null && propDef.getItemDefinition() != null) {
                restriction = new PropertyRestriction(propDef.getRootEntityDefinition(), alias, propDef.getRootEntityDefinition(), propDef.getItemDefinition());
            } else {
                ProperDefinitionSearchResult<CollectionDefinition> collDef = context.findProperDefinition(fullPath, CollectionDefinition.class);
                if (collDef != null && collDef.getItemDefinition() != null && collDef.getItemDefinition().getDefinition() instanceof PropertyDefinition) {
                    restriction = new CollectionRestriction(collDef.getRootEntityDefinition(), alias, collDef.getItemDefinition());
                } else {
                    EntityDefinition entityDefinition = context.findProperEntityDefinition(fullPath);
                    JpaDefinitionPath jpaDefinitionPath = entityDefinition.translatePath(fullPath);
                    if (fullPath.first().equivalent(new NameItemPathSegment(ObjectType.F_EXTENSION)) ||
                            fullPath.first().equivalent(new NameItemPathSegment(ShadowType.F_ATTRIBUTES)) ||
                            jpaDefinitionPath.containsAnyDefinition()) {
                        restriction = new AnyPropertyRestriction(entityDefinition, alias, entityDefinition);
                    } else {
                        throw new QueryException("Couldn't find a proper restriction for a ValueFilter: " + valFilter.debugDump());
                    }
                }
            }
        } else if (filter instanceof NoneFilter || filter instanceof AllFilter || filter instanceof UndefinedFilter) {
            // these should be filtered out by the client
            throw new IllegalStateException("Trivial filters are not supported by QueryInterpreter: " + filter.debugDump());
        } else {
            throw new IllegalStateException("Unknown filter: " + filter.debugDump());
        }

        restriction.setContext(context);
        restriction.setParent(parent);
        restriction.setFilter(filter);
        return restriction;
    }

    private void interpretPagingAndSorting(ObjectQuery query, InterpretationContext context, boolean countingObjects) {
        RootHibernateQuery hibernateQuery = context.getHibernateQuery();
        String rootAlias = hibernateQuery.getPrimaryEntityAlias();

        if (query != null && query.getPaging() instanceof ObjectPagingAfterOid) {
            ObjectPagingAfterOid paging = (ObjectPagingAfterOid) query.getPaging();
            if (paging.getOidGreaterThan() != null) {
                Condition c = hibernateQuery.createSimpleComparisonCondition(rootAlias + ".oid", paging.getOidGreaterThan(), ">");
                hibernateQuery.addCondition(c);
            }
        }

        if (!countingObjects && query != null && query.getPaging() != null) {
            if (query.getPaging() instanceof ObjectPagingAfterOid) {
                updatePagingAndSortingByOid(hibernateQuery, (ObjectPagingAfterOid) query.getPaging());                // very special case - ascending ordering by OID (nothing more)
            } else {
                updatePagingAndSorting(hibernateQuery, context.getType(), query.getPaging());
            }
        }
    }

    protected void updatePagingAndSortingByOid(RootHibernateQuery hibernateQuery, ObjectPagingAfterOid paging) {
        String rootAlias = hibernateQuery.getPrimaryEntityAlias();
        if (paging.getOrderBy() != null || paging.getDirection() != null || paging.getOffset() != null) {
            throw new IllegalArgumentException("orderBy, direction nor offset is allowed on ObjectPagingAfterOid");
        }
        hibernateQuery.setOrder(rootAlias + ".oid", OrderDirection.ASCENDING);
        if (paging.getMaxSize() != null) {
            hibernateQuery.setMaxResults(paging.getMaxSize());
        }
    }

    public <T extends ObjectType> void updatePagingAndSorting(RootHibernateQuery hibernateQuery, Class<T> type, ObjectPaging paging) {
        if (paging == null) {
            return;
        }
        if (paging.getOffset() != null) {
            hibernateQuery.setFirstResult(paging.getOffset());
        }
        if (paging.getMaxSize() != null) {
            hibernateQuery.setMaxResults(paging.getMaxSize());
        }

        if (paging.getDirection() == null && (paging.getOrderBy() == null || paging.getOrderBy().isEmpty())) {
            return;
        }

        QueryDefinitionRegistry2 registry = QueryDefinitionRegistry2.getInstance();
        if (paging.getOrderBy() == null || paging.getOrderBy().isEmpty() || paging.getOrderBy().size() > 1 || !(paging.getOrderBy().first() instanceof NameItemPathSegment)) {
            LOGGER.warn("Ordering by property path with size not equal 1 is not supported '" + paging.getOrderBy()
                    + "'.");
            return;
        }
        // FIXME this has to be enhanced for multi-segment paths! (e.g. create joins if needed)
        Definition def = registry.findDefinition(type, paging.getOrderBy(), Definition.class);
        if (def == null) {
            LOGGER.warn("Unknown path '" + paging.getOrderBy() + "', couldn't find definition for it, "
                    + "list will not be ordered by it.");
            return;
        }

        String propertyName = hibernateQuery.getPrimaryEntityAlias() + "." + def.getJpaName();
        if (PolyString.class.equals(def.getJaxbType())) {
            propertyName += ".orig";
        }

        if (paging.getDirection() != null) {
            switch (paging.getDirection()) {
                case ASCENDING:
                    hibernateQuery.setOrder(propertyName, OrderDirection.ASCENDING);
                    break;
                case DESCENDING:
                    hibernateQuery.setOrder(propertyName, OrderDirection.DESCENDING);
                    break;
            }
        } else {
            hibernateQuery.setOrder(propertyName, OrderDirection.ASCENDING);
        }
    }

    public <T extends Object> Matcher<T> findMatcher(T value) {
        return findMatcher(value != null ? (Class<T>) value.getClass() : null);
    }

    public <T extends Object> Matcher<T> findMatcher(Class<T> type) {
        Matcher<T> matcher = AVAILABLE_MATCHERS.get(type);
        if (matcher == null) {
            //we return default matcher
            matcher = AVAILABLE_MATCHERS.get(null);
        }
        return matcher;
    }
}
