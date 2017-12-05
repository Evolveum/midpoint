/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.repo.sql.query2.restriction;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtReference;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaAnyReferenceDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaReferenceDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaLinkDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.AndCondition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.OrCondition;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;

import static com.evolveum.midpoint.repo.sql.util.RUtil.qnameToString;

/**
 * @author lazyman
 */
public class ReferenceRestriction extends ItemValueRestriction<RefFilter> {

    private static final Trace LOGGER = TraceManager.getTrace(ReferenceRestriction.class);

    // Definition of the item being queried.
    @NotNull private final JpaLinkDefinition<JpaReferenceDefinition> linkDefinition;

    public ReferenceRestriction(InterpretationContext context, RefFilter filter, JpaEntityDefinition baseEntityDefinition,
                                Restriction parent, @NotNull JpaLinkDefinition<JpaReferenceDefinition> linkDefinition) {
        super(context, filter, baseEntityDefinition, parent);
        this.linkDefinition = linkDefinition;
    }

    @Override
    public Condition interpretInternal() throws QueryException {

		String hqlPath = hqlDataInstance.getHqlPath();
		LOGGER.trace("interpretInternal starting with hqlPath = {}", hqlPath);

		RootHibernateQuery hibernateQuery = context.getHibernateQuery();

		List<PrismReferenceValue> values = filter.getValues();
		if (CollectionUtils.isEmpty(values)) {
			return hibernateQuery.createIsNull(hqlDataInstance.getHqlPath());
		}
		Set<String> oids = new HashSet<>();
		Set<QName> relations = new HashSet<>();
		Set<QName> targetTypes = new HashSet<>();
		for (PrismReferenceValue value : values) {
			if (value.getOid() == null) {
				throw new QueryException("Null OID is not allowed in the reference query. Use empty reference list if needed.");
			}
			oids.add(value.getOid());
			if (value.getRelation() == null) {
				relations.add(SchemaConstants.ORG_DEFAULT);
			} else {
				// we intentionally don't normalize relations namespaces, to be able to do namespace-insensitive searches
				// so the caller is responsible to unify namespaces if he needs to optimize queries (use IN instead of OR)
				relations.add(value.getRelation());
			}
			targetTypes.add(qualifyTypeName(value.getTargetType()));
		}

		if (relations.size() > 1 || targetTypes.size() > 1) {
			// we must use 'OR' clause
			OrCondition rootOr = hibernateQuery.createOr();
			values.forEach(prv -> rootOr
					.add(createRefCondition(hibernateQuery, Collections.singleton(prv.getOid()), prv.getRelation(), prv.getTargetType())));
			return rootOr;
		} else {
			return createRefCondition(hibernateQuery, oids, MiscUtil.extractSingleton(relations), MiscUtil.extractSingleton(targetTypes));
		}
	}

	private QName qualifyTypeName(QName typeName) throws QueryException {
    	if (typeName != null) {
			try {
				return context.getPrismContext().getSchemaRegistry().qualifyTypeName(typeName);
			} catch (SchemaException e) {
				throw new QueryException("Cannot qualify name of the target type: " + typeName + ": " + e.getMessage(), e);
			}
		} else {
    		return null;
		}
	}

	private Condition createRefCondition(RootHibernateQuery hibernateQuery,
			Collection<String> oids, QName relation, QName targetType) {
		String hqlPath = hqlDataInstance.getHqlPath();

		final String TARGET_OID_HQL_PROPERTY, RELATION_HQL_PROPERTY, TARGET_TYPE_HQL_PROPERTY;
		if (linkDefinition.getTargetDefinition() instanceof JpaAnyReferenceDefinition) {
			TARGET_OID_HQL_PROPERTY = ROExtReference.F_TARGET_OID;
			RELATION_HQL_PROPERTY = ROExtReference.F_RELATION;
			TARGET_TYPE_HQL_PROPERTY = ROExtReference.F_TARGET_TYPE;
		} else {
			TARGET_OID_HQL_PROPERTY = RObjectReference.F_TARGET_OID;
			RELATION_HQL_PROPERTY = RObjectReference.F_RELATION;
			TARGET_TYPE_HQL_PROPERTY = RObjectReference.F_TARGET_TYPE;
		}

        AndCondition conjunction = hibernateQuery.createAnd();
        conjunction.add(hibernateQuery.createEqOrInOrNull(hqlDataInstance.getHqlPath() + "." + TARGET_OID_HQL_PROPERTY, oids));

		if (ObjectTypeUtil.isDefaultRelation(relation)) {
			// Return references without relation or with "member" relation
			conjunction.add(hibernateQuery.createIn(hqlPath + "." + RELATION_HQL_PROPERTY,
					Arrays.asList(RUtil.QNAME_DELIMITER, qnameToString(SchemaConstants.ORG_DEFAULT))));
		} else if (QNameUtil.match(relation, PrismConstants.Q_ANY)) {
			// Return all relations => no restriction
		} else {
			// return references with specific relation
			List<String> relationsToTest = new ArrayList<>();
			relationsToTest.add(qnameToString(relation));
			if (QNameUtil.noNamespace(relation)) {
				relationsToTest.add(qnameToString(QNameUtil.setNamespaceIfMissing(relation, SchemaConstants.NS_ORG, null)));
			} else if (SchemaConstants.NS_ORG.equals(relation.getNamespaceURI())) {
				relationsToTest.add(qnameToString(new QName(relation.getLocalPart())));
			} else {
				// non-empty non-standard NS => nothing to add
			}
			conjunction.add(hibernateQuery.createEqOrInOrNull(hqlPath + "." + RELATION_HQL_PROPERTY, relationsToTest));
		}

		if (targetType != null) {
			conjunction.add(handleEqInOrNull(hibernateQuery, hqlPath + "." + TARGET_TYPE_HQL_PROPERTY,
					ClassMapper.getHQLTypeForQName(targetType)));
		}
        return conjunction;
    }

	private Condition handleEqInOrNull(RootHibernateQuery hibernateQuery, String propertyName, Object value) {
        if (value == null) {
            return hibernateQuery.createIsNull(propertyName);
        } else {
            return hibernateQuery.createEq(propertyName, value);
        }
    }
}
