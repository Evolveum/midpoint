/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query2.resolution;

import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaAnyPropertyDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaDataNodeDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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
 *
 * @author mederly
 */
public class HqlDataInstance<D extends JpaDataNodeDefinition> implements DebugDumpable {

    @SuppressWarnings("unused")
    private static final Trace LOGGER = TraceManager.getTrace(HqlDataInstance.class);

    @NotNull final String hqlPath;                         // concrete path for accessing this item
    @NotNull final D jpaDefinition;                        // definition of this item
    final HqlDataInstance<?> parentDataItem;                // how we got here - optional

    HqlDataInstance(@NotNull String hqlPath, @NotNull D jpaDefinition, HqlDataInstance<?> parentDataItem) {
        this.hqlPath = hqlPath;
        this.jpaDefinition = jpaDefinition;
        this.parentDataItem = parentDataItem;
    }

    public String getHqlPath() {
        if (jpaDefinition instanceof JpaAnyPropertyDefinition) {
            // This is quite dangerous. Assumes that we don't continue with resolving ItemPath after finding
            // this kind of definition (and that's true).
            return hqlPath + "." + RAnyValue.F_VALUE;
        } else {
            return hqlPath;
        }
    }

    @NotNull
    public D getJpaDefinition() {
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
