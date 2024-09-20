/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

import com.evolveum.midpoint.repo.sql.query.hqm.JoinSpecification;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @param <TD> Target definition
 */
public class JpaLinkDefinition<TD extends JpaDataNodeDefinition>
        implements Visitable, DebugDumpable {

    /** usually single item, but might be longer */
    @NotNull private final ItemPath itemPath;

    /** beware - null for "same entity" transitions (metadata, construction, ...) */
    private final String jpaName;

    /** null if single valued */
    private final CollectionSpecification collectionSpecification;

    private final boolean embedded;

    /** When joining, should we mention the target type explicitly? See {@link JoinSpecification#explicitJoinedType}. */
    private final boolean explicitTargetTypeRequired;

    @NotNull private TD targetDefinition;

    public JpaLinkDefinition(
            @NotNull ItemPath itemPath, String jpaName, CollectionSpecification collectionSpecification,
            boolean embedded, @NotNull TD targetDefinition) {
        this(itemPath, jpaName, collectionSpecification, embedded, targetDefinition, false);

    }
    public JpaLinkDefinition(@NotNull ItemPath itemPath, String jpaName, CollectionSpecification collectionSpecification,
            boolean embedded, @NotNull TD targetDefinition, boolean explicitTargetTypeRequired) {
        this.itemPath = itemPath;
        this.jpaName = jpaName;
        this.collectionSpecification = collectionSpecification;
        this.embedded = embedded;
        this.targetDefinition = targetDefinition;
        this.explicitTargetTypeRequired = explicitTargetTypeRequired;
    }

    @NotNull
    public ItemPath getItemPath() {
        return itemPath;
    }

    Object getItemPathSegment() {
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
    public TD getTargetDefinition() {
        return targetDefinition;
    }

    public boolean matchesExactly(ItemPath itemPath) {
        return this.itemPath.equivalent(itemPath);
    }

    public boolean matchesStartOf(ItemPath itemPath) {
        return this.itemPath.isSubPathOrEquivalent(itemPath);
    }

    @SuppressWarnings("unchecked")
    public Class<TD> getTargetClass() {
        return (Class<TD>) targetDefinition.getClass();
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
        if (targetDefinition instanceof JpaEntityPointerDefinition pointerDefinition) {
            // typing hack but we don't mind
            targetDefinition = (TD) pointerDefinition.getResolvedEntityDefinition();
        }
    }

    public String getExplicitJoinedType() {
        if (explicitTargetTypeRequired) {
            return getTargetDefinition().getJpaClassName();
        } else {
            return null;
        }
    }
}
