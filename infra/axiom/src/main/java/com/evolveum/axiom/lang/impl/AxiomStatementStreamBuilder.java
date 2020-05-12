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
    public void argument(AxiomIdentifier identifier, String source, int line, int posInLine) {
        argument0(identifier, source, line, posInLine);
    }

    @Override
    public void argument(String identifier, String source, int line, int posInLine) {
        argument0(identifier, source, line, posInLine);
    }

    private void argument0(Object value, String source, int line, int posInLine) {
            current().setValue(value);

    }

    @Override
    public void startStatement(AxiomIdentifier statement, String sourceName,  int line, int posInLine) throws AxiomSyntaxException {
        Optional<AxiomItemDefinition> childDef = current().childDef(statement);
        AxiomSyntaxException.check(childDef.isPresent(), sourceName, line, posInLine, "Statement %s not allowed in %s", statement, current().identifier());
        queue.offerFirst(createBuilder(childDef.get()));
    }

    private StatementTreeBuilder createBuilder(AxiomItemDefinition item) {
        return current().createChildNode(item.identifier());
    }

    @Override
    public void endStatement() {
        StatementTreeBuilder current = queue.poll();
        context.endStatement(current);
    }

}
