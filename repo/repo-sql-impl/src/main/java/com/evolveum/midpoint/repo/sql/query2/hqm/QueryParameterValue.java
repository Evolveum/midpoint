/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.repo.sql.query2.hqm;

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
