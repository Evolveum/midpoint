/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.repo.sql.query.custom;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.query.RQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.hibernate.Session;

import java.util.Collection;

/**
 * @author lazyman
 */
public abstract class CustomQuery {

    private SqlRepositoryConfiguration repoConfiguration;
    private PrismContext prismContext;

    protected SqlRepositoryConfiguration getRepoConfiguration() {
        return repoConfiguration;
    }

    protected PrismContext getPrismContext() {
        return prismContext;
    }

    public void init(SqlRepositoryConfiguration repoConfiguration, PrismContext prismContext) {
        this.repoConfiguration = repoConfiguration;
        this.prismContext = prismContext;
    }

    public abstract boolean match(ObjectQuery objectQuery, Class<? extends ObjectType> type,
                                  Collection<SelectorOptions<GetOperationOptions>> options, boolean countingObjects);

    public abstract RQuery createQuery(ObjectQuery objectQuery, Class<? extends ObjectType> type,
                                       Collection<SelectorOptions<GetOperationOptions>> options, boolean countingObjects,
                                       Session session);
}
