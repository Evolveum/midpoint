/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.extensions.AbstractDelegatedPrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;

import org.jetbrains.annotations.NotNull;

/**
 * PrismValueDeltaSetTriple that also remembers the source from which it has originated.
 *
 * @author semancik
 */
public class SourceTriple<V extends PrismValue,D extends ItemDefinition<?>> extends AbstractDelegatedPrismValueDeltaSetTriple<V> {

    @NotNull
    private final Source<V,D> source;

    public SourceTriple(@NotNull Source<V, D> source, PrismContext prismContext) {
        super(prismContext);
        this.source = source;
    }

    @NotNull
    public Source<V,D> getSource() {
        return source;
    }

    @NotNull
    public QName getName() {
        return source.getName();
    }

    public ItemPath getResidualPath() {
        return source.getResidualPath();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        super.shortDump(sb);
        sb.append(", source=").append(source);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SourceTriple(");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }
}
