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
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
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
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;

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

    private SqlRepositoryConfiguration repoConfiguration;

    public QueryInterpreter(SqlRepositoryConfiguration repoConfiguration) {
        this.repoConfiguration = repoConfiguration;
    }

    public SqlRepositoryConfiguration getRepoConfiguration() {
        return repoConfiguration;
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

        Criteria criteria;
        if (query != null && query.getFilter() != null) {
            criteria = interpretQuery(query, type, prismContext, session);
        } else {
            criteria = session.createCriteria(ClassMapper.getHQLTypeClass(type));
        }

        if (!countingObjects && query != null && query.getPaging() != null) {
            criteria = updatePagingAndSorting(criteria, type, query.getPaging());
        }

      if (!countingObjects) {
            ProjectionList projections = Projections.projectionList();
            projections.add(Projections.property("fullObject"));

            projections.add(Projections.property("stringsCount"));
            projections.add(Projections.property("longsCount"));
            projections.add(Projections.property("datesCount"));
            projections.add(Projections.property("referencesCount"));
            projections.add(Projections.property("polysCount"));

            criteria.setProjection(projections);
        }

        return criteria;
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

        if (paging.getDirection() != null) {
            switch (paging.getDirection()) {
                case ASCENDING:
                    query = query.addOrder(Order.asc(propertyName));
                    break;
                case DESCENDING:
                    query = query.addOrder(Order.desc(propertyName));
                    break;
            }
        } else {
            query = query.addOrder(Order.asc(propertyName));
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

        LOGGER.error("Couldn't find proper restriction that can handle filter '{}'.", new Object[]{filter.debugDump()});
        throw new QueryException("Couldn't find proper restriction that can handle '" + filter + "'");
    }
}
