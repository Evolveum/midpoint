/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;

import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.selector.eval.FilteringContext;
import com.evolveum.midpoint.schema.selector.eval.MatchingContext;
import com.evolveum.midpoint.schema.selector.eval.FilterCollector;
import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.*;

import static com.evolveum.midpoint.util.MiscUtil.*;

/**
 * A clause that:
 *
 * . Puts the (presumably) sub-object selector into the context of either a prism object, or upper-level container.
 * (Using a type in the {@link #parentSelector} and a {@link #path}.)
 *
 * . Optionally restricts the set of candidate parent values (of object or container).
 */
public class ParentClause extends SelectorClause {

    /** Selector that should be applied onto the parent value. */
    @NotNull private final ValueSelector parentSelector;

    /** Path from the parent value to the current value. Must not be empty. */
    @NotNull private final ItemPath path;

    private ParentClause(@NotNull ValueSelector parentSelector, @NotNull ItemPath path) {
        this.parentSelector = parentSelector;
        this.path = path;
    }

    public static ParentClause of(@NotNull ValueSelector parent, @NotNull ItemPath path) throws ConfigurationException {
        configCheck(!path.isEmpty(), "path must not be empty");
        return new ParentClause(parent, path);
    }

    @Override
    public @NotNull String getName() {
        return "parent";
    }

    public @NotNull ValueSelector getParentSelector() {
        return parentSelector;
    }

    public @NotNull ItemPath getPath() {
        return path;
    }

    @Override
    public boolean matches(@NotNull PrismValue value, @NotNull MatchingContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        for (int i = 0; i < path.size(); i++) {
            var parent1 = value.getParent();
            PrismValue parent2 = parent1 instanceof Item<?, ?> item ? item.getParent() : null;
            if (parent2 == null) {
                traceNotApplicable(ctx, "value has no parent");
                return false;
            }
            value = parent2;
        }
        boolean matches = parentSelector.matches(value, ctx.next("p", "parent"));
        traceApplicability(ctx, matches, "parent specification matches: %s", matches);
        return matches;
    }

    @Override
    public boolean toFilter(@NotNull FilteringContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var parentClass = parentSelector.getEffectiveTypeClause().getTypeClass();

        var parentFilterCollector = FilterCollector.defaultOne();
        FilteringContext parentCtx = ctx.next(
                parentClass,
                parentFilterCollector,
                null, // the original filter is not interesting (or, should we look for parent there?)
                "p", "parent");

        if (!parentSelector.toFilter(parentCtx)) {
            traceNotApplicable(ctx, "parent selector not applicable");
            return false;
        }

        ObjectFilter conjunct = createOwnedByFilter(
                ctx.getRestrictedType(), parentClass, parentFilterCollector.getFilter(), path);
        addConjunct(ctx, conjunct);
        return true;
    }

    /**
     * Between parent and child there can be multiple levels of containers.
     * For example,
     *
     * - parent: {@link AccessCertificationCampaignType}
     * - path: `case/workItem`
     * - child: {@link AccessCertificationWorkItemType}
     *
     * As the current repository is limited to single-level parent-child relations, we have to create a sequence of nested
     * owned-by filters.
     */
    private ObjectFilter createOwnedByFilter(
            Class<?> ultimateChildClass,
            Class<?> ultimateParentClass,
            ObjectFilter ultimateParentFilter,
            ItemPath path) throws SchemaException {

        ObjectFilter currentParentFilter = ultimateParentFilter;
        //noinspection unchecked
        Class<? extends Containerable> currentParentClass = (Class<? extends Containerable>) ultimateParentClass;
        ItemPath currentPath = path;
        for (;;) {
            Class<? extends Containerable> currentChildClass;
            ItemName first = currentPath.firstNameOrFail();
            if (currentPath.size() == 1) {
                //noinspection unchecked
                currentChildClass = (Class<? extends Containerable>) ultimateChildClass;
            } else {
                var ctd = stateNonNull(
                        SchemaRegistry.get().findComplexTypeDefinitionByCompileTimeClass(currentParentClass),
                        "No complex type definition for %s", currentParentClass);
                ItemDefinition<?> childDef = requireNonNull(
                        ctd.findItemDefinition(first),
                        () -> "No definition for " + first + " in " + ctd);
                //noinspection unchecked
                currentChildClass = (Class<? extends Containerable>) requireNonNull(
                        childDef.getTypeClass(),
                        () -> "No static class defined for " + childDef);
            }
            var currentFilter = PrismContext.get().queryFor(currentChildClass)
                    .ownedBy(currentParentClass, first)
                    .filter(currentParentFilter)
                    .buildFilter();
            if (currentPath.size() == 1) {
                return currentFilter;
            }
            currentParentFilter = currentFilter;
            currentParentClass = currentChildClass;
            currentPath = currentPath.rest();
        }
    }

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        sb.append(" path=").append(path);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "parent selector", parentSelector, indent + 1);
    }

    @Override
    public String toString() {
        return "ParentClause{" +
                "parentSelector=" + parentSelector +
                ", path=" + path +
                "}";
    }
}
