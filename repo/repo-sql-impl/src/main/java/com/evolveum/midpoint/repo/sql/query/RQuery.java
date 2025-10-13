/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query;

import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

import java.util.List;

/**
 * @author lazyman
 */
public interface RQuery {

    <T> List<T> list() throws HibernateException;

    <T> T uniqueResult() throws HibernateException;

    ScrollableResults scroll(ScrollMode mode) throws HibernateException;
}
