/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api.stream;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.concepts.SourceLocation;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;


public class AxiomBuilderStreamTarget implements AxiomItemStream.TargetWithContext {

    private final Deque<Builder> queue = new LinkedList<>();

    protected AxiomBuilderStreamTarget() {}

    public AxiomBuilderStreamTarget(ValueBuilder root) {
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
        Preconditions.checkState(node instanceof ItemBuilder, "Incorrect nesting: expected item, got value");
        return (ItemBuilder) node;
    }

    private ValueBuilder value(Builder node) {
        Preconditions.checkState(node instanceof ValueBuilder, "Incorrect nesting: expected value, got item");
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
        Optional<AxiomItemDefinition> childDef = value(current()).childItemDef(item);
        AxiomSyntaxException.check(childDef.isPresent(), loc , "Item %s not allowed in %s", item, current().name());
        offer(value(current()).startItem(item, loc));
    }

    @Override
    public void startInfra(AxiomName item, SourceLocation loc) {
        Optional<AxiomItemDefinition> childDef = value(current()).infraItemDef(item);
        AxiomSyntaxException.check(childDef.isPresent(), loc , "Infra Item %s not allowed in %s", item, current().name());
        offer(value(current()).startInfra(item, loc));
    }

    @Override
    public void endInfra(SourceLocation loc) {
        item(poll()).endNode(loc);
    }

    @Override
    public void endItem(SourceLocation loc) {
        item(poll()).endNode(loc);
    }

    private interface Builder {
        AxiomName name();

        AxiomTypeDefinition currentInfra();

        AxiomTypeDefinition currentType();
    }

    public interface ItemBuilder extends Builder {
        ValueBuilder startValue(Object value, SourceLocation loc);
        void endNode(SourceLocation loc);

    }

    public interface ValueBuilder extends Builder {
        Optional<AxiomItemDefinition> childItemDef(AxiomName statement);
        Optional<AxiomItemDefinition> infraItemDef(AxiomName item);
        ItemBuilder startItem(AxiomName identifier, SourceLocation loc);
        ItemBuilder startInfra(AxiomName identifier, SourceLocation loc);
        void endValue(SourceLocation loc);

        default AxiomNameResolver axiomAsConditionalDefault() {
            return (prefix, name) -> {
                if(Strings.isNullOrEmpty(prefix)) {
                    AxiomName axiomNs = AxiomName.axiom(name);
                    if(childItemDef(axiomNs).isPresent()) {
                        return axiomNs;
                    }
                }
                return null;
            };
        }
    }

    @Override
    public AxiomTypeDefinition currentInfra() {
        return current().currentInfra();
    }

    @Override
    public AxiomTypeDefinition currentType() {
        return current().currentType();
    }

}
