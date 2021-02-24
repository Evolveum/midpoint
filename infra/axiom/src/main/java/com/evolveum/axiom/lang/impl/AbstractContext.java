/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition.Scope;
import com.evolveum.concepts.SourceLocation;
import com.evolveum.axiom.api.AxiomValueIdentifier;

abstract class AbstractContext<P extends AbstractContext<?>> implements IdentifierSpaceHolder {

    private final P parent;
    private SourceLocation start;

    private final IdentifierSpaceHolder localSpace;

    public AbstractContext(P context, SourceLocation loc, Scope scope) {
        this(context,loc, new IdentifierSpaceHolderImpl(scope));
    }

    public AbstractContext(P context, SourceLocation loc, IdentifierSpaceHolder space) {
        parent = context;
        start = loc;
        localSpace = space;
    }

    public P parent() {
        return parent;
    }
    protected abstract Optional<AxiomItemDefinition> childItemDef(AxiomName id);


    protected SourceContext rootImpl() {
        return parent.rootImpl();
    }

    public SourceLocation startLocation() {
        return start;
    }

    @Override
    public ValueContext<?> lookup(AxiomName space, AxiomValueIdentifier key) {
        ValueContext<?> maybe = localSpace.lookup(space, key);
        if(maybe != null) {
            return maybe;
        }
        return parent().lookup(space, key);
    }

    @Override
    public void register(AxiomName space, Scope scope, AxiomValueIdentifier key, ValueContext<?> context) {
        switch (scope) {
            case GLOBAL:
                rootImpl().register(space, scope, key, context);
                break;
            case PARENT:
                parent().register(space, Scope.LOCAL, key, context);
                break;
            case LOCAL:
                localSpace.register(space, scope, key, context);
                break;
        default:
            throw new IllegalStateException("Unsupported scope");
        }
    }

    @Override
    public Map<AxiomValueIdentifier, ValueContext<?>> space(AxiomName space) {
        return localSpace.space(space);
    }
}
