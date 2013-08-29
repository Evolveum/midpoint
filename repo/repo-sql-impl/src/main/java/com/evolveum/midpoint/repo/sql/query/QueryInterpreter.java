/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.sql.query.definition.Definition;
import com.evolveum.midpoint.repo.sql.query.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.query.matcher.DefaultMatcher;
import com.evolveum.midpoint.repo.sql.query.matcher.Matcher;
import com.evolveum.midpoint.repo.sql.query.matcher.PolyStringMatcher;
import com.evolveum.midpoint.repo.sql.query.matcher.StringMatcher;
import com.evolveum.midpoint.repo.sql.query.restriction.Restriction;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectSelector;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author lazyman
 */
public class QueryInterpreter {

    private static final Trace LOGGER = TraceManager.getTrace(QueryInterpreter.class);
    private static final Set<Restriction> AVAILABLE_RESTRICTIONS;
    private static final Map<Class, Matcher> AVAILABLE_MATCHERS;

    static {
        Set<Restriction> restrictions = new HashSet<Restriction>();

        String packageName = Restriction.class.getPackage().getName();
        Set<Class> classes = ClassPathUtil.listClasses(packageName);
        LOGGER.debug("Found {} classes in package {}.", new Object[]{classes.size(), packageName});
        for (Class<Restriction> clazz : classes) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                //we don't need interfaces and abstract classes
                continue;
            }

            if (!Restriction.class.isAssignableFrom(clazz)) {
                //we don't need classes that don't inherit from Restriction
                continue;
            }

            try {
                Restriction restriction = (Restriction) ConstructorUtils.invokeConstructor(clazz, null);
                restrictions.add(restriction);

                LOGGER.debug("Added '{}' instance to available restrictions.",
                        new Object[]{restriction.getClass().getName()});
            } catch (Exception ex) {
                LoggingUtils.logException(LOGGER, "Error occurred during query interpreter initialization", ex);
                if (ex instanceof SystemException) {
                    throw (SystemException) ex;
                }
                throw new SystemException(ex.getMessage(), ex);
            }
        }

        AVAILABLE_RESTRICTIONS = Collections.unmodifiableSet(restrictions);
    }

    static {
        Map<Class, Matcher> matchers = new HashMap<Class, Matcher>();
        //default matcher with null key
        matchers.put(null, new DefaultMatcher());
        matchers.put(PolyString.class, new PolyStringMatcher());
        matchers.put(String.class, new StringMatcher());


        AVAILABLE_MATCHERS = Collections.unmodifiableMap(matchers);
    }

    public Criteria interpretGet(String oid, Class<? extends ObjectType> type,
                                 Collection<SelectorOptions<GetOperationOptions>> options, PrismContext prismContext,
                                 Session session) throws QueryException {
        Validate.notNull(oid, "Oid must not be null.");
        Validate.notNull(type, "Type must not be null.");
        Validate.notNull(session, "Session must not be null.");
        Validate.notNull(prismContext, "Prism context must not be null.");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Interpreting get for type '{}', oid:\n{}", new Object[]{type.getSimpleName(), oid});
        }

        Criteria main = session.createCriteria(ClassMapper.getHQLTypeClass(type));
        main.add(Restrictions.eq("id", 0L));
        main.add(Restrictions.eq("oid", oid));

        updateFetchingMode(main, type, options);

        return main;
    }

    public Criteria interpret(ObjectQuery query, Class<? extends ObjectType> type,
                              Collection<SelectorOptions<GetOperationOptions>> options, PrismContext prismContext,
                              boolean countingObjects, Session session) throws QueryException {
        Validate.notNull(type, "Type must not be null.");
        Validate.notNull(session, "Session must not be null.");
        Validate.notNull(prismContext, "Prism context must not be null.");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Interpreting query for type '{}', query:\n{}", new Object[]{type, query});
        }

        if (countingObjects) {
            //compute minimal options which should be used in JOINs
            options = createOptions(query);
        }

        Criteria criteria;
        if (query != null && query.getFilter() != null) {
            criteria = interpretQuery(query, type, prismContext, session);
        } else {
            criteria = session.createCriteria(ClassMapper.getHQLTypeClass(type));
        }

        if (countingObjects && query != null && query.getPaging() != null) {
            criteria = updatePagingAndSorting(criteria, type, query.getPaging());
        }

        updateFetchingMode(criteria, type, options);

        return criteria;
    }

    /**
     * This method creates selector options for count operation. Options are created to help query interpreter
     * setup proper fetching strategies when building criteria.
     *
     * @param query
     * @return
     */
    private Collection<SelectorOptions<GetOperationOptions>> createOptions(ObjectQuery query){
        Collection<SelectorOptions<GetOperationOptions>> options =
                new ArrayList<SelectorOptions<GetOperationOptions>>();
        if (query == null || query.getFilter() == null) {
            return options;
        }

        ObjectFilter filter = query.getFilter();
        return createOptionsFromFilter(filter, options);
    }

    private Collection<SelectorOptions<GetOperationOptions>> createOptionsFromFilter(ObjectFilter filter,
                                                                                     Collection<SelectorOptions<GetOperationOptions>> options) {
        if (filter instanceof ValueFilter) {
            ValueFilter vFilter = (ValueFilter) filter;
            ItemPath path = RUtil.createFullPath(vFilter);
            options.add(SelectorOptions.create(path, GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        } else if (filter instanceof LogicalFilter) {
            LogicalFilter lFilter = (LogicalFilter) filter;
            for (ObjectFilter cFilter : lFilter.getCondition()) {
                createOptionsFromFilter(cFilter, options);
            }
        }

        return options;
    }

    /**
     * This method updates fetch mode for hibernate entity associations.
     * <p/>
     * For example, we check and update:
     * {@link ObjectType#F_METADATA}
     * {@link com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType#F_RESULT}
     * <p/>
     * Collection associations always have LAZY initialization (SELECTs not JOINs) and they are loaded if
     * necessary when translating hibernate entity to {@link com.evolveum.midpoint.prism.PrismObject} in
     * {@link com.evolveum.midpoint.repo.sql.data.common.RObject#copyToJAXB}.
     *
     * @param criteria Generated criteria based on oid (when interpreting get operation) or query (when
     *                 interpreting search or count).
     * @param type     Returned object type.
     * @param options  If options are null, or there are not retrieve options defined, OneToOne entities
     *                 are loaded eagerly. Fetching mode update is skipped.
     */
    private void updateFetchingMode(Criteria criteria, Class<? extends ObjectType> type,
                                    Collection<SelectorOptions<GetOperationOptions>> options) {
        LOGGER.debug("Updating fetch mode for created criteria.");
        LOGGER.trace("Options for fetch mode {}.", new Object[]{options});

        List<SelectorOptions<GetOperationOptions>> retrieveOptions = RUtil.filterRetrieveOptions(options);
        if (retrieveOptions.isEmpty()) {
            // we don't need to touch fetch strategies if there are not custom retrieve options
            return;
        }

        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();
        EntityDefinition definition = registry.findDefinition(type, null, EntityDefinition.class);
        if (!hasToIncludeAll(retrieveOptions)) {
            //fetch mode cleanup
            for (Definition def : definition.getDefinitions()) {
                if (def instanceof EntityDefinition) {
                    EntityDefinition child = (EntityDefinition) def;
                    if (child.isEmbedded()) {
                        continue;
                    }

                    LOGGER.trace("Setting fetch mode for {} to {}.", new Object[]{child.getJpaName(), FetchMode.SELECT});
                    criteria.setFetchMode(child.getJpaName(), FetchMode.SELECT);
                }

                // there is not need to set fetch mode for properties, collections (already have fetch mode
                // SELECT by default). For AnyDefinition it will be implemented later, if necessary. For
                // reference it's not needed (they are in collections or they are embedded)
            }
        }

        //add fetch mode JOIN based on retrieve options
        for (SelectorOptions<GetOperationOptions> option : retrieveOptions) {
            ObjectSelector selector = option.getSelector();
            if (selector.getPath() == null || selector.getPath().size() != 1) {
                //fetching mode update for subcriteria will be supported later
                continue;
            }

            Definition def = definition.findDefinition(selector.getPath(), Definition.class);
            if (def == null) {
                continue;
            }

            if (!(def instanceof EntityDefinition)) {
                continue;
            }

            if (Set.class.equals(def.getJpaType())) {
                //we don't need to update fetch mode for collections
                continue;
            }

            GetOperationOptions opts = option.getOptions();
            FetchMode mode = RetrieveOption.INCLUDE == opts.getRetrieve() ? FetchMode.JOIN : FetchMode.SELECT;
            LOGGER.trace("Setting fetch mode for {} to {}.", new Object[]{def.getJpaName(), mode});
            criteria.setFetchMode(def.getJpaName(), mode);
        }
    }

    private boolean hasToIncludeAll(List<SelectorOptions<GetOperationOptions>> options) {
        for (SelectorOptions<GetOperationOptions> option : options) {
            if (!ItemPath.EMPTY_PATH.equals(option.getSelector().getPath())) {
                continue;
            }

            GetOperationOptions opt = option.getOptions();
            return opt.getRetrieve() == RetrieveOption.INCLUDE;
        }

        return false;
    }

    private Criteria interpretQuery(ObjectQuery query, Class<? extends ObjectType> type, PrismContext prismContext,
                                    Session session) throws QueryException {
        ObjectFilter filter = query.getFilter();
        try {
            QueryContext context = new QueryContext(this, type, prismContext, session);

            Restriction restriction = findAndCreateRestriction(filter, context, null, query);
            Criterion criterion = restriction.interpret(filter);

            Criteria criteria = context.getCriteria(null);
            criteria.add(criterion);

            return criteria;
        } catch (QueryException ex) {
            throw ex;
        } catch (Exception ex) {
            LOGGER.trace(ex.getMessage(), ex);
            throw new QueryException(ex.getMessage(), ex);
        }
    }

    public <T extends ObjectType> Criteria updatePagingAndSorting(Criteria query, Class<T> type, ObjectPaging paging) {
        if (paging == null) {
            return query;
        }
        if (paging.getOffset() != null) {
            query = query.setFirstResult(paging.getOffset());
        }
        if (paging.getMaxSize() != null) {
            query = query.setMaxResults(paging.getMaxSize());
        }

        if (paging.getDirection() == null && paging.getOrderBy() == null) {
            return query;
        }

        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();
        // PropertyPath path = new
        // XPathHolder(paging.getOrderBy()).toPropertyPath();
        if (paging.getOrderBy() == null) {
            LOGGER.warn("Ordering by property path with size not equal 1 is not supported '" + paging.getOrderBy()
                    + "'.");
            return query;
        }
        EntityDefinition definition = registry.findDefinition(type, null, EntityDefinition.class);
        Definition def = definition.findDefinition(paging.getOrderBy(), Definition.class);
        if (def == null) {
            LOGGER.warn("Unknown path '" + paging.getOrderBy() + "', couldn't find definition for it, "
                    + "list will not be ordered by it.");
            return query;
        }

        String propertyName = def.getJpaName();
        if (PolyString.class.equals(def.getJaxbType())) {
            propertyName += ".orig";
        }

        switch (paging.getDirection()) {
            case ASCENDING:
                query = query.addOrder(Order.asc(propertyName));
                break;
            case DESCENDING:
                query = query.addOrder(Order.desc(propertyName));
        }

        return query;
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

    public <T extends ObjectFilter> Restriction findAndCreateRestriction(T filter, QueryContext context,
                                                                         Restriction parent, ObjectQuery query)
            throws QueryException {

        for (Restriction restriction : AVAILABLE_RESTRICTIONS) {
            if (!restriction.canHandle(filter, context)) {
                continue;
            }

            Restriction<T> res = restriction.cloneInstance();
            res.setContext(context);
            res.setParent(parent);
            res.setQuery(query);

            return res;
        }

        LOGGER.error("Couldn't find proper restriction that can handle filter '{}'.", new Object[]{filter.dump()});
        throw new QueryException("Couldn't find proper restriction that can handle '" + filter + "'");
    }
}
