/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class ObjectCollectionViewFinder<RA> {

    private final ObjectCollectionViewsAsserter<RA> viewsAsserter;
    private String identifier;

    public ObjectCollectionViewFinder(ObjectCollectionViewsAsserter<RA> viewsAsserter) {
        this.viewsAsserter = viewsAsserter;
    }

    public ObjectCollectionViewFinder<RA> identifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public ObjectCollectionViewAsserter<ObjectCollectionViewsAsserter<RA>> find() throws ObjectNotFoundException, SchemaException {
        CompiledObjectCollectionView found = null;
        for (CompiledObjectCollectionView view: viewsAsserter.getViews()) {
            if (matches(view)) {
                if (found == null) {
                    found = view;
                } else {
                    fail("Found more than one link that matches search criteria");
                }
            }
        }
        if (found == null) {
            fail("Found no link that matches search criteria");
        }
        return viewsAsserter.forView(found);
    }

    public ObjectCollectionViewsAsserter<RA> assertCount(int expectedCount) throws ObjectNotFoundException, SchemaException {
        int foundCount = 0;
        for (CompiledObjectCollectionView view: viewsAsserter.getViews()) {
            if (matches(view)) {
                foundCount++;
            }
        }
        assertEquals("Wrong number of links for specified criteria in "+viewsAsserter.desc(), expectedCount, foundCount);
        return viewsAsserter;
    }

    private boolean matches(CompiledObjectCollectionView view) throws ObjectNotFoundException, SchemaException {

        if (identifier != null) {
            if (!identifier.equals(view.getViewIdentifier())) {
                return false;
            }
        }

        // TODO: more criteria
        return true;
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }

}
