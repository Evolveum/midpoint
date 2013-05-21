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

package com.evolveum.midpoint.repo.sql.query.definition;

import com.evolveum.midpoint.util.DebugDumpable;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class CollectionDefinition extends Definition {

    private Definition definition;

    public CollectionDefinition(QName jaxbName, Class jaxbType, String propertyName, Class propertyType) {
        super(jaxbName, jaxbType, propertyName, propertyType);
    }

    public Definition getDefinition() {
        return definition;
    }

    void setDefinition(Definition definition) {
        this.definition = definition;
    }

    @Override
    protected void toStringExtended(StringBuilder builder) {
        String def = definition != null ? definition.toString() : null;
        builder.append(", def=").append(def);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(DebugDumpable.INDENT_STRING);
        }
        sb.append(toString());

        sb.append('\n');
        String def = null;
        if (definition == null) {
            for (int i = 0; i < indent + 1; i++) {
                sb.append(DebugDumpable.INDENT_STRING);
            }
        } else {
            def = definition.debugDump(indent + 1);
        }
        sb.append(def);

        return sb.toString();
    }

    @Override
    protected String getDebugDumpClassName() {
        return "Col";
    }
}
