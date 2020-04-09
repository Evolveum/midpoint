/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ExistsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.Visitor;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

/**
 * TODO think about creating abstract ItemFilter (ItemRelatedFilter) for this filter and ValueFilter.
 *
 * @author lazyman
 * @author mederly
 */
public final class ExistsFilterImpl extends ObjectFilterImpl implements ExistsFilter {

    @NotNull private final ItemPath fullPath;
    private ItemDefinition definition;
    private ObjectFilter filter;

    private ExistsFilterImpl(@NotNull ItemPath fullPath, ItemDefinition definition, ObjectFilter filter) {
        this.fullPath = fullPath;
        this.definition = definition;
        this.filter = filter;
        checkConsistence(true);
    }

    @NotNull
    @Override
    public ItemPath getFullPath() {
        return fullPath;
    }

    public ItemDefinition getDefinition() {
        return definition;
    }

    public ObjectFilter getFilter() {
        return filter;
    }

    public void setFilter(ObjectFilter filter) {
        this.filter = filter;
    }

    public static <C extends Containerable> ExistsFilter createExists(ItemPath itemPath, PrismContainerDefinition<C> containerDef,
                                                                      ObjectFilter filter) throws SchemaException {
        ItemDefinition itemDefinition = FilterImplUtil.findItemDefinition(itemPath, containerDef);
        return new ExistsFilterImpl(itemPath, itemDefinition, filter);
    }

    public static <C extends Containerable> ExistsFilter createExists(ItemPath itemPath, Class<C> clazz, PrismContext prismContext,
                                                                      ObjectFilter filter) {
        ItemDefinition itemDefinition = FilterImplUtil.findItemDefinition(itemPath, clazz, prismContext);
        return new ExistsFilterImpl(itemPath, itemDefinition, filter);
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public ExistsFilterImpl clone() {
        ObjectFilter f = filter != null ? filter.clone() : null;
        return new ExistsFilterImpl(fullPath, definition, f);
    }

    public ExistsFilter cloneEmpty() {
        return new ExistsFilterImpl(fullPath, definition, null);
    }

    @Override
    public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        Item itemToFind = value.findItem(fullPath);
        if (itemToFind == null || itemToFind.getValues().isEmpty()) {
            return false;
        }
        if (!(itemToFind instanceof PrismContainer)) {
            throw new SchemaException("Couldn't use exists query to search for items other than containers: " + itemToFind);
        }
        if (filter == null) {
            return true;
        }
        for (PrismContainerValue<?> pcv : ((PrismContainer<?>) itemToFind).getValues()) {
            if (filter.match(pcv, matchingRuleRegistry)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void checkConsistence(boolean requireDefinitions) {
        if (fullPath.isEmpty()) {
            throw new IllegalArgumentException("Null or empty path in "+this);
        }
        if (requireDefinitions && definition == null) {
            throw new IllegalArgumentException("Null definition in "+this);
        }
        // null subfilter is legal. It means "ALL".
        if (filter != null) {
            filter.checkConsistence(requireDefinitions);
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("EXISTS: ");
        sb.append(fullPath);
        sb.append('\n');
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("DEF: ");
        if (getDefinition() != null) {
            sb.append(getDefinition().toString());
        } else {
            sb.append("null");
        }
        sb.append("\n");
        if (filter != null) {
            sb.append(filter.debugDump(indent + 1));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EXISTS(");
        sb.append(PrettyPrinter.prettyPrint(fullPath));
        sb.append(",");
        sb.append(filter);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public void accept(Visitor visitor) {
        super.accept(visitor);
        if (filter != null) {
            visitor.visit(filter);
        }
    }

    @Override
    public boolean equals(Object o, boolean exact) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExistsFilterImpl that = (ExistsFilterImpl) o;

        if (!fullPath.equals(that.fullPath, exact)) return false;
        if (exact) {
            if (definition != null ? !definition.equals(that.definition) : that.definition != null) {
                return false;
            }
        }
        return filter != null ? filter.equals(that.filter, exact) : that.filter == null;
    }

    // Just to make checkstyle happy
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (definition != null ? definition.hashCode() : 0);
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        return result;
    }
}
