/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.delta;

import java.util.Iterator;
import java.util.Objects;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;

public class ContainerTreeDelta<C extends Containerable>
        extends ItemTreeDelta<PrismContainerValue<C>, PrismContainerDefinition<C>, PrismContainer<C>, ContainerTreeDeltaValue<C>> {

    public ContainerTreeDelta(PrismContainerDefinition<C> definition) {
        super(definition);
    }

    @Override
    protected String debugDumpShortName() {
        return "CTD";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        if (DebugUtil.isDetailedDebugDump()) {
            sb.append(debugDumpShortName()).append(": ");
        }
        sb.append(DebugUtil.formatElementName(getItemName()));
        sb.append(": ");
        appendDebugDumpSuffix(sb);

        if (!getValues().isEmpty()) {
            sb.append("\n");
        }

        Iterator<ContainerTreeDeltaValue<C>> i = getValues().iterator();
        while (i.hasNext()) {
            ContainerTreeDeltaValue<C> pval = i.next();
            sb.append(pval.debugDump(indent + 1));
            if (i.hasNext()) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    protected void appendDebugDumpSuffix(StringBuilder sb) {
    }

    @Override
    public ContainerTreeDeltaValue<C> createNewValue() {
        return new ContainerTreeDeltaValue<>();
    }

    public ItemTreeDelta findItemDelta(ItemPath path) {
        return findItemDelta(path, ItemTreeDelta.class);
    }

    public <D extends ItemTreeDelta> D findItemDelta(ItemPath path, Class<D> deltaClass) {
        return findItemDelta(path, deltaClass, false);
    }

    public <D extends ItemTreeDelta> D findOrCreateItemDelta(ItemPath path, Class<D> deltaClass) {
        return findItemDelta(path, deltaClass, true);
    }

    public <D extends ItemTreeDelta> D findItemDelta(ItemPath path, Class<D> deltaClass, boolean createIfNotExists) {
        if (ItemPath.isEmpty(path)) {
            throw new IllegalArgumentException("Empty path specified");
        }

        Long id = path.firstToIdOrNull();
        ContainerTreeDeltaValue<C> val = findValue(id);
        if (val == null) {
            if (!createIfNotExists) {
                return null;
            }

            val = createNewValue();
            val.setId(id);

            addValue(val);
        }

        ItemPath rest = path.startsWithId() ? path.rest() : path;
        return val.findItemDelta(rest, deltaClass, createIfNotExists);
    }

    public ContainerTreeDeltaValue<C> findValue(Long id) {
        if (id == null) {
            if (getDefinition().isSingleValue()) {
                return getSingleValue();
            } else {
                throw new IllegalArgumentException("Attempt to get segment without an ID from a multi-valued container delta " + getItemName());
            }
        }

        return getValues().stream()
                .filter(v -> Objects.equals(id, v.getId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean containsModifications() {
        if (super.containsModifications()) {
            return true;
        }

        return getValues().stream().anyMatch(ContainerTreeDeltaValue::containsModifications);
    }
}
