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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public class JpaLinkDefinition<D extends JpaDataNodeDefinition> implements Visitable, DebugDumpable {

    @NotNull private final ItemPath itemPath;                         // usually single item, but might be longer
    private final String jpaName;                                     // beware - null for "same entity" transitions (metadata, construction, ...)
    private final CollectionSpecification collectionSpecification;    // null if single valued
    private final boolean embedded;
    @NotNull private D targetDefinition;

    public JpaLinkDefinition(@NotNull ItemPath itemPath, String jpaName, CollectionSpecification collectionSpecification,
			boolean embedded, @NotNull D targetDefinition) {
        this.itemPath = itemPath;
        this.jpaName = jpaName;
        this.collectionSpecification = collectionSpecification;
        this.embedded = embedded;
        this.targetDefinition = targetDefinition;
    }

    public JpaLinkDefinition(@NotNull ItemPathSegment itemPathSegment, String jpaName, CollectionSpecification collectionSpecification, boolean embedded, D targetDefinition) {
        this(new ItemPath(itemPathSegment), jpaName, collectionSpecification, embedded, targetDefinition);
    }

    public JpaLinkDefinition(@NotNull QName jaxbName, String jpaName, CollectionSpecification collectionSpecification, boolean embedded, D targetDefinition) {
        this(new NameItemPathSegment(jaxbName), jpaName, collectionSpecification, embedded, targetDefinition);

    }

    @NotNull
    public ItemPath getItemPath() {
        return itemPath;
    }

    ItemPathSegment getItemPathSegment() {
        if (itemPath.size() != 1) {
            throw new IllegalStateException("Expected single-item path, found '" + itemPath + "' instead.");
        }
        return itemPath.first();
    }

    public String getJpaName() {
        return jpaName;
    }

    public CollectionSpecification getCollectionSpecification() {
        return collectionSpecification;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    @NotNull
    public D getTargetDefinition() {
        return targetDefinition;
    }

    public boolean matchesExactly(ItemPath itemPath) {
        return this.itemPath.equivalent(itemPath);
    }

    public boolean matchesStartOf(ItemPath itemPath) {
        return itemPath.startsWith(this.itemPath);
    }

    @SuppressWarnings("unchecked")
    public Class<D> getTargetClass() {
        return (Class<D>) targetDefinition.getClass();
    }

    public boolean isMultivalued() {
        return collectionSpecification != null;
    }

    /**
     * Has this link JPA representation? I.e. is it represented as a getter?
     * Some links, e.g. metadata and construction, are not.
     */
    public boolean hasJpaRepresentation() {
        return jpaName != null;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        targetDefinition.accept(visitor);
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        dumpLink(sb);
        sb.append(targetDefinition.debugDump(indent + 1));
        return sb.toString();
    }

    private void dumpLink(StringBuilder sb) {
        sb.append(itemPath).append(" => ").append(jpaName);
        if (collectionSpecification != null) {
            sb.append(collectionSpecification.getShortInfo());
        }
        if (embedded) {
            sb.append(" (embedded)");
        }
        sb.append(" -> ");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        dumpLink(sb);
        sb.append(targetDefinition.getShortInfo());
        return sb.toString();
    }

	@SuppressWarnings("unchecked")
    void resolveEntityPointer() {
        if (targetDefinition instanceof JpaEntityPointerDefinition) {
            // typing hack but we don't mind
            targetDefinition = (D) ((JpaEntityPointerDefinition) targetDefinition).getResolvedEntityDefinition();
        }
    }
}
