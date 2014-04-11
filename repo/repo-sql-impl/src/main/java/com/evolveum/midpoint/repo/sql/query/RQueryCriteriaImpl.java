/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query;

import org.apache.commons.lang.Validate;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

import java.util.List;

/**
 * @author lazyman
 */
public class RQueryCriteriaImpl implements RQuery {

    private Criteria criteria;

    public RQueryCriteriaImpl(Criteria criteria) {
        Validate.notNull(criteria, "Criteria must not be null.");
        this.criteria = criteria;
    }

    @Override
    public List list() throws HibernateException {
        return criteria.list();
    }

    @Override
    public Object uniqueResult() throws HibernateException {
        return criteria.uniqueResult();
    }

    @Override
    public ScrollableResults scroll(ScrollMode mode) throws HibernateException {
        return criteria.scroll(mode);
    }

    public Criteria getCriteria() {
        return criteria;
    }
}
