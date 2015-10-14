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

package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.query.QueryContext;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import org.hibernate.criterion.Criterion;

/**
 * @author lazyman
 */
public abstract class Restriction<T extends ObjectFilter> {

    protected QueryContext context;
    protected Restriction parent;
    protected T filter;

    public T getFilter() {
        return filter;
    }

    public void setFilter(T filter) {
        this.filter = filter;
    }

    public QueryContext getContext() {
        return context;
    }

    public void setContext(QueryContext context) {
        this.context = context;
    }

    public Restriction getParent() {
        return parent;
    }

    public void setParent(Restriction parent) {
        this.parent = parent;
    }

    public abstract Criterion interpret() throws QueryException;

    public abstract boolean canHandle(ObjectFilter filter) throws QueryException;

    // todo don't know if cloning is necessary... [lazyman]
    // this can be replaced probably by simple java reflection call
    public abstract Restriction newInstance();
}
