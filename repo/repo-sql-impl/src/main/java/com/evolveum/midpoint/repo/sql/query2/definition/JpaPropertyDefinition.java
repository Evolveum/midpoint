/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query2.definition;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.query2.resolution.DataSearchResult;

/**
 * @author lazyman
 */
public class JpaPropertyDefinition<T extends JpaPropertyDefinition<T>>
        extends JpaDataNodeDefinition<T> {

    private final boolean lob;
    private final boolean enumerated;
    private final boolean indexed;            // unused now (true if @Index-ed)
    private final boolean count;            // "count"-type variable, like RShadow.pendingOperationCount
    private final boolean neverNull;

    JpaPropertyDefinition(
            Class<? extends RObject> jpaClass, Class jaxbClass, boolean lob,
            boolean enumerated, boolean indexed, boolean count, boolean neverNull) {
        super(jpaClass, jaxbClass);
        this.lob = lob;
        this.enumerated = enumerated;
        this.indexed = indexed;
        this.count = count;
        this.neverNull = neverNull;
    }

    public boolean isLob() {
        return lob;
    }

    public boolean isEnumerated() {
        return enumerated;
    }

    public boolean isIndexed() {
        return indexed;
    }

    public boolean isCount() {
        return count;
    }

    public boolean isNeverNull() {
        return neverNull;
    }

    @Override
    protected String getDebugDumpClassName() {
        return "Prop";
    }

    @Override
    public DataSearchResult nextLinkDefinition(ItemPath path, ItemDefinition itemDefinition, PrismContext prismContext) {
        // nowhere to come from here
        return null;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getShortInfo());
        if (lob) {
            sb.append(", lob");
        }
        if (enumerated) {
            sb.append(", enumerated");
        }
        if (indexed) {
            sb.append(", indexed");
        }
        if (count) {
            sb.append(", count");
        }
        if (neverNull) {
            sb.append(", non-null");
        }
        return sb.toString();
    }
}
