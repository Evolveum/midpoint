/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.query;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lazyman
 */
public class EntityDefinition extends Definition implements DebugDumpable {

    private Map<QName, Definition> definitions = new HashMap<QName, Definition>();
    private boolean any;
    private boolean embedded;

    public boolean isAny() {
        return any;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public void setEmbedded(boolean embedded) {
        this.embedded = embedded;
    }

    public void setAny(boolean any) {
        this.any = any;
    }

    @Override
    public Definition findDefinition(QName qname) {
        return findDefinition(qname, Definition.class);
    }

    @Override
    public <T extends Definition> T findDefinition(QName qname, Class<T> type) {
        Validate.notNull(qname, "QName must not be null.");
        Validate.notNull(type, "Class type must not be null.");

        Definition definition = definitions.get(qname);
        if (definition == null) {
            return null;
        }

        if (type.isAssignableFrom(definition.getClass())) {
            return (T) definition;
        }

        return null;
    }

    @Override
    public boolean isEntity() {
        return true;
    }

    void putDefinition(QName qname, Definition definition) {
        definitions.put(qname, definition);
    }

    @Override
    public String toString() {
        return debugDump();
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
        sb.append("n: ");
        sb.append(getName());
        sb.append(", t: ");
        sb.append(getType());
        sb.append(", a: ");
        sb.append(isAny());
        sb.append(", e: ");
        sb.append(isEmbedded());
        sb.append("\n");
        DebugUtil.debugDumpMapMultiLine(sb, definitions, indent + 1);
        return sb.toString();
    }
}
