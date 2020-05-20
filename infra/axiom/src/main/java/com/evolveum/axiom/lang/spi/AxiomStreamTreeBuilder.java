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
import com.evolveum.axiom.lang.api.AxiomItemDefinition;


public class AxiomStreamTreeBuilder implements AxiomStatementStreamListener {

    private final Deque<NodeBuilder> queue = new LinkedList<>();

    public AxiomStreamTreeBuilder(NodeBuilder root) {
        queue.add(root);
    }

    protected NodeBuilder current() {
        return queue.peek();
    }

    @Override
    public void argument(AxiomIdentifier identifier, SourceLocation loc) {
        argument0(identifier, loc);
    }

    @Override
    public void argument(String identifier, SourceLocation loc) {
        argument0(identifier, loc);
    }

    private void argument0(Object value, SourceLocation loc) {
            current().setValue(value, loc);
    }

    @Override
    public void startStatement(AxiomIdentifier statement, SourceLocation loc) throws AxiomSyntaxException {
        Optional<AxiomItemDefinition> childDef = current().childDef(statement);
        AxiomSyntaxException.check(childDef.isPresent(), loc , "Statement %s not allowed in %s", statement, current().identifier());
        queue.offerFirst(createBuilder(childDef.get(), loc));
    }

    private NodeBuilder createBuilder(AxiomItemDefinition item, SourceLocation loc) {
        return current().startChildNode(item.name(), loc);
    }

    @Override
    public void endStatement(SourceLocation loc) {
        NodeBuilder current = queue.poll();
        current.endNode(loc);
    }

    public interface NodeBuilder {

        void endNode(SourceLocation loc);

        Optional<AxiomItemDefinition> childDef(AxiomIdentifier statement);

        AxiomIdentifier identifier();

        void setValue(Object value, SourceLocation loc);

        NodeBuilder startChildNode(AxiomIdentifier identifier, SourceLocation loc);


    }

}
