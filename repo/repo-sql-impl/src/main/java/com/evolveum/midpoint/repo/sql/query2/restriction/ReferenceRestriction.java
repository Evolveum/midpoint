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

package com.evolveum.midpoint.repo.sql.query2.restriction;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.repo.sql.data.common.ObjectReference;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaReferenceDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaLinkDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.AndCondition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author lazyman
 */
public class ReferenceRestriction extends ItemValueRestriction<RefFilter> {

    private static final Trace LOGGER = TraceManager.getTrace(ReferenceRestriction.class);

    // Definition of the item being queried.
    private final JpaLinkDefinition<JpaReferenceDefinition> linkDefinition;

    public ReferenceRestriction(InterpretationContext context, RefFilter filter, JpaEntityDefinition baseEntityDefinition,
                                Restriction parent, JpaLinkDefinition<JpaReferenceDefinition> linkDefinition) {
        super(context, filter, baseEntityDefinition, parent);
        Validate.notNull(linkDefinition, "linkDefinition");
        this.linkDefinition = linkDefinition;
    }

    @Override
    public Condition interpretInternal() throws QueryException {

        String hqlPath = getHqlDataInstance().getHqlPath();
        LOGGER.trace("interpretInternal starting with hqlPath = {}", hqlPath);

        List<? extends PrismValue> values = filter.getValues();
        if (values != null && values.size() > 1) {
            throw new QueryException("Ref filter '" + filter + "' contain more than one reference value (which is not supported for now).");
        }
        PrismReferenceValue refValue = null;
        if (values != null && !values.isEmpty()) {
            refValue = (PrismReferenceValue) values.get(0);
        }

        InterpretationContext context = getContext();
        RootHibernateQuery hibernateQuery = context.getHibernateQuery();

        String refValueOid = null;
        QName refValueRelation = null;
        QName refValueTargetType = null;
        if (refValue != null) {
        	refValueOid = refValue.getOid();
        	refValueRelation = refValue.getRelation();
        	refValueTargetType = refValue.getTargetType();
        }
        AndCondition conjunction = hibernateQuery.createAnd();
        conjunction.add(handleEqOrNull(hibernateQuery, hqlPath + "." + ObjectReference.F_TARGET_OID, refValueOid));

        if (refValueOid != null) {
	        if (refValueRelation == null) {
	        	// Return only references without relation
	        	conjunction.add(hibernateQuery.createEq(hqlPath + "." + ObjectReference.F_RELATION, RUtil.QNAME_DELIMITER));
	        } else if (refValueRelation.equals(PrismConstants.Q_ANY)) {
	        	// Return all relations => no restriction
	        } else {
	        	// return references with specific relation
	            conjunction.add(handleEqOrNull(hibernateQuery, hqlPath + "." + ObjectReference.F_RELATION, RUtil.qnameToString(refValueRelation)));
	        }
	
	        if (refValueTargetType != null) {
	            conjunction.add(handleEqOrNull(hibernateQuery, hqlPath + "." + ObjectReference.F_TYPE,
	                    ClassMapper.getHQLTypeForQName(refValueTargetType)));
	        }
        }

        // TODO what about isNotNull if necessary ?

        return conjunction;
    }


    private Condition handleEqOrNull(RootHibernateQuery hibernateQuery, String propertyName, Object value) {
        if (value == null) {
            return hibernateQuery.createIsNull(propertyName);
        } else {
            return hibernateQuery.createEq(propertyName, value);
        }
    }
}
