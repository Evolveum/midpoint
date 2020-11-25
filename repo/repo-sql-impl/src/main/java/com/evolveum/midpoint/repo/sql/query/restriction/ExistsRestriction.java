/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.ExistsFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sql.query.QueryInterpreter;
import com.evolveum.midpoint.repo.sql.query.definition.JpaDataNodeDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.JpaPropertyDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query.resolution.HqlDataInstance;
import com.evolveum.midpoint.repo.sql.query.resolution.HqlEntityInstance;
import com.evolveum.midpoint.repo.sql.query.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;

/**
 * @author mederly
 */
public class ExistsRestriction extends ItemRestriction<ExistsFilter> {

    public ExistsRestriction(InterpretationContext context, ExistsFilter filter, JpaEntityDefinition baseEntityDefinition,
                             Restriction parent) {
        super(context, filter, filter.getFullPath(), null, baseEntityDefinition, parent);
    }

    @Override
    public Condition interpret() throws QueryException {
        HqlDataInstance dataInstance = getItemPathResolver()
                .resolveItemPath(filter.getFullPath(), filter.getDefinition(), getBaseHqlEntity(), false);

        boolean isAll = filter.getFilter() == null || filter.getFilter() instanceof AllFilter;
        JpaDataNodeDefinition jpaDefinition = dataInstance.getJpaDefinition();
        if (!isAll) {
            if (!(jpaDefinition instanceof JpaEntityDefinition)) {    // partially checked already (for non-null-ness)
                 throw new QueryException("ExistsRestriction with non-empty subfilter points to non-entity node: " + jpaDefinition);
            }
            setHqlDataInstance(dataInstance);
            QueryInterpreter interpreter = context.getInterpreter();
            return interpreter.interpretFilter(context, filter.getFilter(), this);
        } else if (jpaDefinition instanceof JpaPropertyDefinition && (((JpaPropertyDefinition) jpaDefinition).isCount())) {
            RootHibernateQuery hibernateQuery = context.getHibernateQuery();
            return hibernateQuery.createSimpleComparisonCondition(dataInstance.getHqlPath(), 0, ">");
        } else {
            // TODO support exists also for other properties (single valued or multi valued)
            throw new UnsupportedOperationException("Exists filter with 'all' subfilter is currently not supported");
        }
    }

    @Override
    public HqlEntityInstance getBaseHqlEntityForChildren() {
        return hqlDataInstance.asHqlEntityInstance();
    }
}
