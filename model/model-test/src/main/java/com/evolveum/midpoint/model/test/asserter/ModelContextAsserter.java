/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class ModelContextAsserter<O extends ObjectType,RA> extends AbstractAsserter<RA> {

    private ModelContext<O> modelContext;

    public ModelContextAsserter(ModelContext<O> modelContext) {
        super();
        this.modelContext = modelContext;
    }

    public ModelContextAsserter(ModelContext<O> modelContext, String details) {
        super(details);
        this.modelContext = modelContext;
    }

    public ModelContextAsserter(ModelContext<O> modelContext, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.modelContext = modelContext;
    }

    public ModelContext<O> getModelContext() {
        return modelContext;
    }

    public static <O extends ObjectType> ModelContextAsserter<O,Void> forContext(ModelContext<O> ctx) {
        return new ModelContextAsserter<>(ctx);
    }

    public static <O extends ObjectType> ModelContextAsserter<O,Void> forContext(ModelContext<O> ctx, String details) {
        return new ModelContextAsserter<>(ctx, details);
    }

    public FocusContextAsserter<O,? extends ModelContextAsserter<O,RA>> focusContext() {
        FocusContextAsserter<O,? extends ModelContextAsserter<O,RA>> asserter = new FocusContextAsserter<>(modelContext.getFocusContext(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ProjectionContextsAsserter<O, ? extends ModelContextAsserter<O,RA>, RA> projectionContexts() {
        ProjectionContextsAsserter<O, ? extends ModelContextAsserter<O,RA>, RA> asserter = new ProjectionContextsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    protected String desc() {
        return "model context " + modelContext;
    }

    public ModelContextAsserter<O,RA> display(String message) {
        PrismTestUtil.display(message, modelContext);
        return this;
    }

    public ModelContextAsserter<O,RA> display() {
        display(desc());
        return this;
    }
}
