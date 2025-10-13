/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.Interceptor;

/**
 * @author lazyman
 */
public class EntityStateInterceptor implements Interceptor {

    @Override
    public Boolean isTransient(Object entity) {
        if (entity instanceof EntityState) {
            return isTransient((EntityState) entity);
        }

        return null;
    }

    private Boolean isTransient(EntityState object) {
        return isTransient(object, false);
    }

    @SuppressWarnings("SameParameterValue")
    private Boolean isTransient(EntityState object, boolean isObjectMyParent) {
        Boolean trans = object != null ? object.isTransient() : null;
        if (!isObjectMyParent) {
            return trans;
        }
        if (Boolean.TRUE.equals(trans)) {
            return true;
        }

        return null;
    }
}
