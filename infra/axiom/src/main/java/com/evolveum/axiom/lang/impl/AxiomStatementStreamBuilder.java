/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.evolveum.axiom.lang.api.stmt.AxiomStatementStreamListener;
import com.evolveum.axiom.lang.api.stmt.SourceLocation;
import com.google.common.collect.Iterables;


public class AxiomStatementStreamBuilder implements AxiomStatementStreamListener {


    private final ModelReactorContext context;
    private final Deque<StatementTreeBuilder> queue = new LinkedList<>();

    public AxiomStatementStreamBuilder(ModelReactorContext context, StatementTreeBuilder root) {
        this.context = context;
        queue.add(root);
    }

    protected StatementTreeBuilder current() {
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

    private StatementTreeBuilder createBuilder(AxiomItemDefinition item, SourceLocation loc) {
        return current().createChildNode(item.name(), loc);
    }

    @Override
    public void endStatement(SourceLocation loc) {
        StatementTreeBuilder current = queue.poll();
        context.endStatement(current, loc);
    }

}
