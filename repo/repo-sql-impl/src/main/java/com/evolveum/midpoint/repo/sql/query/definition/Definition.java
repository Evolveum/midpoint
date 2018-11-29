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

package com.evolveum.midpoint.repo.sql.query.definition;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public abstract class Definition implements DebugDumpable {

    //jaxb
    private QName jaxbName;
    private Class jaxbType;
    //jpa
    private String jpaName;
    private Class jpaType;

    public Definition(QName jaxbName, Class jaxbType, String jpaName, Class jpaType) {
        this.jaxbName = jaxbName;
        this.jaxbType = jaxbType;
        this.jpaName = jpaName;
        this.jpaType = jpaType;
    }

    public QName getJaxbName() {
        return jaxbName;
    }

    public Class getJaxbType() {
        return jaxbType;
    }

    public String getJpaName() {
        return jpaName;
    }

    public Class getJpaType() {
        return jpaType;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getDebugDumpClassName());
        builder.append('{');
        builder.append("jaxbN=").append(dumpQName(jaxbName));
        builder.append(", jaxbT=").append((jaxbType != null ? jaxbType.getSimpleName() : ""));
        builder.append(", jpaN=").append(jpaName);
        builder.append(", jpaT=").append((jpaType != null ? jpaType.getSimpleName() : ""));
        toStringExtended(builder);
        builder.append('}');

        return builder.toString();
    }

    protected void toStringExtended(StringBuilder builder) {

    }

    protected String dumpQName(QName qname) {
        if (qname == null) {
            return null;
        }

        String namespace = qname.getNamespaceURI();
        namespace = namespace.replaceFirst("http://midpoint\\.evolveum\\.com/xml/ns/public", "..");

        StringBuilder builder = new StringBuilder();
        builder.append('{');
        builder.append(namespace);
        builder.append('}');
        builder.append(qname.getLocalPart());
        return builder.toString();
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(DebugDumpable.INDENT_STRING);
        }
        sb.append(toString());
        return sb.toString();
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    protected abstract String getDebugDumpClassName();

    public <D extends Definition> D findDefinition(ItemPath path, Class<D> type) {
        return null;
    }

    public <D extends Definition> D findLocalDefinition(QName jaxbName, Class<D> type) {
        return null;
    }
}
