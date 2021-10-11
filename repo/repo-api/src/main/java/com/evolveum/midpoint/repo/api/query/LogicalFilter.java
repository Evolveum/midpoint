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
public abstract class LogicalFilter implements QueryFilter {

    private QName type;

    public LogicalFilter() {
        this(null);
    }

    public LogicalFilter(QName type) {
        this.type = type;
    }

    @Override
    public QName getType() {
        return type;
    }

    @Override
    public void setType(QName type) {
        this.type = type;
    }
}
