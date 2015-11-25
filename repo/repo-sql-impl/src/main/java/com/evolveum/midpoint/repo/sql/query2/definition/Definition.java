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
import com.evolveum.midpoint.repo.sql.query2.DefinitionSearchResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public abstract class Definition implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(Definition.class);

    //jaxb
    private QName jaxbName;
    private Class jaxbType;
    //jpa
    private String jpaName;
    private Class jpaType;

    private CollectionSpecification collectionSpecification;

    public Definition(QName jaxbName, Class jaxbType, String jpaName, Class jpaType, CollectionSpecification collectionSpecification) {
        this.jaxbName = jaxbName;
        this.jaxbType = jaxbType;
        this.jpaName = jpaName;
        this.jpaType = jpaType;
        this.collectionSpecification = collectionSpecification;
    }

    public QName getJaxbName() {
        return jaxbName;
    }

    public Class getJaxbType() {
        return jaxbType;
    }

    public String getJpaName() {
        return jpaName;
    }

    public Class getJpaType() {
        return jpaType;
    }

    protected void debugDumpExtended(StringBuilder builder, int indent) {

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
        sb.append(getDebugDumpClassName());
        sb.append('{');
        sb.append("jaxbN=").append(dumpQName(jaxbName));
        sb.append(", jaxbT=").append((jaxbType != null ? jaxbType.getSimpleName() : ""));
        sb.append(", jpaN=").append(jpaName);
        sb.append(", jpaT=").append((jpaType != null ? jpaType.getSimpleName() : ""));
        if (collectionSpecification != null) {
            sb.append(", coll=").append(collectionSpecification);       // TODO
        }
        debugDumpExtended(sb, indent);
        sb.append('}');

        return sb.toString();
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    protected abstract String getDebugDumpClassName();

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
    public abstract DefinitionSearchResult nextDefinition(ItemPath path);

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
    public <D extends Definition> DefinitionSearchResult<D> findDefinition(ItemPath path, Class<D> type) {
        Definition currentDefinition = this;
        for (;;) {
            if (ItemPath.isNullOrEmpty(path)) {     // we are at the end of search - hoping we found the correct class
                if (type.isAssignableFrom(currentDefinition.getClass())) {
                    return new DefinitionSearchResult<>((D) currentDefinition, null);
                } else {
                    return null;
                }
            }
            if (currentDefinition instanceof AnyDefinition) {
                if (type.isAssignableFrom(AnyDefinition.class)) {
                    return new DefinitionSearchResult<>((D) currentDefinition, path);
                } else {
                    return null;
                }
            }
            DefinitionSearchResult<D> result = currentDefinition.nextDefinition(path);
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

    public DefinitionPath translatePath(ItemPath itemPath) {
        Definition currentDefinition = this;
        ItemPath originalPath = itemPath;                            // we remember original path just for logging
        DefinitionPath definitionPath = new DefinitionPath();

        for (;;) {
            if (currentDefinition instanceof AnyDefinition || ItemPath.isNullOrEmpty(itemPath)) {
                LOGGER.trace("ItemPath {} translated to DefinitionPath {} (started in {})", originalPath, definitionPath, this.getShortInfo());
                return definitionPath;
            }
            DefinitionSearchResult result = currentDefinition.nextDefinition(itemPath);
            LOGGER.trace("nextDefinition on {} returned {}", itemPath, result != null ? result.getItemDefinition().getShortInfo() : "(null)");
            if (result == null) {
                return null;        // sorry we failed
            }
            currentDefinition = result.getItemDefinition();
            definitionPath.add(currentDefinition);
            itemPath = result.getRemainder();
        }
    }

    public String getShortInfo() {
        return getDebugDumpClassName() +
                (collectionSpecification != null ? "[]" : "") +
                ":" + DebugUtil.formatElementName(getJaxbName()) + ":" + getJpaName();
    }

    public String toString() {
        return getShortInfo();
    }

    public boolean isCollection() {
        return collectionSpecification != null;
    }

    public CollectionSpecification getCollectionSpecification() {
        return collectionSpecification;
    }
}
