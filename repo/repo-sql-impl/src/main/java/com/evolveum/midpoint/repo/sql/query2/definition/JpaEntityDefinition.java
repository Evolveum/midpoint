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
import com.evolveum.midpoint.repo.sql.query2.DataSearchResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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
 * @author mederly
 */
public class JpaEntityDefinition extends JpaDataNodeDefinition implements DebugDumpable, Visitable {

    private static final Trace LOGGER = TraceManager.getTrace(JpaEntityDefinition.class);

    /**
     * child definitions of this entity
     */
    private List<JpaLinkDefinition> definitions = new ArrayList<>();
    private JpaEntityDefinition superclassDefinition;

    public JpaEntityDefinition(Class jpaClass, Class jaxbClass) {
        super(jpaClass, jaxbClass);
    }

    public void addDefinition(JpaLinkDefinition definition) {
        JpaLinkDefinition oldDef = findLinkDefinition(definition.getItemPathSegment(), JpaDataNodeDefinition.class);
        if (oldDef != null) {
            definitions.remove(oldDef);
        }
        definitions.add(definition);
    }

    public void sortDefinitions() {
        Collections.sort(definitions, new LinkDefinitionComparator());
    }

    private <D extends JpaDataNodeDefinition> JpaLinkDefinition<D> findLinkDefinition(ItemPathSegment itemPathSegment, Class<D> type) {
        Validate.notNull(itemPathSegment, "ItemPathSegment must not be null.");
        Validate.notNull(type, "Definition type must not be null.");

        for (JpaLinkDefinition definition : definitions) {
            if (!definition.matches(itemPathSegment)) {
                continue;
            }
            if (type.isAssignableFrom(definition.getTargetClass())) {
                return definition;
            }
        }
        return null;
    }

    public interface LinkDefinitionHandler {
        void handle(JpaLinkDefinition linkDefinition);
    }

    /**
     * Resolves the whole ItemPath (non-empty!)
     *
     * If successful, returns either:
     *  - correct definition + empty path, or
     *  - Any definition + path remainder
     *
     * If unsuccessful, return null.
     *
     * @return
     */
    public <D extends JpaDataNodeDefinition> DataSearchResult<D> findDataNodeDefinition(ItemPath path, Class<D> type) throws QueryException {
        return findDataNodeDefinition(path, type, null);
    }

    public <D extends JpaDataNodeDefinition> DataSearchResult<D> findDataNodeDefinition(ItemPath path, Class<D> type, LinkDefinitionHandler handler) throws QueryException {
        JpaDataNodeDefinition currentDefinition = this;
        for (;;) {
            DataSearchResult<JpaDataNodeDefinition> result = currentDefinition.nextLinkDefinition(path);
            if (result == null) {   // oops
                return null;
            }
            if (handler != null) {
                handler.handle(result.getLinkDefinition());
            }
            JpaLinkDefinition linkDefinition = result.getLinkDefinition();
            JpaDataNodeDefinition targetDefinition = linkDefinition.getTargetDefinition();

            if (result.isComplete() || targetDefinition instanceof JpaAnyDefinition) {
                if (type.isAssignableFrom(targetDefinition.getClass())) {
                    return (DataSearchResult<D>) result;
                } else {
                    return null;
                }
            }

            path = result.getRemainder();
            currentDefinition = targetDefinition;
        }
    }

    /**
     * Translates ItemPath to a sequence of definitions.
     *
     * @param itemPath
     * @return The translation (if successful) or null (if not successful).
     * For "Any" elements, the last element in the path is Any.
     */

    public DefinitionPath translatePath(ItemPath itemPath) throws QueryException {
        final DefinitionPath definitionPath = new DefinitionPath();
        DataSearchResult result = findDataNodeDefinition(itemPath, JpaDataNodeDefinition.class, new LinkDefinitionHandler() {
            @Override
            public void handle(JpaLinkDefinition linkDefinition) {
                definitionPath.add(linkDefinition);
            }
        });
        if (result != null) {
            LOGGER.trace("ItemPath {} successfully translated to DefinitionPath {} (started in {})", itemPath, definitionPath, this);
            return definitionPath;
        } else {
            LOGGER.trace("ItemPath {} PARTIALLY translated to DefinitionPath {} (started in {})", itemPath, definitionPath, this);
            return null;
        }
    }

    public void setSuperclassDefinition(JpaEntityDefinition superclassDefinition) {
        this.superclassDefinition = superclassDefinition;
    }

    public JpaEntityDefinition getSuperclassDefinition() {
        return superclassDefinition;
    }

    @Override
    public DataSearchResult nextLinkDefinition(ItemPath path) throws QueryException {

        // first treat known discrepancies between prism structure and repo representation:
        // metadata -> none,
        // construction/resourceRef -> resourceRef
        //
        // TODO replace with the use of VirtualEntity with null jpaName
        while (skipFirstItem(path)) {
            path = path.tail();
        }

        if (ItemPath.isNullOrEmpty(path)) {     // doesn't fulfill precondition
            return null;
        }

        JpaLinkDefinition link = findLinkDefinition(path.first(), JpaDataNodeDefinition.class);
        if (link == null) {
            return null;
        } else {
            link.resolveEntityPointer();
            return new DataSearchResult(link, path.tail());
        }
    }


    private boolean skipFirstItem(ItemPath path) throws QueryException {
        if (ItemPath.isNullOrEmpty(path)) {
            return false;
        }
        ItemPathSegment first = path.first();
        if (first instanceof IdItemPathSegment) {
            throw new QueryException("ID path segments are not allowed in query: " + path);
        } else if (first instanceof ObjectReferencePathSegment) {
            throw new QueryException("'@' path segment cannot be used in the context of an entity " + this);
        } else if (first instanceof ParentPathSegment) {
            return false;
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
        for (JpaLinkDefinition definition : definitions) {
            definition.accept(visitor);
        }
    }

    @Override
    protected String getDebugDumpClassName() {
        return "Ent";
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    public boolean isAssignableFrom(JpaEntityDefinition specificEntityDefinition) {
        return getJpaClass().isAssignableFrom(specificEntityDefinition.getJpaClass());
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(getJpaClass().getModifiers());
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getShortInfo());
        if (superclassDefinition != null) {
            sb.append(" extends ").append(superclassDefinition);
        }
        for (JpaLinkDefinition definition : definitions) {
            sb.append("\n");
            sb.append(definition.debugDump(indent + 1));
        }
        return sb.toString();
    }
}

