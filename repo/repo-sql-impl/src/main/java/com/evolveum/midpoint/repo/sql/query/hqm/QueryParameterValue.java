/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.hqm;

import org.hibernate.type.Type;

/**
 * @author mederly
 */
public class QueryParameterValue {

    private Object value;
    private Type type;

    public QueryParameterValue(Object value, Type type) {
        this.value = value;
        this.type = type;
    }

    public QueryParameterValue(Object value) {
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public String debugDump() {
        StringBuilder sb = new StringBuilder();
        sb.append(value);
        if (value instanceof Enum) {
            sb.append(" (");
            sb.append(value.getClass().getName());
            sb.append('.');
            sb.append(((Enum) value).name());
            sb.append(')');
        }
        if (type != null) {
            sb.append(" (type = ");
            sb.append(type);
            sb.append(")");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return debugDump();
    }
}
