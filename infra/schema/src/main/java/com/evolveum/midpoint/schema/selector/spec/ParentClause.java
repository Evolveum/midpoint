/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;

import com.evolveum.midpoint.schema.selector.eval.FilteringContext;
import com.evolveum.midpoint.schema.selector.eval.MatchingContext;
import com.evolveum.midpoint.schema.selector.eval.FilterCollector;
import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.*;

import javax.xml.namespace.QName;

public class ParentClause extends SelectorClause {

    @NotNull private final ValueSelector parent;
    @NotNull private final ItemPath path;

    private ParentClause(@NotNull ValueSelector parent, @NotNull ItemPath path) {
        this.parent = parent;
        this.path = path;
    }

    public static ParentClause of(@NotNull ValueSelector parent, @NotNull ItemPath path) {
        return new ParentClause(parent, path);
    }

    @Override
    public @NotNull String getName() {
        return "parent";
    }

    public @NotNull ValueSelector getParent() {
        return parent;
    }

    public @NotNull ItemPath getPath() {
        return path;
    }

    @Override
    public boolean matches(@NotNull PrismValue value, @NotNull MatchingContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        // TODO multiple levels
        var parent1 = value.getParent();
        PrismValue parent2 = parent1 instanceof Item<?, ?> ? ((Item<?, ?>) parent1).getParent() : null;
        if (parent2 == null) {
            traceNotApplicable(ctx, "value has no parent");
            return false;
        }
        boolean matches = parent.matches(parent2, ctx.child("p", "parent"));
        traceApplicability(ctx, matches, "parent specification matches: %s", matches);
        return matches;
    }

    @Override
    public boolean toFilter(@NotNull FilteringContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        PrismContext prismContext = PrismContext.get();
        QName parentTypeName = parent.getTypeName();
        if (parentTypeName == null) {
            throw new ConfigurationException("Parent specification must have type name");
        }
        var ctd = prismContext.getSchemaRegistry().findComplexTypeDefinitionByType(parentTypeName);
        if (ctd == null) {
            throw new UnsupportedOperationException("No CTD for " + parentTypeName);
        }
        Class<?> parentClass = ctd.getCompileTimeClass();
        if (parentClass == null) {
            throw new UnsupportedOperationException("No static class for " + ctd);
        }

        ObjectFilter conjunct;
        var childCollector = FilterCollector.defaultOne();
        FilteringContext childCtx = ctx.child(
                parentClass,
                childCollector,
                null, // the original filter is not interesting (or, should we look for parent there?)
                "p", "parent");

        var applicable = parent.toFilter(childCtx);
        if (!applicable) {
            traceNotApplicable(ctx, "parent selector not applicable");
            return false;
        }

        //noinspection unchecked
        conjunct = prismContext.queryFor((Class<? extends Containerable>) ctx.getRestrictedType())
                .ownedBy((Class<? extends Containerable>) parentClass, path)
                .filter(childCollector.getFilter())
                .buildFilter();

        addConjunct(ctx, conjunct);
        return true;
    }

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        sb.append(" path=").append(path);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "parent selector", parent, indent + 1);
    }
}
