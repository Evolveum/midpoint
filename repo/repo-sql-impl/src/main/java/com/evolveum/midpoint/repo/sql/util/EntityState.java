/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.util;

/**
 * @author lazyman
 */
public interface EntityState {

    /**
     * Tells hibernate {@link org.hibernate.Interceptor} that entity is transient, so that hibernate session
     * doesn't need to verify it using select queries.
     *
     * @return true if entity is transient
     */
    Boolean isTransient();

    void setTransient(Boolean trans);
}
