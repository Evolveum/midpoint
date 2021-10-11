/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.spi.PersisterCreationContext;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * See MidPointPersisterUtil comments.
 *
 * @author mederly
 */
public class MidPointJoinedPersister extends JoinedSubclassEntityPersister {

    public MidPointJoinedPersister(PersistentClass persistentClass, EntityRegionAccessStrategy cacheAccessStrategy,
                                   NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
                                   PersisterCreationContext creationContext) throws HibernateException {
        super(persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext);
    }

    @Override
    public Object[] hydrate(ResultSet rs, Serializable id, Object object, Loadable rootLoadable,
                            String[][] suffixedPropertyColumns, boolean allProperties,
                            SharedSessionContractImplementor session) throws SQLException, HibernateException {

        Object[] values = super.hydrate(rs, id, object, rootLoadable, suffixedPropertyColumns, allProperties, session);
        MidpointPersisterUtil.killUnwantedAssociationValues(getPropertyNames(), getPropertyTypes(), values);
        return values;
    }

}
