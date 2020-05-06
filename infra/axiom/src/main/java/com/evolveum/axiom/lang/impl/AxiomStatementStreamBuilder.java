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
import com.evolveum.axiom.lang.impl.AbstractAxiomStatement.Context;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;


public class AxiomStatementStreamBuilder implements AxiomStatementStreamListener {


    private static final AxiomIdentifier ROOT = AxiomIdentifier.from("root","root");

    private final AxiomStatementFactoryContext factories;
    private final Deque<AbstractAxiomStatement.Context> queue = new LinkedList<>();
    private final AbstractAxiomStatement.Context<?> result;

    public AxiomStatementStreamBuilder(AxiomItemDefinition item, AxiomStatementFactoryContext factories) {
        this.factories = factories;
        result = createBuilder(fakeRootItem(item));
        queue.offerFirst(result);
    }

    public static AxiomStatementStreamBuilder create(AxiomItemDefinition item, AxiomStatementFactoryContext factories) {
        return new AxiomStatementStreamBuilder(item, factories);
    }

    protected Context current() {
        return queue.peek();
    }

    @Override
    public void argument(AxiomIdentifier identifier, int line, int posInLine) {
        argument0(identifier, line, posInLine);
    }

    @Override
    public void argument(String identifier, int line, int posInLine) {
        argument0(identifier, line, posInLine);
    }

    private void argument0(Object value, int line, int posInLine) {
        Optional<AxiomItemDefinition> argDef = current().argumentDef();
        if(argDef.isPresent()) {
            startStatement(argDef.get().identifier(), line, posInLine);
            current().setValue(value);

            endStatement();
        } else {
            current().setValue(value);
        }
    }

    @Override
    public void startStatement(AxiomIdentifier statement, int line, int posInLine) throws AxiomSyntaxException {
        Optional<AxiomItemDefinition> childDef = current().childDef(statement);
        AxiomSyntaxException.check(childDef.isPresent(), "", line, posInLine, "Statement %s not allowed in %s", statement, current().identifier());

        queue.offerFirst(createBuilder(childDef.get()));
    }

    private Context<?> createBuilder(AxiomItemDefinition item) {
        return AbstractAxiomStatement.builder(item, factories.factoryFor(item.type()));
    }

    private static AxiomItemDefinition fakeRootItem(AxiomItemDefinition item) {
        return new DefaultItemDefinition(ROOT, fakeType(item));
    }

    private static AxiomTypeDefinition fakeType(AxiomItemDefinition item) {
        return new DefaultTypeDefinition(ROOT, ImmutableMap.of(item.identifier(), item));
    }

    @Override
    public void endStatement() {
        Context<?> current = queue.poll();
        Context<?> parent = queue.peek();
        parent.add(current.build());
    }

    public AxiomStatement<?> result() {
        AxiomStatement<?> resultHolder = result.build();
        return Iterables.getOnlyElement(resultHolder.children());
    }
}
