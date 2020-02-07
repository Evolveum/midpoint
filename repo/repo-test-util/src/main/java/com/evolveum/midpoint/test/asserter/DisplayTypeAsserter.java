/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertNotNull;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

/**
 * @author semancik
 *
 */
public class DisplayTypeAsserter<RA> extends AbstractAsserter<RA> {

    private final DisplayType displayType;

    public DisplayTypeAsserter(DisplayType displayType, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.displayType = displayType;
    }

    DisplayType getDisplayType() {
        assertNotNull("Null " + desc(), displayType);
        return displayType;
    }

    public DisplayTypeAsserter<RA> assertNull() {
        AssertJUnit.assertNull("Unexpected " + desc(), displayType);
        return this;
    }

    public DisplayTypeAsserter<RA> assertLabel(String expectedOrig) {
        PrismAsserts.assertEqualsPolyString("Wrong label in "+desc(), expectedOrig, displayType.getLabel());
        return this;
    }

    public DisplayTypeAsserter<RA> assertPluralLabel(String expectedOrig) {
        PrismAsserts.assertEqualsPolyString("Wrong pluralLabel in "+desc(), expectedOrig, displayType.getPluralLabel());
        return this;
    }

    public IconTypeAsserter<DisplayTypeAsserter<RA>> icon() {
        IconTypeAsserter<DisplayTypeAsserter<RA>> displayAsserter = new IconTypeAsserter<>(displayType.getIcon(), this, "in " + desc());
        copySetupTo(displayAsserter);
        return displayAsserter;
    }

    public DisplayTypeAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, displayType);
        return this;
    }

    @Override
    protected String desc() {
        return descWithDetails("display");
    }

}
