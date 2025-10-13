/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.hqm;

import org.hibernate.type.Type;

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
