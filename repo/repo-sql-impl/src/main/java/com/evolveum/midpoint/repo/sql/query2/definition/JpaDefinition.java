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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.DefinitionSearchResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;

/**
 * Defines the content of item or standalone entity.
 *
 * @author mederly
 */
public abstract class JpaDefinition implements DebugDumpable, Visitable {

    private static final Trace LOGGER = TraceManager.getTrace(JpaDefinition.class);

    /**
     * JPA class - either "composite" (RObject, RUser, RAssignment, ...) or "primitive" (String, Integer, int, ...)
     */
    private Class jpaClass;

    public JpaDefinition(Class jpaClass) {
        Validate.notNull(jpaClass, "jpaClass");
        this.jpaClass = jpaClass;
    }

    public Class getJpaClass() {
        return jpaClass;
    }

    public String getJpaClassName() {
        return jpaClass.getSimpleName();
    }

    /**
     * Tries to find "next step" in the definition chain for a given ItemPath.
     * Parts of the path that have no representation in the repository (e.g. metadata,
     * construction) are simply skipped.
     *
     * @param path A path to be resolved. Always non-null and non-empty.
     * @return
     * - Normally it returns the search result containing next item definition (entity, collection, ...) in the chain
     *   and the unresolved remainder of the path.
     * - If the search was not successful, returns null.
     * - Note that for "Any" container returns the container entity definition itself plus the path that is to
     *   be searched in the given "Any" container. However, there's no point in repeating the search there,
     *   as it would yield the same result.
     */
    public abstract DefinitionSearchResult<JpaItemDefinition> nextDefinition(ItemPath path) throws QueryException;

    /**
     * Resolves the whole ItemPath.
     *
     * If successful, returns either:
     *  - correct definition + empty path, or
     *  - Any definition + path remainder
     *
     * If unsuccessful, return null.
     *
     * @return
     */
    public <D extends JpaItemDefinition> DefinitionSearchResult<D> findDefinition(ItemPath path, Class<D> type) throws QueryException {
        JpaDefinition currentDefinition = this;
        for (;;) {
            if (ItemPath.isNullOrEmpty(path)) {     // we are at the end of search - hoping we found the correct class
                if (type.isAssignableFrom(currentDefinition.getClass())) {
                    return new DefinitionSearchResult<>((D) currentDefinition, null);
                } else {
                    return null;
                }
            }
            if (currentDefinition instanceof JpaAnyDefinition) {
                if (type.isAssignableFrom(JpaAnyDefinition.class)) {
                    return new DefinitionSearchResult<>((D) currentDefinition, path);
                } else {
                    return null;
                }
            }
            DefinitionSearchResult<JpaItemDefinition> result = currentDefinition.nextDefinition(path);
            if (result == null) {   // oops
                return null;
            }
            currentDefinition = result.getItemDefinition();
            path = result.getRemainder();
        }
    }

    /**
     * Translates ItemPath to a sequence of definitions.
     *
     * @param itemPath
     * @return The translation (if successful) or null (if not successful).
     * For "Any" elements, the last element in the path is Any.
     *
     * Note: structurally similar to findDefinition above
     */

//    public DefinitionPath translatePath(ItemPath itemPath) throws QueryException {
//        ItemPath originalPath = itemPath;                            // we remember original path just for logging
//        DefinitionPath definitionPath = new DefinitionPath();
//
//        JpaDefinition currentDefinition = this;
//        for (;;) {
//            if (currentDefinition instanceof JpaAnyDefinition || ItemPath.isNullOrEmpty(itemPath)) {
//                LOGGER.trace("ItemPath {} translated to DefinitionPath {} (started in {})", originalPath, definitionPath, this);
//                return definitionPath;
//            }
//            DefinitionSearchResult result = currentDefinition.nextDefinition(itemPath);
//            LOGGER.trace("nextDefinition on {} returned {}", itemPath, result != null ? result.getItemDefinition() : "(null)");
//            if (result == null) {
//                return null;        // sorry we failed
//            }
//            JpaItemDefinition nextDefinition = result.getItemDefinition();
//            definitionPath.add(nextDefinition);
//
//            itemPath = result.getRemainder();
//            currentDefinition = nextDefinition;
//        }
//    }

    public String toString() {
        return getDebugDumpClassName() + ":" + jpaClass.getSimpleName();
    }

    protected abstract String getDebugDumpClassName();

    @Override
    public String debugDump() {
        return debugDump(0);
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

}
