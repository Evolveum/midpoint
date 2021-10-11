/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
