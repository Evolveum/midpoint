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

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.resolution.DataSearchResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
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
        JpaLinkDefinition oldDef = findRawLinkDefinition(definition.getItemPath(), JpaDataNodeDefinition.class, true);
        if (oldDef != null) {
            // we don't replace definitions. E.g. name=>nameCopy for concrete classes with name=>name in RObject
            return;
        }
        definitions.add(definition);
    }

    public void sortDefinitions() {
        definitions.sort(new LinkDefinitionComparator());
    }

    private <D extends JpaDataNodeDefinition> JpaLinkDefinition<D> findRawLinkDefinition(@NotNull ItemPath itemPath, @NotNull Class<D> type, boolean exact) {
        for (JpaLinkDefinition<?> definition : definitions) {
            if (exact) {
                if (!definition.matchesExactly(itemPath)) {
                    continue;
                }
            } else {
                if (!definition.matchesStartOf(itemPath)) {
                    continue;
                }
            }
            if (type.isAssignableFrom(definition.getTargetClass())) {
                @SuppressWarnings({ "unchecked", "raw" })
                JpaLinkDefinition<D> typeDefinition = (JpaLinkDefinition<D>) definition;
                return typeDefinition;
            }
        }
        return null;
    }

    @FunctionalInterface
    public interface LinkDefinitionHandler {
        void handle(JpaLinkDefinition linkDefinition);
    }

    /**
     * Resolves the whole ItemPath
     *
     * @param path ItemPath to resolve. Non-empty!
     * @param itemDefinition Definition of the final path segment, if it's "any" property.
     * @param type Type of definition to be found
     * @return
     *
     * If successful, returns correct definition + empty path.
     * If unsuccessful, return null.
     */
    public <D extends JpaDataNodeDefinition> DataSearchResult<D> findDataNodeDefinition(ItemPath path,
            ItemDefinition itemDefinition, Class<D> type, PrismContext prismContext) throws QueryException {
        return findDataNodeDefinition(path, itemDefinition, type, null, prismContext);
    }

    public <D extends JpaDataNodeDefinition> DataSearchResult<D> findDataNodeDefinition(ItemPath path,
            ItemDefinition itemDefinition, Class<D> type, LinkDefinitionHandler handler, PrismContext prismContext) throws QueryException {
        JpaDataNodeDefinition currentDefinition = this;
        for (;;) {
            DataSearchResult<?> result = currentDefinition.nextLinkDefinition(path, itemDefinition, prismContext);
            if (result == null) {   // oops
                return null;
            }
            if (handler != null) {
                handler.handle(result.getLinkDefinition());
            }
            JpaLinkDefinition linkDefinition = result.getLinkDefinition();
            JpaDataNodeDefinition targetDefinition = linkDefinition.getTargetDefinition();

            if (result.isComplete()) {
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

    public void setSuperclassDefinition(JpaEntityDefinition superclassDefinition) {
        this.superclassDefinition = superclassDefinition;
    }

    public JpaEntityDefinition getSuperclassDefinition() {
        return superclassDefinition;
    }

    @Override
    public DataSearchResult<?> nextLinkDefinition(ItemPath path, ItemDefinition<?> itemDefinition, PrismContext prismContext) throws QueryException {

        if (ItemPath.isNullOrEmpty(path)) {     // doesn't fulfill precondition
            return null;
        }

        ItemPathSegment first = path.first();
        if (first instanceof IdItemPathSegment) {
            throw new QueryException("ID item path segments are not allowed in query: " + path);
        } else if (first instanceof ObjectReferencePathSegment) {
            throw new QueryException("'@' path segment cannot be used in the context of an entity " + this);
        }

        JpaLinkDefinition<?> link = findRawLinkDefinition(path, JpaDataNodeDefinition.class, false);
        if (link == null) {
            return null;
        } else {
            link.resolveEntityPointer();
            return new DataSearchResult<>(link, path.tail(link.getItemPath().size()));
        }
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

