/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.resolution.DataSearchResult;

/**
 * Special placeholder to allow for cross-references: entity definition that points to another entity.
 * Currently, the process of resolving allows to point to root entity definitions here only.
 * As a hack, we implement self pointers (e.g. RAssignment.metadata->RAssignment) also for non-root
 * entities, provided they are resolved on creation. (The reason of using JpaEntityPointerDefinition
 * there is just to break navigation cycles e.g. when using a visitor.)
 *
 * @author mederly
 */
public class JpaEntityPointerDefinition extends JpaDataNodeDefinition<JpaEntityPointerDefinition> {

    private JpaEntityDefinition resolvedEntityDefinition; // lazily evaluated

    public JpaEntityPointerDefinition(Class<? extends RObject> jpaClass) {
        super(jpaClass, null);
    }

    public JpaEntityPointerDefinition(JpaEntityDefinition alreadyResolved) {
        super(alreadyResolved.getJpaClass(), alreadyResolved.getJaxbClass());
        this.resolvedEntityDefinition = alreadyResolved;
    }

    public JpaEntityDefinition getResolvedEntityDefinition() {
        return resolvedEntityDefinition;
    }

    public void setResolvedEntityDefinition(JpaEntityDefinition resolvedEntityDefinition) {
        this.resolvedEntityDefinition = resolvedEntityDefinition;
    }

    @Override
    public DataSearchResult<?> nextLinkDefinition(
            ItemPath path, ItemDefinition<?> itemDefinition, PrismContext prismContext)
            throws QueryException {
        return resolvedEntityDefinition.nextLinkDefinition(path, itemDefinition, prismContext);
    }

    @Override
    protected String getDebugDumpClassName() {
        return "EntPtr";
    }

    @Override
    public String debugDump(int indent) {
        if (resolvedEntityDefinition == null) {
            return getShortInfo();
        } else {
            return getDebugDumpClassName() + ":" + resolvedEntityDefinition.getShortInfo();
        }
    }

    @Override
    public void accept(Visitor<JpaDataNodeDefinition<JpaEntityPointerDefinition>> visitor) {
        visitor.visit(this);
    }

    public boolean isResolved() {
        return resolvedEntityDefinition != null;
    }
}
