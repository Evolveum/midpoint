/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.repo.sql.query.custom;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.query.QueryDefinitionRegistry;
import com.evolveum.midpoint.repo.sql.query.RQuery;
import com.evolveum.midpoint.repo.sql.query.RQueryCriteriaImpl;
import com.evolveum.midpoint.repo.sql.query.definition.Definition;
import com.evolveum.midpoint.repo.sql.query.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.GetObjectResult;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;

import javax.xml.namespace.QName;

import java.util.Collection;

/**
 * A hack oriented towards queries in the form
 *  resourceRef = X && (attributes/uid = Y || attributes/name = Z)
 *
 * The reason is that default implementation of such queries returns shadows that comply with
 * both branches of the disjunction twice. Changing the way of dealing with such queries seriously
 * is not possible at this moment (moreover, it brings along unclear performance consequences).
 * Therefore this hack.
 *
 * @author mederly
 */
public class ShadowQueryWithDisjunction extends CustomQuery {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowQueryWithDisjunction.class);

    static private class ParsedQuery {
        RefFilter refFilter;
        EqualFilter eqUidFilter, eqNameFilter;
    }

    @Override
    public boolean match(ObjectQuery objectQuery, Class<? extends ObjectType> type,
                         Collection<SelectorOptions<GetOperationOptions>> options, boolean countingObjects) {

        if (!ShadowType.class.equals(type)) {
            return false;
        }
        return parse(objectQuery) != null;
    }

    private ParsedQuery parse(ObjectQuery objectQuery) {

        if (objectQuery == null || !(objectQuery.getFilter() instanceof AndFilter)) {
            return null;
        }

        LOGGER.info("create shadow disjunction parse");
        AndFilter andFilter = (AndFilter) objectQuery.getFilter();

        RefFilter refFilter = null;
        OrFilter orFilter = null;
        for (ObjectFilter filter : andFilter.getConditions()) {
            if (filter instanceof RefFilter) {
                refFilter = (RefFilter) filter;
            } else if (filter instanceof OrFilter) {
                orFilter = (OrFilter) filter;
            } else {
                return null;
            }
        }
        if (refFilter == null || orFilter == null) {
            return null;
        }
        if (!new ItemPath(ShadowType.F_RESOURCE_REF).equivalent(refFilter.getPath())) {
            return null;
        }
        if (refFilter.getValues() == null || refFilter.getValues().size() != 1) {
            return null;
        }

        EqualFilter eqUidFilter = null;
        EqualFilter eqNameFilter = null;
//        ItemPath uidPath = new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstantsGenerated.ICF_S_UID);
        if (orFilter.getConditions() != null && !orFilter.getConditions().isEmpty()){
        	if (orFilter.getConditions().size() != 2){
        		return null;
        	}
        }
        ItemPath namePath = new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstantsGenerated.ICF_S_NAME);
        for (ObjectFilter filter : orFilter.getConditions()) {
            if (!(filter instanceof EqualFilter)) {
                return null;
            }
            EqualFilter equalFilter = (EqualFilter) filter;
            if (namePath.equivalent(equalFilter.getPath())) {
            	eqNameFilter = equalFilter;
            } else if (ShadowType.F_ATTRIBUTES.equals(((NameItemPathSegment)equalFilter.getPath().first()).getName())) {
            	eqUidFilter = equalFilter;
            } else {
                return null;
            }
        }
        if (eqUidFilter == null || eqNameFilter == null) {
            return null;
        }
        if (eqUidFilter.getValues() == null || eqUidFilter.getValues().size() != 1) {
            return null;
        }
        if (eqNameFilter.getValues() == null || eqNameFilter.getValues().size() != 1) {
            return null;
        }
        ParsedQuery parsedQuery = new ParsedQuery();
        parsedQuery.refFilter = refFilter;
        parsedQuery.eqNameFilter = eqNameFilter;
        parsedQuery.eqUidFilter = eqUidFilter;
        return parsedQuery;
    }

    @Override
    public RQuery createQuery(ObjectQuery objectQuery, Class<? extends ObjectType> type,
                              Collection<SelectorOptions<GetOperationOptions>> options, boolean countingObjects,
                              Session session) {

        DetachedCriteria c1 = DetachedCriteria.forClass(ClassMapper.getHQLTypeClass(ShadowType.class), "s");
        c1.createCriteria("strings", "s1", JoinType.LEFT_OUTER_JOIN);

        ParsedQuery parsedQuery = parse(objectQuery);
        Conjunction conjunction = Restrictions.conjunction();
        conjunction.add(Restrictions.eq("resourceRef.targetOid", parsedQuery.refFilter.getValues().get(0).getOid()));
        Disjunction disjunction = Restrictions.disjunction();
        disjunction.add(createAttributeEq(parsedQuery.eqUidFilter, parsedQuery.eqUidFilter.getPath().lastNamed().getName()));
        disjunction.add(createAttributeEq(parsedQuery.eqNameFilter, SchemaConstantsGenerated.ICF_S_NAME));
        conjunction.add(disjunction);
        c1.add(conjunction);

        if (countingObjects) {
            c1.setProjection(Projections.countDistinct("s.oid"));
            return new RQueryCriteriaImpl(c1.getExecutableCriteria(session));
        }

        c1.setProjection(Projections.distinct(Projections.property("s.oid")));

        Criteria cMain = session.createCriteria(ClassMapper.getHQLTypeClass(ShadowType.class), "o");
        cMain.add(Subqueries.propertyIn("oid", c1));

        if (objectQuery != null && objectQuery.getPaging() != null) {
            cMain = updatePagingAndSorting(cMain, type, objectQuery.getPaging());
        }

        ProjectionList projections = Projections.projectionList();
        projections.add(Projections.property("fullObject"));
        projections.add(Projections.property("stringsCount"));
        projections.add(Projections.property("longsCount"));
        projections.add(Projections.property("datesCount"));
        projections.add(Projections.property("referencesCount"));
        projections.add(Projections.property("polysCount"));

        cMain.setProjection(projections);

        cMain.setResultTransformer(GetObjectResult.RESULT_TRANSFORMER);
        LOGGER.info("Criteria: {}", cMain);
        return new RQueryCriteriaImpl(cMain);
    }

    private Criterion createAttributeEq(EqualFilter attributeEqFilter, QName attributeName) {
        Conjunction conjunction = Restrictions.conjunction();
        conjunction.add(Restrictions.eq("s1.ownerType", RObjectExtensionType.ATTRIBUTES));
        conjunction.add(Restrictions.eq("s1.name", RUtil.qnameToString(attributeName)));
        conjunction.add(Restrictions.eq("s1.value", ((PrismPropertyValue) attributeEqFilter.getValues().get(0)).getValue()));
        return conjunction;
    }

    // copied from QueryInterpreter, todo refactor to some util class
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
}
