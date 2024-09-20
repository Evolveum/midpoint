/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.resolution;

import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.query.definition.JpaAnyPropertyDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.JpaDataNodeDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Describes result of ItemPath resolution: HQL property path + current data node definition.
 * Points to parent data item (i.e. the one that corresponds to parent ItemPath translation),
 * just in case we would like to go back via ".." operator.
 *
 * This differs from JpaDefinitions in that it points to a specific HQL property used in the
 * query being constructed.
 *
 * This object is unmodifiable.
 */
public class HqlDataInstance<D extends JpaDataNodeDefinition> implements DebugDumpable {

    /** Concrete path for accessing this item */
    @NotNull final String hqlPath;

    /** Definition of this item */
    @NotNull final D jpaDefinition;

    /** How we got here - may be null if root or when using ".." path element. */
    final HqlDataInstance<?> parentDataItem;

    HqlDataInstance(@NotNull String hqlPath, @NotNull D jpaDefinition, HqlDataInstance<?> parentDataItem) {
        this.hqlPath = hqlPath;
        this.jpaDefinition = jpaDefinition;
        this.parentDataItem = parentDataItem;
    }

    /** Separate method because of type inference needs. */
    static <D extends JpaDataNodeDefinition> @NotNull HqlDataInstance<?> create(
            String newHqlPath, DataSearchResult<D> result, HqlDataInstance<?> parentDataInstance) {
        return new HqlDataInstance<>(newHqlPath, result.getTargetDefinition(), parentDataInstance);
    }

    public String getHqlPath() {
        return hqlPath;
    }

    public @NotNull D getJpaDefinition() {
        return jpaDefinition;
    }

    HqlDataInstance<?> getParentItem() {
        return parentDataItem;
    }

    @SuppressWarnings("unused")
    public String debugDumpNoParent() {
        return debugDump(0, false);
    }

    @Override
    public String debugDump(int indent) {
        return debugDump(indent, true);
    }

    public String debugDump(int indent, boolean showParent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("HqlDataInstance:\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("HQL path: ").append(hqlPath).append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("JPA definition: ").append(jpaDefinition).append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("Previous result: ");
        if (parentDataItem != null) {
            if (showParent) {
                sb.append("\n");
                sb.append(parentDataItem.debugDump(indent + 2));
            } else {
                sb.append(parentDataItem).append("\n");
            }
        } else {
            sb.append("(null)\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "HqlDataInstance{" +
                "hqlPath='" + hqlPath + '\'' +
                ", jpaDefinition=" + jpaDefinition +
                ", parentDataItem=(" + (parentDataItem != null ? parentDataItem.hqlPath : "none") +
                ")}";
    }

    public HqlEntityInstance asHqlEntityInstance() {
        return new HqlEntityInstance(hqlPath, (JpaEntityDefinition) jpaDefinition, parentDataItem);
    }
}
