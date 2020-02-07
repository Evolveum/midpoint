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

import com.evolveum.midpoint.model.api.authentication.CompiledUserProfile;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;

/**
 * @author semancik
 *
 */
public class CompiledUserProfileAsserter<RA> extends AbstractAsserter<RA> {

    private final CompiledUserProfile compiledUserProfile;

    public CompiledUserProfileAsserter(CompiledUserProfile compiledUserProfile, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.compiledUserProfile = compiledUserProfile;
    }

    CompiledUserProfile getCompiledUserProfile() {
        assertNotNull("Null " + desc(), compiledUserProfile);
        return compiledUserProfile;
    }

    public CompiledUserProfileAsserter<RA> assertNull() {
        AssertJUnit.assertNull("Unexpected " + desc(), compiledUserProfile);
        return this;
    }

    public CompiledUserProfileAsserter<RA> assertAdditionalMenuLinks(int expectedMenuLinks) {
        assertEquals("Wrong number of additionalMenuLinks in " + desc(), expectedMenuLinks, getCompiledUserProfile().getAdditionalMenuLink().size());
        return this;
    }

    public CompiledUserProfileAsserter<RA> assertUserDashboardLinks(int expectedLinks) {
        assertEquals("Wrong number of userDashboardLinks in " + desc(), expectedLinks, getCompiledUserProfile().getUserDashboardLink().size());
        return this;
    }

    public CompiledUserProfileAsserter<RA> assertObjectForms(int expectedForms) {
        if (getCompiledUserProfile().getObjectForms() == null) {
            assertTrue("Wrong number of object forms in " + desc() + "; exected " + expectedForms + " but was null", expectedForms == 0);
        } else {
            assertEquals("Wrong number of object forms in " + desc(), expectedForms, getCompiledUserProfile().getObjectForms().getObjectForm().size());
        }
        return this;
    }

    public CompiledUserProfileAsserter<RA> assertUserDashboardWidgets(int expectedWidgetws) {
        if ( compiledUserProfile.getUserDashboard() == null) {
            if (expectedWidgetws != 0) {
                fail("Wrong number of widgets in user dashboard admin GUI configuration, expected "
                        + expectedWidgetws + " but there was none");
            }
        } else {
            assertEquals("Wrong number of user dashboard widgets in " + desc(), expectedWidgetws, getCompiledUserProfile().getUserDashboard().getWidget().size());
        }
        return this;
    }

    public CompiledUserProfileAsserter<RA> assertObjectCollectionViews(int expectedViews) {
        assertEquals("Wrong number of object collection views in " + desc(), expectedViews, getCompiledUserProfile().getObjectCollectionViews().size());
        return this;
    }

    public ObjectCollectionViewsAsserter<CompiledUserProfileAsserter<RA>> objectCollectionViews() {
        ObjectCollectionViewsAsserter<CompiledUserProfileAsserter<RA>> asserter = new ObjectCollectionViewsAsserter<>(getCompiledUserProfile().getObjectCollectionViews(), this, desc());
        copySetupTo(asserter);
        return asserter;
    }

    // TODO: better asserter for views

    public CompiledUserProfileAsserter<RA> display() {
        display(desc());
        return this;
    }

    public CompiledUserProfileAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, compiledUserProfile);
        return this;
    }

    @Override
    protected String desc() {
        return descWithDetails("compiled user profile");
    }

}
