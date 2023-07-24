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
import com.evolveum.midpoint.prism.path.ItemPath;
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
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Expression evaluation source.
 *
 * Basically a named item-delta-item.
 *
 * @author semancik
 */
public class Source<V extends PrismValue, D extends ItemDefinition<?>>
        extends ItemDeltaItem<V, D>
        implements DebugDumpable, ShortDumpable {

    @NotNull private final QName name;

    public Source(Item<V,D> itemOld, ItemDelta<V,D> delta, Item<V,D> itemNew, @NotNull QName name, D definition) {
        super(itemOld, delta, itemNew, definition);
        this.name = name;
    }

    public Source(
            @Nullable Item<V, D> itemOld,
            @Nullable ItemDelta<V, D> delta,
            @Nullable Item<V, D> itemNew,
            @Nullable D definition,
            @NotNull ItemPath resolvePath,
            @Nullable ItemPath residualPath,
            @Nullable Collection<? extends ItemDelta<?, ?>> subItemDeltas,
            @NotNull QName name) {
        super(
                itemOld,
                delta,
                itemNew,
                determineDefinition(itemOld, delta, itemNew, definition),
                resolvePath,
                residualPath,
                subItemDeltas);
        this.name = name;
    }

    public Source(ItemDeltaItem<V,D> idi, @NotNull QName name) {
        super(idi.getItemOld(), idi.getDelta(), idi.getItemNew(), idi.getDefinition());
        this.name = name;
    }

    @NotNull
    public QName getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Source(" + shortDump() + ")";
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(PrettyPrinter.prettyPrint(name))
                .append(": old=").append(getItemOld())
                .append(", delta=").append(getDelta())
                .append(", new=").append(getItemNew());
    }

    public void mediumDump(StringBuilder sb) {
        sb.append("Source ").append(PrettyPrinter.prettyPrint(name)).append(":\n");
        sb.append("  old: ").append(getItemOld()).append("\n");
        sb.append("  delta: ").append(getDelta()).append("\n");
        if (getSubItemDeltas() != null) {
            sb.append("  sub-item deltas: ").append(getSubItemDeltas()).append("\n");
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
    public ItemDeltaItemType toItemDeltaItemType() throws SchemaException {
        ItemDeltaItemType rv = new ItemDeltaItemType();
        rv.setOldItem(ItemType.fromItem(getItemOld(), PrismContext.get()));
        ItemDelta<V, D> delta = getDelta();
        if (delta != null) {
            rv.getDelta().addAll(DeltaConvertor.toItemDeltaTypes(delta));
        }
        rv.setNewItem(ItemType.fromItem(getItemNew(), PrismContext.get()));
        return rv;
    }
}
