/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query2.hqm.condition;

import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import org.apache.commons.lang.Validate;

/**
 * @author mederly
 */
public class HibernateSubquery extends HibernateQuery {

    private HibernateQuery parentQuery;

    public HibernateSubquery(JpaEntityDefinition primaryEntityDef, HibernateQuery parentQuery) {
        super(primaryEntityDef);
        Validate.notNull(parentQuery);
        this.parentQuery = parentQuery;
    }

    @Override
    public RootHibernateQuery getRootQuery() {
        return parentQuery.getRootQuery();
    }
}
