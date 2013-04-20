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

package com.evolveum.midpoint.repo.sql.query.definition;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.util.DebugDumpable;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class EntityDefinition extends Definition {

    /**
     * child definitions of this entity
     */
    private List<Definition> definitions;
    private boolean embedded;

    public EntityDefinition(QName jaxbName, Class jaxbType, String jpaName, Class jpaType) {
        super(jaxbName, jaxbType, jpaName, jpaType);
    }

    public boolean isEmbedded() {
        return embedded;
    }

    void setEmbedded(boolean embedded) {
        this.embedded = embedded;
    }

    public List<Definition> getDefinitions() {
        if (definitions == null) {
            definitions = new ArrayList<Definition>();
        }
        return definitions;
    }

    @Override
    protected void toStringExtended(StringBuilder builder) {
        builder.append(", embedded=").append(isEmbedded());
        builder.append(", definitions=[");

        List<Definition> definitions = getDefinitions();
        for (Definition definition : definitions) {
            builder.append(definition.getDebugDumpClassName());
            builder.append('(').append(dumpQName(getJaxbName())).append(')');
        }
        builder.append(']');
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(DebugDumpable.INDENT_STRING);
        }
        sb.append(toString());

        List<Definition> definitions = getDefinitions();
        for (Definition definition : definitions) {
            sb.append(definition.debugDump(indent + 1));
            if (definitions.indexOf(definition) != definitions.size() - 1) {
                sb.append('\n');
            }
        }

        return sb.toString();
    }

    @Override
    protected String getDebugDumpClassName() {
        return "Ent";
    }

    @Override
    public <D extends Definition> D findDefinition(ItemPath path, Class<D> type) {
        if (path == null || path.isEmpty()) {
            if (type.isAssignableFrom(EntityDefinition.class)) {
                return (D) this;
            }
        }

        NameItemPathSegment first = (NameItemPathSegment) path.first();
        ItemPath tail = path.tail();

        if (tail.isEmpty()) {
            return findDefinition(first.getName(), type);
        } else {
            EntityDefinition nextEntity = findDefinition(first.getName(), EntityDefinition.class);
            if (nextEntity != null) {
                return nextEntity.findDefinition(tail, type);
            }
        }

        return null;
    }

    @Override
    public <D extends Definition> D findDefinition(QName jaxbName, Class<D> type) {
        Validate.notNull(jaxbName, "Jaxb name must not be null.");
        Validate.notNull(type, "Definition type must not be null.");

        for (Definition definition : getDefinitions()) {
            if (!jaxbName.equals(definition.getJaxbName())) {
                continue;
            }

            if (type.isAssignableFrom(definition.getClass())) {
                return (D) definition;
            }
        }

        return null;
    }
}
