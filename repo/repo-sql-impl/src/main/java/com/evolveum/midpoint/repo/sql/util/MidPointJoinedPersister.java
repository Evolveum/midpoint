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

package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;
import org.hibernate.persister.entity.Loadable;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * See MidPointPersisterUtil comments.
 *
 * @author mederly
 */
public class MidPointJoinedPersister extends JoinedSubclassEntityPersister {

    public MidPointJoinedPersister(PersistentClass persistentClass, EntityRegionAccessStrategy cacheAccessStrategy, NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy, SessionFactoryImplementor factory, Mapping mapping) throws HibernateException {
        super(persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, factory, mapping);
    }

    public MidPointJoinedPersister(EntityBinding entityBinding, EntityRegionAccessStrategy cacheAccessStrategy, NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy, SessionFactoryImplementor factory, Mapping mapping) throws HibernateException {
        super(entityBinding, cacheAccessStrategy, naturalIdRegionAccessStrategy, factory, mapping);
    }

    @Override
    public Object[] hydrate(ResultSet rs, Serializable id, Object object, Loadable rootLoadable, String[][] suffixedPropertyColumns, boolean allProperties, SessionImplementor session) throws SQLException, HibernateException {
        Object[] values = super.hydrate(rs, id, object, rootLoadable, suffixedPropertyColumns, allProperties, session);
        MidpointPersisterUtil.killUnwantedAssociationValues(getPropertyNames(), getPropertyTypes(), values);
        return values;
    }
}
