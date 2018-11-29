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

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Collections;
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
            definitions = new ArrayList<>();
        }

        return Collections.unmodifiableList(definitions);
    }

    public void addDefinition(Definition definition) {
        if (definitions == null) {
            definitions = new ArrayList<>();
        }

        Definition oldDef = findLocalDefinition(definition.getJaxbName(), Definition.class);
        if (oldDef != null) {
            definitions.remove(oldDef);
        }
        definitions.add(definition);

        Collections.sort(definitions, new DefinitionComparator());
    }

    @Override
    protected void toStringExtended(StringBuilder builder) {
        builder.append(", embedded=").append(isEmbedded());
        builder.append(", definitions=[");

        List<Definition> definitions = getDefinitions();
        for (Definition definition : definitions) {
            builder.append(definition.getDebugDumpClassName());
            builder.append('(').append(dumpQName(definition.getJaxbName())).append(')');
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
        if (ItemPath.isEmpty(path)) {
            if (type.isAssignableFrom(EntityDefinition.class)) {
                return (D) this;
            } else {
                return null;
            }
        }

        ItemName firstName = path.firstToName();
        ItemPath rest = path.rest();
        if (QNameUtil.match(ObjectType.F_METADATA, firstName)) {
            //metadata is not an repository entity
            firstName = rest.firstToName();
            rest = rest.rest();
        } else if (QNameUtil.match(AssignmentType.F_CONSTRUCTION, firstName) &&
                rest.startsWithName(ConstructionType.F_RESOURCE_REF)) {
            // ugly hack: construction/resourceRef -> resourceRef
            firstName = rest.firstToName();
            rest = rest.rest();
        }

        if (rest.isEmpty()) {
            return findLocalDefinition(firstName, type);
        } else {
            Definition def = findLocalDefinition(firstName, Definition.class);
            if (def instanceof CollectionDefinition) {
                CollectionDefinition collDef = (CollectionDefinition) def;
                def = collDef.getDefinition();
            }
            if (def instanceof EntityDefinition) {
                EntityDefinition nextEntity = (EntityDefinition) def;
                return nextEntity.findDefinition(rest, type);
            } else {
                return null;
            }
        }
    }

    @Override
    public <D extends Definition> D findLocalDefinition(@NotNull QName jaxbName, @NotNull Class<D> type) {

        for (Definition definition : getDefinitions()) {
        	//TODO: using match instead of strict equals..is this OK for repository??this is the situation, we have "common" namepsace
            if (QNameUtil.match(jaxbName, definition.getJaxbName()) && type.isAssignableFrom(definition.getClass())) {
                return (D) definition;
            }
        }
        return null;
    }
}
