/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.spi.PersisterCreationContext;

/**
 * See MidPointPersisterUtil comments.
 */
public class MidPointSingleTablePersister extends SingleTableEntityPersister {

    public MidPointSingleTablePersister(PersistentClass persistentClass, EntityDataAccess cacheAccessStrategy,
            NaturalIdDataAccess naturalIdRegionAccessStrategy,
            PersisterCreationContext creationContext) throws HibernateException {
        super(persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext);
    }

    // FIXME: Find hydrate alternative
    /*
    @Override
    public Object[] hydrate(ResultSet rs, Serializable id, Object object, Loadable rootLoadable,
            String[][] suffixedPropertyColumns, boolean allProperties,
            SharedSessionContractImplementor session) throws SQLException, HibernateException {

        Object[] values = super.hydrate(rs, id, object, rootLoadable, suffixedPropertyColumns, allProperties, session);
        MidpointPersisterUtil.killUnwantedAssociationValues(getPropertyNames(), getPropertyTypes(), values);
        return values;
    }*/
}
