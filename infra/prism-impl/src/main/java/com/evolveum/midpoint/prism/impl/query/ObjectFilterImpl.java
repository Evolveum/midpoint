/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import com.evolveum.midpoint.prism.AbstractFreezable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.Visitor;

public abstract class ObjectFilterImpl extends AbstractFreezable implements ObjectFilter {

    protected transient PrismContext prismContext;

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void revive(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    @Override
    public PrismContext getPrismContext() {
        return prismContext;
    }

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    @Override
    protected abstract void performFreeze();

    @Override
    public abstract ObjectFilterImpl clone();
}
