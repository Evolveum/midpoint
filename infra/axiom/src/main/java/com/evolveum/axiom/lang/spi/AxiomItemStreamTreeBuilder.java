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
import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.stream.AxiomItemStream;
import com.evolveum.concepts.SourceLocation;
import com.google.common.base.Preconditions;


public class AxiomItemStreamTreeBuilder implements AxiomItemStream.Target {

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
    public void startItem(AxiomName item, SourceLocation loc) {
        Optional<AxiomItemDefinition> childDef = value(current()).childDef(item);
        AxiomSyntaxException.check(childDef.isPresent(), loc , "Item %s not allowed in %s", item, current().name());
        offer(value(current()).startItem(item, loc));
    }

    @Override
    public void endItem(SourceLocation loc) {
        item(poll()).endNode(loc);
    }

    private interface Builder {
        AxiomName name();
    }

    public interface ItemBuilder extends Builder {
        ValueBuilder startValue(Object value, SourceLocation loc);
        void endNode(SourceLocation loc);

    }

    public interface ValueBuilder extends Builder {
        Optional<AxiomItemDefinition> childDef(AxiomName statement);
        ItemBuilder startItem(AxiomName identifier, SourceLocation loc);
        void endValue(SourceLocation loc);
    }

}
