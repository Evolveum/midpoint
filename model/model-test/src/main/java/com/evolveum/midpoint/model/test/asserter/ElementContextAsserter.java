/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertNull;

import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.prism.ObjectDeltaAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 */
public abstract class ElementContextAsserter<C extends ModelElementContext<O>, O extends ObjectType, RA> extends AbstractAsserter<RA> {

    private final C elementContext;

    public ElementContextAsserter(C elementContext) {
        super();
        this.elementContext = elementContext;
    }

    public ElementContextAsserter(C elementContext, String detail) {
        super(detail);
        this.elementContext = elementContext;
    }

    public ElementContextAsserter(C elementContext, RA returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.elementContext = elementContext;
    }

    public C getElementContext() {
        return elementContext;
    }

    public PrismObjectAsserter<O, ? extends ElementContextAsserter<C, O, RA>> objectOld() {
        PrismObjectAsserter<O, ElementContextAsserter<C, O, RA>> asserter = new PrismObjectAsserter<>(
                elementContext.getObjectOld(), this, "object old in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    public PrismObjectAsserter<O, ? extends ElementContextAsserter<C, O, RA>> objectCurrent() {
        PrismObjectAsserter<O, ElementContextAsserter<C, O, RA>> asserter = new PrismObjectAsserter<>(
                elementContext.getObjectCurrent(), this, "object current in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    public PrismObjectAsserter<O, ? extends ElementContextAsserter<C, O, RA>> objectNew() {
        PrismObjectAsserter<O, ? extends ElementContextAsserter<C, O, RA>> asserter = new PrismObjectAsserter<>(
                elementContext.getObjectNew(), this, "object new in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    public ObjectDeltaAsserter<O, ? extends ElementContextAsserter<C, O, RA>> primaryDelta() {
        ObjectDeltaAsserter<O, ? extends ElementContextAsserter<C, O, RA>> deltaAsserter = new ObjectDeltaAsserter<>(
                elementContext.getPrimaryDelta(), this, "primary delta in " + desc());
        copySetupTo(deltaAsserter);
        return deltaAsserter;
    }

    public ElementContextAsserter<C, O, RA> assertNoPrimaryDelta() {
        assertNull("Unexpected primary delta in " + desc(), elementContext.getPrimaryDelta());
        return this;
    }

    public ObjectDeltaAsserter<O, ? extends ElementContextAsserter<C, O, RA>> summarySecondaryDelta() {
        ObjectDeltaAsserter<O, ? extends ElementContextAsserter<C, O, RA>> deltaAsserter = new ObjectDeltaAsserter<>(
                elementContext.getSummarySecondaryDelta(), this, "summary secondary delta in " + desc());
        copySetupTo(deltaAsserter);
        return deltaAsserter;
    }

    @SuppressWarnings("UnusedReturnValue")
    public ElementContextAsserter<C, O, RA> assertNoSecondaryDelta() {
        assertNull("Unexpected secondary delta in " + desc(), elementContext.getSummarySecondaryDelta());
        return this;
    }

    protected String desc() {
        // TODO: better desc
        return descWithDetails(elementContext);
    }

    public ElementContextAsserter<C, O, RA> display() {
        display(desc());
        return this;
    }

    public ElementContextAsserter<C, O, RA> display(String message) {
        PrismTestUtil.display(message, elementContext);
        return this;
    }

    @SafeVarargs
    public final ElementContextAsserter<C, O, RA> assertEventMarks(TestObject<MarkType>... expectedMarks) {
        assertEventMarks(expectedMarks, elementContext.getMatchingEventMarksOids());
        return this;
    }
}
