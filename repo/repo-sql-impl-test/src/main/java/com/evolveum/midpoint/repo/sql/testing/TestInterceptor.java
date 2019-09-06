/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.testing;

import com.evolveum.midpoint.repo.sql.util.EntityStateInterceptor;
import org.hibernate.EmptyInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TestInterceptor extends EmptyInterceptor {

    @Autowired
    private EntityStateInterceptor entityStateInterceptor;

    @Override
    public Boolean isTransient(Object entity) {
        return entityStateInterceptor.isTransient(entity);
    }
}
