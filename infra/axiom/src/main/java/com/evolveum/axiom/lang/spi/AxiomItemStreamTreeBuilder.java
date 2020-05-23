/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.spi;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItem;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomItemStream;
import com.evolveum.axiom.lang.api.AxiomItemValue;
import com.google.common.base.Preconditions;


public class AxiomItemStreamTreeBuilder implements AxiomItemStream.Listener {

    private final Deque<Builder> queue = new LinkedList<>();

    public AxiomItemStreamTreeBuilder(ValueBuilder root) {
        queue.add(root);
    }

    protected <V extends Builder> V offer(V builder) {
        queue.offerFirst(builder);
        return builder;
    }

    protected Builder current() {
        return queue.peek();
    }

    protected Builder poll() {
        return queue.poll();
    }

    private ItemBuilder item(Builder node) {
        Preconditions.checkState(node instanceof ItemBuilder);
        return (ItemBuilder) node;
    }

    private ValueBuilder value(Builder node) {
        Preconditions.checkState(node instanceof ValueBuilder);
        return (ValueBuilder) node;
    }

    @Override
    public void startValue(Object value, SourceLocation loc) {
        queue.offerFirst(item(current()).startValue(value, loc));
    }

    @Override
    public void endValue(SourceLocation loc) {
        value(poll()).endValue(loc);
    }

    @Override
    public void startItem(AxiomIdentifier item, SourceLocation loc) {
        Optional<AxiomItemDefinition> childDef = value(current()).childDef(item);
        AxiomSyntaxException.check(childDef.isPresent(), loc , "Item %s not allowed in %s", item, current().name());
        offer(value(current()).startItem(item, loc));
    }

    @Override
    public void endItem(SourceLocation loc) {
        item(poll()).endNode(loc);
    }

    private interface Builder {
        AxiomIdentifier name();
    }

    public interface ItemBuilder extends Builder {
        ValueBuilder startValue(Object value, SourceLocation loc);
        void endNode(SourceLocation loc);

    }

    public interface ValueBuilder extends Builder {
        Optional<AxiomItemDefinition> childDef(AxiomIdentifier statement);
        ItemBuilder startItem(AxiomIdentifier identifier, SourceLocation loc);
        void endValue(SourceLocation loc);
    }

    public void stream(AxiomItem<?> itemDef) {
        for(AxiomItemValue<?> value : itemDef.values()) {
            startItem(itemDef.name(), null);
                stream(value);
            endItem(null);

        }
    }

    private void stream(AxiomItemValue<?> value) {
        Object valueMapped = value.get();
        startValue(valueMapped, null);
        for(AxiomItem<?> item : value.items()) {
            stream(item);
        }
        endValue(null);
    }

}
