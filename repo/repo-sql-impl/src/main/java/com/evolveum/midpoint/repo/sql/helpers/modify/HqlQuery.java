/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.repo.sql.helpers.modify;

import com.evolveum.midpoint.prism.path.ItemPath;
import org.hibernate.query.QueryParameter;

import java.util.Map;

/**
 * Created by Viliam Repan (lazyman).
 */
public class HqlQuery {

    private ItemPath identifier;

    private String query;
    private Map<String, QueryParameter> parameters;

    public HqlQuery(ItemPath identifier, String query, Map<String, QueryParameter> parameters) {
        this.identifier = identifier;
        this.query = query;
        this.parameters = parameters;
    }

    public ItemPath getIdentifier() {
        return identifier;
    }

    public String getQuery() {
        return query;
    }

    public Map<String, QueryParameter> getParameters() {
        return parameters;
    }
}
