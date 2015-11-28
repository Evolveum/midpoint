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

import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.prism.path.ParentPathSegment;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.DefinitionSearchResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mederly
 */
public class JpaEntityContentDefinition implements DebugDumpable, Visitable {

    /**
     * child definitions of this entity
     */
    private List<JpaItemDefinition> definitions;
    private JpaRootEntityDefinition superclassDefinition;

    public void addDefinition(JpaItemDefinition definition) {
        if (definitions == null) {
            definitions = new ArrayList<>();
        }
        JpaItemDefinition oldDef = findDefinition(definition.getJaxbName(), JpaItemDefinition.class);
        if (oldDef != null) {
            definitions.remove(oldDef);
        }
        definitions.add(definition);
    }

    public <D extends JpaItemDefinition> D findDefinition(QName jaxbName, Class<D> type) {
        Validate.notNull(jaxbName, "Jaxb name must not be null.");
        Validate.notNull(type, "Definition type must not be null.");

        for (JpaItemDefinition definition : definitions) {
            if (!QNameUtil.match(jaxbName, definition.getJaxbName())){
                continue;
            }
            if (type.isAssignableFrom(definition.getClass())) {
                return (D) definition;
            }
        }
        return null;
    }

    public void setSuperclassDefinition(JpaRootEntityDefinition superclassDefinition) {
        this.superclassDefinition = superclassDefinition;
    }

    public JpaRootEntityDefinition getSuperclassDefinition() {
        return superclassDefinition;
    }

    public DefinitionSearchResult nextDefinition(JpaEntityDefinition owningDefinition, ItemPath path) throws QueryException {

        // first treat known discrepancies between prism structure and repo representation:
        // metadata -> none,
        // construction/resourceRef -> resourceRef
        while (skipFirstItem(path, owningDefinition)) {
            path = path.tail();
        }

        // now find the definition
        if (ItemPath.isNullOrEmpty(path)) {
            // note that for wrong input (e.g. path=metadata) this answer might be wrong, but we don't care
            // return new DefinitionSearchResult(owningDefinition, null);
            return null;
        }

        QName firstName = ((NameItemPathSegment) path.first()).getName();
        JpaItemDefinition def = findDefinition(firstName, JpaItemDefinition.class);
        if (def == null) {
            return null;
        } else {
            return new DefinitionSearchResult(def, path.tail());
        }
    }


    private boolean skipFirstItem(ItemPath path, JpaEntityDefinition owningDefinition) throws QueryException {
        if (ItemPath.isNullOrEmpty(path)) {
            return false;
        }
        ItemPathSegment first = path.first();
        if (first instanceof IdItemPathSegment) {
            throw new QueryException("ID path segments are not allowed in query: " + path);
        } else if (first instanceof ObjectReferencePathSegment) {
            throw new QueryException("'@' path segment cannot be used in the context of an entity " + owningDefinition);
        } else if (first instanceof ParentPathSegment) {
            throw new UnsupportedOperationException("TODO");
        }

        QName firstName = ((NameItemPathSegment) first).getName();

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


    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        for (JpaItemDefinition definition : definitions) {
            definition.accept(visitor);
        }
    }

    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("Superclass: ").append(superclassDefinition).append(", definitions (").append(definitions.size()).append("):\n");
        for (JpaItemDefinition definition : definitions) {
            sb.append(definition.debugDump(indent+1)).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }
}

