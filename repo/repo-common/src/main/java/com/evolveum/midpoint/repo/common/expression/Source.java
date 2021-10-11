/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaItemType;
import com.evolveum.prism.xml.ns._public.types_3.ItemType;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class Source<V extends PrismValue,D extends ItemDefinition> extends ItemDeltaItem<V,D> implements DebugDumpable, ShortDumpable {

    private QName name;

    public Source(Item<V,D> itemOld, ItemDelta<V,D> delta, Item<V,D> itemNew, QName name, D definition) {
        super(itemOld, delta, itemNew, definition);
        this.name = name;
    }

    public Source(ItemDeltaItem<V,D> idi, QName name) {
        super(idi);
        this.name = name;
    }

    public QName getName() {
        return name;
    }

    public void setName(QName name) {
        this.name = name;
    }

    public Item<V,D> getEmptyItem() throws SchemaException {
        ItemDefinition definition = getDefinition();
        if (definition == null) {
            throw new IllegalStateException("No definition in source "+this);
        }
        return definition.instantiate(getElementName());
    }

    @Override
    public String toString() {
        return "Source(" + shortDump() + ")";
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(PrettyPrinter.prettyPrint(name)).append(": old=").append(getItemOld()).append(", delta=").append(getDelta()).append(", new=").append(getItemNew());
    }

    public void mediumDump(StringBuilder sb) {
        sb.append("Source ").append(PrettyPrinter.prettyPrint(name)).append(":\n");
        sb.append("  old: ").append(getItemOld()).append("\n");
        sb.append("  delta: ").append(getDelta()).append("\n");
        if (getSubItemDeltas() != null) {
            sb.append("  subitem deltas: ").append(getSubItemDeltas()).append("\n");
        }
        sb.append("  new: ").append(getItemNew());
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("Source ").append(PrettyPrinter.prettyPrint(name));
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "old", getItemOld(), indent +1);
        DebugUtil.debugDumpWithLabelLn(sb, "delta", getDelta(), indent +1);
        DebugUtil.debugDumpWithLabelLn(sb, "new", getItemNew(), indent +1);
        return sb.toString();
    }

    @NotNull
    public ItemDeltaItemType toItemDeltaItemType(PrismContext prismContext) throws SchemaException {
        ItemDeltaItemType rv = new ItemDeltaItemType();
        rv.setOldItem(ItemType.fromItem(getItemOld(), prismContext));
        ItemDelta<V, D> delta = getDelta();
        if (delta != null) {
            rv.getDelta().addAll(DeltaConvertor.toItemDeltaTypes(delta));
        }
        rv.setNewItem(ItemType.fromItem(getItemNew(), prismContext));
        return rv;
    }
}
