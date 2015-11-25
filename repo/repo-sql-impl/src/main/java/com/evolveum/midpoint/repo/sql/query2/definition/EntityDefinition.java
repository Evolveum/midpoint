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
import com.evolveum.midpoint.repo.sql.query2.DefinitionSearchResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.lang.reflect.Modifier;
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
    private EntityDefinition superclassDefinition;

    public EntityDefinition(QName jaxbName, Class jaxbType, String jpaName, Class jpaType, CollectionSpecification collectionSpecification) {
        super(jaxbName, jaxbType, jpaName, jpaType, collectionSpecification);
        Validate.notNull(jaxbName, "jaxbName");
        Validate.notNull(jaxbType, "jaxbType");
        Validate.notNull(jpaName, "jpaName");
        Validate.notNull(jpaType, "jpaType");
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
    protected void debugDumpExtended(StringBuilder builder, int indent) {
        builder.append(", embedded=").append(isEmbedded());
        builder.append(", definitions=\n");

        List<Definition> definitions = getDefinitions();
        for (Definition definition : definitions) {
            builder.append(definition.debugDump(indent+1));
            builder.append('\n');
        }
        DebugUtil.indentDebugDump(builder, indent);     // indentation before final '}'
    }

    @Override
    protected String getDebugDumpClassName() {
        return "Ent";
    }

    private <D extends Definition> D findDefinition(QName jaxbName, Class<D> type) {
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
    public DefinitionSearchResult nextDefinition(ItemPath path) {

        // first treat known discrepancies betweeen prism structure and repo representation:
        // metadata -> none,
        // construction/resourceRef -> resourceRef
        while (skipFirstItem(path)) {
            path = path.tail();
        }

        // now find the definition
        if (ItemPath.isNullOrEmpty(path)) {
            // note that for wrong input (e.g. path=metadata) this answer might be wrong, but we don't care
            return new DefinitionSearchResult(this, null);
        }

        QName firstName = ((NameItemPathSegment) path.first()).getName();
        Definition def = findDefinition(firstName, Definition.class);
        if (def != null) {
            return new DefinitionSearchResult(def, path.tail());
        } else {
            return null;
        }
    }

    private boolean skipFirstItem(ItemPath path) {
        if (ItemPath.isNullOrEmpty(path)) {
            return false;
        }
        QName firstName = ((NameItemPathSegment) path.first()).getName();

        // metadata -> null
        if (QNameUtil.match(firstName, ObjectType.F_METADATA)) {
            return true;
        }

        // construction/resourceRef -> construction
        ItemPath remainder = path.tail();
        if (remainder.isEmpty() || !(remainder.first() instanceof NameItemPathSegment)) {
            return false;
        }
        NameItemPathSegment second = ((NameItemPathSegment) (remainder.first()));
        QName secondName = second.getName();
        if (QNameUtil.match(firstName, AssignmentType.F_CONSTRUCTION) &&
                QNameUtil.match(secondName, ConstructionType.F_RESOURCE_REF)) {
            return true;
        }
        return false;
    }

    public boolean isAssignableFrom(EntityDefinition specificEntityDefinition) {
        return getJpaType().isAssignableFrom(specificEntityDefinition.getJpaType());
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(getJpaType().getModifiers());
    }

    public void setSuperclassDefinition(EntityDefinition superclassDefinition) {
        this.superclassDefinition = superclassDefinition;
    }

    public EntityDefinition getSuperclassDefinition() {
        return superclassDefinition;
    }
}
