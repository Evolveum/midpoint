/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.helpers.ObjectRetriever;
import com.evolveum.midpoint.repo.sql.query.definition.*;
import com.evolveum.midpoint.repo.sql.query.hqm.CountProjectionElement;
import com.evolveum.midpoint.repo.sql.query.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query.matcher.DefaultMatcher;
import com.evolveum.midpoint.repo.sql.query.matcher.Matcher;
import com.evolveum.midpoint.repo.sql.query.matcher.PolyStringMatcher;
import com.evolveum.midpoint.repo.sql.query.matcher.StringMatcher;
import com.evolveum.midpoint.repo.sql.query.resolution.ItemPathResolver;
import com.evolveum.midpoint.repo.sql.query.resolution.ProperDataSearchResult;
import com.evolveum.midpoint.repo.sql.query.restriction.*;
import com.evolveum.midpoint.repo.sql.util.*;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

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
public class QueryInterpreter {

    private static final Trace LOGGER = TraceManager.getTrace(QueryInterpreter.class);
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
    private ExtItemDictionary extItemDictionary;

    public QueryInterpreter(SqlRepositoryConfiguration repoConfiguration, ExtItemDictionary extItemDictionary) {
        this.repoConfiguration = repoConfiguration;
        this.extItemDictionary = extItemDictionary;
    }

    public SqlRepositoryConfiguration getRepoConfiguration() {
        return repoConfiguration;
    }

    public RootHibernateQuery interpret(ObjectQuery query, @NotNull Class<? extends Containerable> type,
            Collection<SelectorOptions<GetOperationOptions>> options, @NotNull PrismContext prismContext,
            @NotNull RelationRegistry relationRegistry, boolean countingObjects, @NotNull Session session) throws QueryException {
        boolean distinctRequested = GetOperationOptions.isDistinct(SelectorOptions.findRootOptions(options));
        LOGGER.trace("Interpreting query for type '{}' (counting={}, distinctRequested={}), query:\n{}", type, countingObjects, distinctRequested, query);

        InterpretationContext context = new InterpretationContext(this, type, prismContext, relationRegistry, extItemDictionary, session);
        interpretQueryFilter(context, query);
        String rootAlias = context.getHibernateQuery().getPrimaryEntityAlias();
        ResultStyle resultStyle = getResultStyle(context);

        if (countingObjects) {
            interpretPagingAndSorting(context, query, true);
            RootHibernateQuery hibernateQuery = context.getHibernateQuery();
            boolean distinct = distinctRequested && !hibernateQuery.isDistinctNotNecessary();
            hibernateQuery.addProjectionElement(new CountProjectionElement(resultStyle.getCountString(rootAlias), distinct));
            return hibernateQuery;
        }

        /*
           Some databases don't support DISTINCT on BLOBs. In these cases we have to create query like:
           select
             u.oid, u.fullObject, u.stringsCount, ..., u.booleansCount
           from
             RUser u
           where
             u.oid in (select distinct u.oid from RUser u where ...)
         */
        boolean distinctBlobCapable = !repoConfiguration.isUsingOracle() && !repoConfiguration.isUsingSQLServer();
        RootHibernateQuery hibernateQuery = context.getHibernateQuery();
        boolean distinct = distinctRequested && !hibernateQuery.isDistinctNotNecessary();
        hibernateQuery.setDistinct(distinct);
        hibernateQuery.addProjectionElementsFor(resultStyle.getIdentifiers(rootAlias));
        if (distinct && !distinctBlobCapable) {
            String subqueryText = "\n" + hibernateQuery.getAsHqlText(2, true);
            InterpretationContext wrapperContext = new InterpretationContext(this, type, prismContext, relationRegistry,
                    extItemDictionary, session);
            interpretPagingAndSorting(wrapperContext, query, false);
            RootHibernateQuery wrapperQuery = wrapperContext.getHibernateQuery();
            if (repoConfiguration.isUsingSQLServer() && resultStyle.getIdentifiers("").size() > 1) {
                // using 'where exists' clause
                // FIXME refactor this ugly code
                String wrappedRootAlias = "_" + wrapperQuery.getPrimaryEntityAlias();    // to distinguish from the same alias in inner query
                wrapperQuery.setPrimaryEntityAlias(wrappedRootAlias);
                wrapperQuery.setResultTransformer(resultStyle.getResultTransformer());
                wrapperQuery.addProjectionElementsFor(resultStyle.getIdentifiers(wrappedRootAlias));
                wrapperQuery.addProjectionElementsFor(resultStyle.getContentAttributes(wrappedRootAlias));
                StringBuilder linkingCondition = new StringBuilder();
                for (String id : resultStyle.getIdentifiers(wrappedRootAlias)) {
                    linkingCondition.append(" and ").append(id).append(" = ").append(id.substring(1));
                }
                wrapperQuery.getConditions().add(wrapperQuery.createExists(subqueryText, linkingCondition.toString()));
            } else {
                // using 'in' clause (multi-column only for Oracle)
                String wrappedRootAlias = wrapperQuery.getPrimaryEntityAlias();
                wrapperQuery.setResultTransformer(resultStyle.getResultTransformer());
                wrapperQuery.addProjectionElementsFor(resultStyle.getIdentifiers(wrappedRootAlias));
                wrapperQuery.addProjectionElementsFor(resultStyle.getContentAttributes(wrappedRootAlias));
                List<String> inVariablesList = resultStyle.getIdentifiers(wrapperQuery.getPrimaryEntityAlias());
                String inVariablesString = inVariablesList.size() != 1
                        ? "(" + StringUtils.join(inVariablesList, ", ") + ")"
                        : inVariablesList.get(0);
                wrapperQuery.getConditions().add(wrapperQuery.createIn(inVariablesString, subqueryText));
            }
            wrapperQuery.addParametersFrom(hibernateQuery.getParameters());
            return wrapperQuery;
        } else {
            interpretPagingAndSorting(context, query, false);
            hibernateQuery.setResultTransformer(resultStyle.getResultTransformer());
            hibernateQuery.addProjectionElementsFor(resultStyle.getContentAttributes(rootAlias));
            if (distinct) {
                hibernateQuery.addProjectionElementsFor(getOrderingAttributes(context));        // SQL requires this
            }
            return hibernateQuery;
        }
    }

    private List<String> getOrderingAttributes(InterpretationContext context) {
        return context.getHibernateQuery().getOrderingList().stream().map(o -> o.getByProperty()).collect(Collectors.toList());
    }

    private ResultStyle getResultStyle(InterpretationContext context) throws QueryException {
        if (context.isObject()) {
            return GetObjectResult.RESULT_STYLE;
        } else if (AccessCertificationCaseType.class.equals(context.getType())) {
            return GetContainerableResult.RESULT_STYLE;
        } else if (AccessCertificationWorkItemType.class.equals(context.getType())) {
            return GetCertificationWorkItemResult.RESULT_STYLE;
        } else if (CaseWorkItemType.class.equals(context.getType())) {
            return GetContainerableIdOnlyResult.RESULT_STYLE;
        } else {
            throw new QueryException("Unsupported type: " + context.getType());
        }
    }

    private void interpretQueryFilter(InterpretationContext context, ObjectQuery query) throws QueryException {
        if (query != null && query.getFilter() != null) {
            Condition c = interpretFilter(context, query.getFilter(), null);
            context.getHibernateQuery().addCondition(c);
        }
    }

    public Condition interpretFilter(InterpretationContext context, ObjectFilter filter, Restriction parent) throws QueryException {
        Restriction restriction = findAndCreateRestriction(filter, context, parent);
        return restriction.interpret();
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
            ProperDataSearchResult<JpaDataNodeDefinition> searchResult = resolver.findProperDataDefinition(
                    baseEntityDefinition, path, definition, JpaDataNodeDefinition.class, context.getPrismContext());
            if (searchResult == null) {
                throw new QueryException("Path for ExistsFilter (" + path + ") doesn't point to a hibernate entity or property within " + baseEntityDefinition);
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

        //noinspection StringEquality
        if (query != null && query.getPaging() != null && query.getPaging().hasCookie() && query.getPaging().getCookie() != ObjectRetriever.NULL_OID_MARKER) {
            ObjectPaging paging = query.getPaging();
            Condition c = hibernateQuery.createSimpleComparisonCondition(rootAlias + ".oid", paging.getCookie(), ">");
            hibernateQuery.addCondition(c);
        }

        if (!countingObjects && query != null && query.getPaging() != null) {
            if (query.getPaging().hasCookie()) {
                updatePagingAndSortingByOid(hibernateQuery, query.getPaging());                // very special case - ascending ordering by OID (nothing more)
            } else {
                updatePagingAndSorting(context, query.getPaging());
            }
        }
    }

    private void updatePagingAndSortingByOid(RootHibernateQuery hibernateQuery, ObjectPaging paging) {
        String rootAlias = hibernateQuery.getPrimaryEntityAlias();
        if (paging.getOrderBy() != null || paging.getDirection() != null || paging.getOffset() != null) {
            throw new IllegalArgumentException("orderBy, direction nor offset is allowed on ObjectPaging with cookie");
        }
        if (repoConfiguration.isUsingOracle()) {
            hibernateQuery.addOrdering("NLSSORT(" + rootAlias + ".oid, 'NLS_SORT=BINARY_AI')", OrderDirection.ASCENDING);
        } else {
            hibernateQuery.addOrdering(rootAlias + ".oid", OrderDirection.ASCENDING);
        }
        if (paging.getMaxSize() != null) {
            hibernateQuery.setMaxResults(paging.getMaxSize());
        }
    }

    private void updatePagingAndSorting(InterpretationContext context, ObjectPaging paging) throws QueryException {
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

        if (paging.hasOrdering()) {
            for (ObjectOrdering ordering : paging.getOrderingInstructions()) {
                addOrdering(context, ordering);
            }
        }

        if (paging.hasGrouping()) {
            for (ObjectGrouping grouping : paging.getGroupingInstructions()) {
                addGrouping(context, grouping);
            }
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

    private void addGrouping(InterpretationContext context, ObjectGrouping grouping) throws QueryException {

        ItemPath groupByPath = grouping.getGroupBy();

        // TODO if we'd like to have group-by extension properties, we'd need to provide itemDefinition for them
        ProperDataSearchResult<JpaDataNodeDefinition> result = context.getItemPathResolver().findProperDataDefinition(
                context.getRootEntityDefinition(), groupByPath, null, JpaDataNodeDefinition.class, context.getPrismContext());
        if (result == null) {
            LOGGER.error("Unknown path '" + groupByPath + "', couldn't find definition for it, "
                    + "list will not be grouped by it.");
            return;
        }
        JpaDataNodeDefinition targetDefinition = result.getLinkDefinition().getTargetDefinition();
        if (targetDefinition instanceof JpaAnyContainerDefinition) {
            throw new QueryException("Grouping based on extension item or attribute is not supported yet: " + groupByPath);
        } else if (targetDefinition instanceof JpaReferenceDefinition) {
            throw new QueryException("Grouping based on reference is not supported: " + groupByPath);
        } else if (result.getLinkDefinition().isMultivalued()) {
            throw new QueryException("Grouping based on multi-valued item is not supported: " + groupByPath);
        } else if (targetDefinition instanceof JpaEntityDefinition) {
            throw new QueryException("Grouping based on entity is not supported: " + groupByPath);
        } else if (!(targetDefinition instanceof JpaPropertyDefinition)) {
            throw new IllegalStateException("Unknown item definition type: " + result.getClass());
        }

        JpaEntityDefinition baseEntityDefinition = result.getEntityDefinition();
        JpaPropertyDefinition groupByDefinition = (JpaPropertyDefinition) targetDefinition;
        String hqlPropertyPath = context.getItemPathResolver()
                .resolveItemPath(groupByPath, null, context.getPrimaryEntityAlias(), baseEntityDefinition, true)
                .getHqlPath();
        if (RPolyString.class.equals(groupByDefinition.getJpaClass())) {
            hqlPropertyPath += ".orig";
        }

        RootHibernateQuery hibernateQuery = context.getHibernateQuery();

        hibernateQuery.addGrouping(hqlPropertyPath);
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
