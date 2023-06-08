/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.HomePageType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PreviewContainerPanelConfigurationType;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;

import java.util.List;

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

    public CompiledGuiProfileAsserter<RA> assertUserDashboardWidgets(int expectedWidgets) {
        int userDashboardWidgetsCount = countUserDashboardWidgets();
        assertEquals("Wrong number of user dashboard widgets in " + desc(), expectedWidgets, userDashboardWidgetsCount);
        return this;
    }

    public int countUserDashboardWidgets() {
        HomePageType homePage = compiledGuiProfile.getHomePage();
        if (homePage == null) {
            return 0;
        }
        List<PreviewContainerPanelConfigurationType> widgets = homePage.getWidget();
        if (widgets == null) {
            return 0;
        }
        return widgets.size();
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
