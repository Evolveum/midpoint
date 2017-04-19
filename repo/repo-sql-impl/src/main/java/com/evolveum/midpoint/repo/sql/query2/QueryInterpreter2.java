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

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.sql.ObjectPagingAfterOid;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.definition.*;
import com.evolveum.midpoint.repo.sql.query2.hqm.ProjectionElement;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query2.matcher.DefaultMatcher;
import com.evolveum.midpoint.repo.sql.query2.matcher.Matcher;
import com.evolveum.midpoint.repo.sql.query2.matcher.PolyStringMatcher;
import com.evolveum.midpoint.repo.sql.query2.matcher.StringMatcher;
import com.evolveum.midpoint.repo.sql.query2.resolution.ItemPathResolver;
import com.evolveum.midpoint.repo.sql.query2.resolution.ProperDataSearchResult;
import com.evolveum.midpoint.repo.sql.query2.restriction.*;
import com.evolveum.midpoint.repo.sql.util.GetCertificationWorkItemResult;
import com.evolveum.midpoint.repo.sql.util.GetContainerableResult;
import com.evolveum.midpoint.repo.sql.util.GetObjectResult;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import org.apache.commons.lang.Validate;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Interprets midPoint queries by translating them to hibernate (HQL) ones.
 * <p/>
 * There are two parts:
 * - filter translation,
 * - paging translation.
 * <p/>
 * As for filter translation, we traverse the filter depth-first, creating an isomorphic structure of Restrictions.
 * While creating them, we continually build a set of entity references that are necessary to evaluate the query;
 * these references are in the form of cartesian join of entities from which each one can have a set of entities
 * connected to it via left outer join. An example:
 * <p/>
 * from
 * RUser u
 * left join u.assignments a with ...
 * left join u.organization o,
 * RRole r
 * left join r.assignments a2 with ...
 * <p/>
 * This structure is maintained in InterpretationContext, namely in the HibernateQuery being prepared. (In order to
 * produce HQL, we use ad-hoc "hibernate query model" in hqm package, rooted in HibernateQuery class.)
 * <p/>
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
        Map<Class, Matcher> matchers = new HashMap<>();
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

    public RootHibernateQuery interpret(ObjectQuery query, Class<? extends Containerable> type,
                                        Collection<SelectorOptions<GetOperationOptions>> options, PrismContext prismContext,
                                        boolean countingObjects, Session session) throws QueryException {
        Validate.notNull(type, "Type must not be null.");
        Validate.notNull(session, "Session must not be null.");
        Validate.notNull(prismContext, "Prism context must not be null.");

        LOGGER.trace("Interpreting query for type '{}', query:\n{}", type, query);

        InterpretationContext context = new InterpretationContext(this, type, prismContext, session);

        interpretQueryFilter(context, query);
        interpretPagingAndSorting(context, query, countingObjects);

        RootHibernateQuery hibernateQuery = context.getHibernateQuery();
		boolean distinct = GetOperationOptions.isDistinct(SelectorOptions.findRootOptions(options));

        if (countingObjects) {
        	if (distinct) {
				String rootAlias = hibernateQuery.getPrimaryEntityAlias();
				hibernateQuery.addProjectionElement(new ProjectionElement("count(distinct " + rootAlias + ")"));
			} else {
				hibernateQuery.addProjectionElement(new ProjectionElement("count(*)"));
			}
        } else {
			hibernateQuery.setDistinct(distinct);

			String rootAlias = hibernateQuery.getPrimaryEntityAlias();
            // TODO other objects if parent is requested?
            if (context.isObject()) {
                hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".fullObject"));
                hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".stringsCount"));
                hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".longsCount"));
                hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".datesCount"));
                hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".referencesCount"));
                hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".polysCount"));
                hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".booleansCount"));
                hibernateQuery.setResultTransformer(GetObjectResult.RESULT_TRANSFORMER);
            } else if (AccessCertificationCaseType.class.equals(context.getType())) {
				hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".fullObject"));
                hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".ownerOid"));
                hibernateQuery.setResultTransformer(GetContainerableResult.RESULT_TRANSFORMER);
            } else if (AccessCertificationWorkItemType.class.equals(context.getType())) {
            	// TODO owner's full object
				hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".ownerOwnerOid"));
				hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".ownerId"));
				hibernateQuery.addProjectionElement(new ProjectionElement(rootAlias + ".id"));
				hibernateQuery.setResultTransformer(GetCertificationWorkItemResult.RESULT_TRANSFORMER);
            } else {
            	throw new QueryException("Unsupported type: " + context.getType());
			}
        }

        return hibernateQuery;
    }

    private void interpretQueryFilter(InterpretationContext context, ObjectQuery query) throws QueryException {
        if (query != null && query.getFilter() != null) {
            Condition c = interpretFilter(context, query.getFilter(), null);
			context.getHibernateQuery().addCondition(c);
        }
    }

    public Condition interpretFilter(InterpretationContext context, ObjectFilter filter, Restriction parent) throws QueryException {
        Restriction restriction = findAndCreateRestriction(filter, context, parent);
        Condition condition = restriction.interpret();
        return condition;
    }

    private <T extends ObjectFilter> Restriction findAndCreateRestriction(@NotNull T filter,
		    @NotNull InterpretationContext context, Restriction parent) throws QueryException {

        LOGGER.trace("Determining restriction for filter {}", filter);

        ItemPathResolver helper = context.getItemPathResolver();
        JpaEntityDefinition baseEntityDefinition;
        if (parent != null) {
            baseEntityDefinition = parent.getBaseHqlEntityForChildren().getJpaDefinition();
        } else {
            baseEntityDefinition = context.getRootEntityDefinition();
        }
        Restriction restriction = findAndCreateRestrictionInternal(filter, context, parent, helper, baseEntityDefinition);

        LOGGER.trace("Restriction for {} is {}", filter.getClass().getSimpleName(), restriction);
        return restriction;
    }

    private <T extends ObjectFilter>
    Restriction findAndCreateRestrictionInternal(T filter, InterpretationContext context, Restriction parent,
		    ItemPathResolver resolver, JpaEntityDefinition baseEntityDefinition) throws QueryException {

        // the order of processing restrictions can be important, so we do the selection via handwritten code

        if (filter instanceof AndFilter) {
            return new AndRestriction(context, (AndFilter) filter, baseEntityDefinition, parent);
        } else if (filter instanceof OrFilter) {
            return new OrRestriction(context, (OrFilter) filter, baseEntityDefinition, parent);
        } else if (filter instanceof NotFilter) {
            return new NotRestriction(context, (NotFilter) filter, baseEntityDefinition, parent);
        } else if (filter instanceof FullTextFilter) {
	        return new FullTextRestriction(context, (FullTextFilter) filter, baseEntityDefinition, parent);
        } else if (filter instanceof InOidFilter) {
            return new InOidRestriction(context, (InOidFilter) filter, baseEntityDefinition, parent);
        } else if (filter instanceof OrgFilter) {
            return new OrgRestriction(context, (OrgFilter) filter, baseEntityDefinition, parent);
        } else if (filter instanceof TypeFilter) {
            TypeFilter typeFilter = (TypeFilter) filter;
            JpaEntityDefinition refinedEntityDefinition = resolver.findRestrictedEntityDefinition(baseEntityDefinition, typeFilter.getType());
            return new TypeRestriction(context, typeFilter, refinedEntityDefinition, parent);
        } else if (filter instanceof ExistsFilter) {
            ExistsFilter existsFilter = (ExistsFilter) filter;
            ItemPath path = existsFilter.getFullPath();
            ItemDefinition definition = existsFilter.getDefinition();
            ProperDataSearchResult<JpaEntityDefinition> searchResult = resolver.findProperDataDefinition(
                    baseEntityDefinition, path, definition, JpaEntityDefinition.class, context.getPrismContext());
            if (searchResult == null) {
                throw new QueryException("Path for ExistsFilter (" + path + ") doesn't point to a hibernate entity within " + baseEntityDefinition);
            }
            return new ExistsRestriction(context, existsFilter, searchResult.getEntityDefinition(), parent);
        } else if (filter instanceof RefFilter) {
            RefFilter refFilter = (RefFilter) filter;
            ItemPath path = refFilter.getFullPath();
            ItemDefinition definition = refFilter.getDefinition();
            ProperDataSearchResult<JpaReferenceDefinition> searchResult = resolver.findProperDataDefinition(
                    baseEntityDefinition, path, definition, JpaReferenceDefinition.class, context.getPrismContext());
            if (searchResult == null) {
                throw new QueryException("Path for RefFilter (" + path + ") doesn't point to a reference item within " + baseEntityDefinition);
            }
            return new ReferenceRestriction(context, refFilter, searchResult.getEntityDefinition(),
                    parent, searchResult.getLinkDefinition());
        } else if (filter instanceof PropertyValueFilter) {
            PropertyValueFilter valFilter = (PropertyValueFilter) filter;
            ItemPath path = valFilter.getFullPath();
            ItemDefinition definition = valFilter.getDefinition();

            ProperDataSearchResult<JpaPropertyDefinition> propDefRes = resolver.findProperDataDefinition(baseEntityDefinition, path, definition, JpaPropertyDefinition.class,
                    context.getPrismContext());
            if (propDefRes == null) {
                throw new QueryException("Couldn't find a proper data item to query, given base entity " + baseEntityDefinition + " and this filter: " + valFilter.debugDump());
            }
            // TODO can't be unified?
            if (propDefRes.getTargetDefinition() instanceof JpaAnyPropertyDefinition) {
                return new AnyPropertyRestriction(context, valFilter, propDefRes.getEntityDefinition(), parent, propDefRes.getLinkDefinition());
            } else {
                return new PropertyRestriction(context, valFilter, propDefRes.getEntityDefinition(), parent, propDefRes.getLinkDefinition());
            }
        } else if (filter instanceof NoneFilter || filter instanceof AllFilter || filter instanceof UndefinedFilter) {
            // these should be filtered out by the client
            throw new IllegalStateException("Trivial filters are not supported by QueryInterpreter: " + filter.debugDump());
        } else {
            throw new IllegalStateException("Unknown filter: " + filter.debugDump());
        }
    }

    private void interpretPagingAndSorting(InterpretationContext context, ObjectQuery query, boolean countingObjects) throws QueryException {
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
                updatePagingAndSorting(context, query.getPaging());
            }
        }
    }

    protected void updatePagingAndSortingByOid(RootHibernateQuery hibernateQuery, ObjectPagingAfterOid paging) {
        String rootAlias = hibernateQuery.getPrimaryEntityAlias();
        if (paging.getOrderBy() != null || paging.getDirection() != null || paging.getOffset() != null) {
            throw new IllegalArgumentException("orderBy, direction nor offset is allowed on ObjectPagingAfterOid");
        }
        hibernateQuery.addOrdering(rootAlias + ".oid", OrderDirection.ASCENDING);
        if (paging.getMaxSize() != null) {
            hibernateQuery.setMaxResults(paging.getMaxSize());
        }
    }

    public <T extends Containerable> void updatePagingAndSorting(InterpretationContext context,
                                                                 ObjectPaging paging) throws QueryException {

        if (paging == null) {
            return;
        }

        RootHibernateQuery hibernateQuery = context.getHibernateQuery();
        if (paging.getOffset() != null) {
            hibernateQuery.setFirstResult(paging.getOffset());
        }
        if (paging.getMaxSize() != null) {
            hibernateQuery.setMaxResults(paging.getMaxSize());
        }

        if (!paging.hasOrdering()) {
            return;
        }

        for (ObjectOrdering ordering : paging.getOrderingInstructions()) {
            addOrdering(context, ordering);
        }
    }

    private void addOrdering(InterpretationContext context, ObjectOrdering ordering) throws QueryException {

        ItemPath orderByPath = ordering.getOrderBy();

        // TODO if we'd like to have order-by extension properties, we'd need to provide itemDefinition for them
        ProperDataSearchResult<JpaDataNodeDefinition> result = context.getItemPathResolver().findProperDataDefinition(
                context.getRootEntityDefinition(), orderByPath, null, JpaDataNodeDefinition.class, context.getPrismContext());
        if (result == null) {
            LOGGER.error("Unknown path '" + orderByPath + "', couldn't find definition for it, "
                    + "list will not be ordered by it.");
            return;
        }
        JpaDataNodeDefinition targetDefinition = result.getLinkDefinition().getTargetDefinition();
        if (targetDefinition instanceof JpaAnyContainerDefinition) {
            throw new QueryException("Sorting based on extension item or attribute is not supported yet: " + orderByPath);
        } else if (targetDefinition instanceof JpaReferenceDefinition) {
            throw new QueryException("Sorting based on reference is not supported: " + orderByPath);
        } else if (result.getLinkDefinition().isMultivalued()) {
            throw new QueryException("Sorting based on multi-valued item is not supported: " + orderByPath);
        } else if (targetDefinition instanceof JpaEntityDefinition) {
            throw new QueryException("Sorting based on entity is not supported: " + orderByPath);
        } else if (!(targetDefinition instanceof JpaPropertyDefinition)) {
            throw new IllegalStateException("Unknown item definition type: " + result.getClass());
        }

        JpaEntityDefinition baseEntityDefinition = result.getEntityDefinition();
        JpaPropertyDefinition orderByDefinition = (JpaPropertyDefinition) targetDefinition;
        String hqlPropertyPath = context.getItemPathResolver()
                .resolveItemPath(orderByPath, null, context.getPrimaryEntityAlias(), baseEntityDefinition, true)
                .getHqlPath();
        if (RPolyString.class.equals(orderByDefinition.getJpaClass())) {
            hqlPropertyPath += ".orig";
        }

        RootHibernateQuery hibernateQuery = context.getHibernateQuery();
        if (ordering.getDirection() != null) {
            switch (ordering.getDirection()) {
                case ASCENDING:
                    hibernateQuery.addOrdering(hqlPropertyPath, OrderDirection.ASCENDING);
                    break;
                case DESCENDING:
                    hibernateQuery.addOrdering(hqlPropertyPath, OrderDirection.DESCENDING);
                    break;
            }
        } else {
            hibernateQuery.addOrdering(hqlPropertyPath, OrderDirection.ASCENDING);
        }

    }

    public <T> Matcher<T> findMatcher(T value) {
        return findMatcher(value != null ? (Class<T>) value.getClass() : null);
    }

    public <T> Matcher<T> findMatcher(Class<T> type) {
        Matcher<T> matcher = AVAILABLE_MATCHERS.get(type);
        if (matcher == null) {
            //we return default matcher
            matcher = AVAILABLE_MATCHERS.get(null);
        }
        return matcher;
    }
}
