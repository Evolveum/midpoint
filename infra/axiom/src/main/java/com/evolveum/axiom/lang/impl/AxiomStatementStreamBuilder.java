package com.evolveum.axiom.lang.impl;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomStatement;
import com.evolveum.axiom.lang.api.AxiomStatementStreamListener;
import com.evolveum.axiom.lang.impl.AxiomStatementImpl.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;


public class AxiomStatementStreamBuilder implements AxiomStatementStreamListener {


    private static final AxiomIdentifier ROOT = AxiomIdentifier.from("root","root");

    private final Deque<AxiomStatementImpl.Builder> queue = new LinkedList<>();
    private final AxiomStatementImpl.Builder<?> result;

    public AxiomStatementStreamBuilder() {
        result = createBuilder(ROOT);
        queue.offerFirst(result);
    }

    public static AxiomStatementStreamBuilder create() {
        return new AxiomStatementStreamBuilder();
    }

    @Override
    public void argument(AxiomIdentifier identifier) {
        queue.peek().setValue(identifier);
    }

    @Override
    public void argument(String identifier) {
        queue.peek().setValue(identifier);
    }

    @Override
    public void startStatement(AxiomIdentifier statement) {
        queue.offerFirst(createBuilder(statement));
    }

    private Builder<?> createBuilder(AxiomIdentifier statement) {
        return new AxiomStatementImpl.Builder<>(statement);
    }

    @Override
    public void endStatement() {
        Builder<?> current = queue.poll();
        Builder<?> parent = queue.peek();
        parent.add(current.build());
    }

    public AxiomStatement<?> result() {
        AxiomStatement<?> resultHolder = result.build();
        return Iterables.getOnlyElement(resultHolder.children());
    }
}
