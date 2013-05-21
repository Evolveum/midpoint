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

package com.evolveum.midpoint.repo.api.query;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class Query {

    private String description;
    private QName type;
    private QueryFilter filter;

    public String getDescription() {
        return description;
    }

    public QName getType() {
        return type;
    }

    public QueryFilter getFilter() {
        return filter;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFilter(QueryFilter filter) {
        this.filter = filter;
    }

    public void setType(QName type) {
        this.type = type;
    }
}
