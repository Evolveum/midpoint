/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;

/**
 * @author semancik
 *
 */
public class CompiledGuiProfileAsserter<RA> extends AbstractAsserter<RA> {

    private final CompiledGuiProfile compiledGuiProfile;

    public CompiledGuiProfileAsserter(CompiledGuiProfile compiledGuiProfile, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.compiledGuiProfile = compiledGuiProfile;
    }

    CompiledGuiProfile getCompiledGuiProfile() {
        assertNotNull("Null " + desc(), compiledGuiProfile);
        return compiledGuiProfile;
    }

    public CompiledGuiProfileAsserter<RA> assertNull() {
        AssertJUnit.assertNull("Unexpected " + desc(), compiledGuiProfile);
        return this;
    }

    public CompiledGuiProfileAsserter<RA> assertAdditionalMenuLinks(int expectedMenuLinks) {
        assertEquals("Wrong number of additionalMenuLinks in " + desc(), expectedMenuLinks, getCompiledGuiProfile().getAdditionalMenuLink().size());
        return this;
    }

    public CompiledGuiProfileAsserter<RA> assertUserDashboardLinks(int expectedLinks) {
        assertEquals("Wrong number of userDashboardLinks in " + desc(), expectedLinks, getCompiledGuiProfile().getUserDashboardLink().size());
        return this;
    }

    public CompiledGuiProfileAsserter<RA> assertUserDashboardWidgets(int expectedWidgetws) {
        if ( compiledGuiProfile.getUserDashboard() == null) {
            if (expectedWidgetws != 0) {
                fail("Wrong number of widgets in user dashboard admin GUI configuration, expected "
                        + expectedWidgetws + " but there was none");
            }
        } else {
            assertEquals("Wrong number of user dashboard widgets in " + desc(), expectedWidgetws, getCompiledGuiProfile().getUserDashboard().getWidget().size());
        }
        return this;
    }

    public CompiledGuiProfileAsserter<RA> assertObjectCollectionViews(int expectedViews) {
        assertEquals("Wrong number of object collection views in " + desc(), expectedViews, getCompiledGuiProfile().getObjectCollectionViews().size());
        return this;
    }

    public ObjectCollectionViewsAsserter<CompiledGuiProfileAsserter<RA>> objectCollectionViews() {
        ObjectCollectionViewsAsserter<CompiledGuiProfileAsserter<RA>> asserter = new ObjectCollectionViewsAsserter<>(getCompiledGuiProfile().getObjectCollectionViews(), this, desc());
        copySetupTo(asserter);
        return asserter;
    }

    // TODO: better asserter for views

    public CompiledGuiProfileAsserter<RA> display() {
        display(desc());
        return this;
    }

    public CompiledGuiProfileAsserter<RA> display(String message) {
        PrismTestUtil.display(message, compiledGuiProfile);
        return this;
    }

    @Override
    protected String desc() {
        return descWithDetails("compiled user profile");
    }

}
