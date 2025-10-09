/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

/**
 * @author semancik
 *
 */
public class IconTypeAsserter<RA> extends AbstractAsserter<RA> {

    private final IconType iconType;

    public IconTypeAsserter(IconType iconType, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.iconType = iconType;
    }

    IconType getIconType() {
        assertNotNull("Null " + desc(), iconType);
        return iconType;
    }

    public IconTypeAsserter<RA> assertNull() {
        AssertJUnit.assertNull("Unexpected " + desc(), iconType);
        return this;
    }

    public IconTypeAsserter<RA> assertCssClass(String expected) {
        assertEquals("Wrong label in "+desc(), expected, iconType.getCssClass());
        return this;
    }

    public IconTypeAsserter<RA> assertColor(String expected) {
        assertEquals("Wrong color in "+desc(), expected, iconType.getColor());
        return this;
    }

    public IconTypeAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, iconType);
        return this;
    }

    @Override
    protected String desc() {
        return descWithDetails("icon");
    }

}
