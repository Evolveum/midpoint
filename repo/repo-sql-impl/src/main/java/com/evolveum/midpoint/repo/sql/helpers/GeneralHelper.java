/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers;

import com.evolveum.midpoint.repo.sql.util.RUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.exception.SchemaException;

@Component
public class GeneralHelper {

    public int findLastIdInRepo(EntityManager em, String tableOid, String queryName) {
        Query query = em.createNamedQuery(queryName);
        query.setParameter("oid", tableOid);
        Integer lastId = RUtil.getSingleResultOrNull(query);
        if (lastId == null) {
            lastId = 0;
        }
        return lastId;
    }

    public <C extends Containerable> void validateContainerable(C value, Class<C> type)
            throws SchemaException {
        if (value == null) {
            throw new SchemaException("Null object as a result of repository get operation for " + type);
        }
        Class<? extends Containerable> realType = value.getClass();
        if (!type.isAssignableFrom(realType)) {
            throw new SchemaException("Expected to find '" + type.getSimpleName() + "' but found '" + realType.getSimpleName());
        }
        // TODO call check consistence if possible
    }

}
