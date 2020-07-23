/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.antlr;

public class AxiomAntlrVisitor2<T> extends AbstractAxiomAntlrVisitor<T> {

    private final AntlrStreamToItemStream adapter;

    public AxiomAntlrVisitor2(String sourceName, AntlrStreamToItemStream adapter) {
        super(sourceName);
        this.adapter = adapter;
    }

    @Override
    protected AntlrStreamToItemStream delegate() {
        return adapter;
    }
}
