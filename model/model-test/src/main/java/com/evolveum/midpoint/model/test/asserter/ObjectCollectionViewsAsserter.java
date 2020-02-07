/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import java.util.List;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;

/**
 * @author semancik
 *
 */
public class ObjectCollectionViewsAsserter<RA> extends AbstractAsserter<RA> {

    private final List<CompiledObjectCollectionView> objectCollectionViews;

    public ObjectCollectionViewsAsserter(List<CompiledObjectCollectionView> objectCollectionViews, RA returnAsserter, String desc) {
        super(returnAsserter, desc);
        this.objectCollectionViews = objectCollectionViews;
    }

    ObjectCollectionViewAsserter<ObjectCollectionViewsAsserter<RA>> forView(CompiledObjectCollectionView view) {
        ObjectCollectionViewAsserter<ObjectCollectionViewsAsserter<RA>> asserter = new ObjectCollectionViewAsserter<>(view, this, "view in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    public ObjectCollectionViewFinder<RA> by() {
        return new ObjectCollectionViewFinder<>(this);
    }

    public List<CompiledObjectCollectionView> getViews() {
        return objectCollectionViews;
    }

    public ObjectCollectionViewsAsserter<RA> assertViews(int expected) {
        assertEquals("Wrong number of views in "+desc(), expected, getViews().size());
        return this;
    }

    public ObjectCollectionViewAsserter<ObjectCollectionViewsAsserter<RA>> single() {
        assertViews(1);
        return forView(getViews().get(0));
    }

    @Override
    protected String desc() {
        return "object collection views of " + getDetails();
    }

}
