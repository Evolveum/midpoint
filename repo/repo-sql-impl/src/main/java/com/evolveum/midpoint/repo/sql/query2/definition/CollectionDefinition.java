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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.query2.definition;

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
