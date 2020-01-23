/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.DisplayTypeAsserter;

/**
 * @author semancik
 *
 */
public class ObjectCollectionViewAsserter<RA> extends AbstractAsserter<RA> {

    private final CompiledObjectCollectionView view;

    public ObjectCollectionViewAsserter(CompiledObjectCollectionView view, RA returnAsserter, String desc) {
        super(returnAsserter, desc);
        this.view = view;
    }

    public ObjectCollectionViewAsserter<RA> assertName(String expected) {
        assertEquals("Wrong view name in "+desc(), expected, view.getViewIdentifier());
        return this;
    }

    public ObjectCollectionViewAsserter<RA> assertFilter() {
        assertNotNull("Null filter in "+desc(), view.getFilter());
        return this;
    }

    public ObjectCollectionViewAsserter<RA> assertDomainFilter() {
        assertNotNull("Null domain filter in "+desc(), view.getDomainFilter());
        return this;
    }

    public DisplayTypeAsserter<ObjectCollectionViewAsserter<RA>> displayType() {
        DisplayTypeAsserter<ObjectCollectionViewAsserter<RA>> displayAsserter = new DisplayTypeAsserter<>(view.getDisplay(), this, "in " + desc());
        copySetupTo(displayAsserter);
        return displayAsserter;
    }

    // TODO

    public ObjectFilter getFilter() {
        return view.getFilter();
    }

    @Override
    protected String desc() {
        return descWithDetails(view);
    }

}
