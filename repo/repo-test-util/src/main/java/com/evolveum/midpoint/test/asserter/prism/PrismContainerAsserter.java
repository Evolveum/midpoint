/**
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author semancik
 *
 */
public class PrismContainerAsserter<C extends Containerable, RA> extends PrismItemAsserter<PrismContainer<C>, RA> {

    public PrismContainerAsserter(PrismContainer<C> container) {
        super(container);
    }

    public PrismContainerAsserter(PrismContainer<C> container, String detail) {
        super(container, detail);
    }

    public PrismContainerAsserter(PrismContainer<C> container, RA returnAsserter, String detail) {
        super(container, returnAsserter, detail);
    }

    @Override
    public PrismContainerAsserter<C,RA> assertSize(int expected) {
        super.assertSize(expected);
        return this;
    }

    @Override
    public PrismItemAsserter<PrismContainer<C>, RA> assertComplete() {
        super.assertComplete();
        return this;
    }

    @Override
    public PrismItemAsserter<PrismContainer<C>, RA> assertIncomplete() {
        super.assertIncomplete();
        return this;
    }

    protected String desc() {
        return getDetails();
    }

}
