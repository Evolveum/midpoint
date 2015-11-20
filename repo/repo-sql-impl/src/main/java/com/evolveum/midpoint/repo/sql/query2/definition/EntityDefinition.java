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

package com.evolveum.midpoint.repo.sql.query2.definition;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.repo.sql.query.restriction.PathTranslation;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang.Validate;

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
        Definition oldDef = findDefinition(definition.getJaxbName(), Definition.class);
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
    public <D extends Definition> D findDefinition(QName jaxbName, Class<D> type) {
        Validate.notNull(jaxbName, "Jaxb name must not be null.");
        Validate.notNull(type, "Definition type must not be null.");

        for (Definition definition : getDefinitions()) {
        	if (!QNameUtil.match(jaxbName, definition.getJaxbName())){
        		continue;
        	}
            if (type.isAssignableFrom(definition.getClass())) {
                return (D) definition;
            }
        }

        return null;
    }

    @Override
    public Definition nextDefinition(Holder<ItemPath> pathHolder) {
        ItemPath path = pathHolder.getValue();

        for (;;) {
            if (path == null || path.isEmpty()) {
                return this;        // in some cases (metadata, construction) this return value might be suspicious, or altogether wrong -- but we don't care
            }

            NameItemPathSegment first = (NameItemPathSegment) path.first();
            QName firstName = first.getName();

            path = path.tail();
            pathHolder.setValue(path);

            // just a lookahead
            QName secondName = null;
            if (!path.isEmpty() && path.first() instanceof NameItemPathSegment) {
                NameItemPathSegment second = ((NameItemPathSegment) (path.first()));
                secondName = second.getName();
            }

            // known discrepancies betweeen prism structure and repo representation
            if (QNameUtil.match(firstName, ObjectType.F_METADATA)) {
                continue;   // metadata is not an repository entity
            }
            if (QNameUtil.match(firstName, AssignmentType.F_CONSTRUCTION) &&
                    QNameUtil.match(secondName, ConstructionType.F_RESOURCE_REF)) {
                continue;   // construction/resourceRef -> resourceRef
            }

            Definition def = findDefinition(firstName, Definition.class);
            return def;
        }
    }

    // beware, path translation can end e.g. in ANY element (extension, attributes)
    // also, beware of parent links and cross-entity links
    @Override
    public JpaDefinitionPath translatePath(ItemPath path) {
        Holder<ItemPath> pathHolder = new Holder<>(path);
        JpaDefinitionPath jpaPath = new JpaDefinitionPath();

        Definition currentDefinition = this;
        for (;;) {
            ItemPath currentPath = pathHolder.getValue();
            if (currentPath == null || currentPath.isEmpty()) {
                return jpaPath;
            }
            jpaPath.add(currentDefinition);
            currentDefinition = nextDefinition(pathHolder);
        }
    }

}
