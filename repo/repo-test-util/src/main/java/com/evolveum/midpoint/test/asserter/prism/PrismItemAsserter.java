/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.util.DebugUtil;

import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 */
public abstract class PrismItemAsserter<I extends Item, RA> extends AbstractAsserter<RA> {

    private I item;

    public PrismItemAsserter(I item) {
        super();
        this.item = item;
    }

    public PrismItemAsserter(I item, String detail) {
        super(detail);
        this.item = item;
    }

    public PrismItemAsserter(I item, RA returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.item = item;
    }

    public I getItem() {
        return item;
    }

    public PrismItemAsserter<I,RA> assertSize(int expected) {
        assertEquals("Wrong number of values in "+desc(), expected, item != null ? item.size() : 0);
        return this;
    }

    public PrismItemAsserter<I,RA> assertNullOrNoValues() {
        assertTrue("Wrong number of values in "+desc()+": "+item, item == null || item.hasNoValues());
        return this;
    }

    public PrismItemAsserter<I,RA> assertComplete() {
        assertFalse("Expected complete item, but it was incomplete "+desc(), item.isIncomplete());
        return this;
    }

    public PrismItemAsserter<I,RA> assertIncomplete() {
        assertTrue("Expected incomplete item, but it was complete "+desc(), item.isIncomplete());
        return this;
    }

    public PrismItemAsserter<I,RA> assertHasDefinition() {
        assertNotNull("Expected definition but none is present "+desc(), item.getDefinition());
        return this;
    }

    // TODO

    protected String desc() {
        return getDetails();
    }

    public PrismItemAsserter<I, RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(item));
        return this;
    }
}
