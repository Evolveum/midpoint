/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Locale;

/**
 * Created by Viliam Repan (lazyman).
 */
@UnusedTestElement("1 test failing, not in suite")
public class LocalizationTest extends AbstractUnitTest {

    private static String midpointHome;

    private static LocalizationServiceImpl service;

    @BeforeClass
    public static void beforeClass() {
        midpointHome = System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY);

        File file = new File(".");
        String newMidpointHome = file.getAbsolutePath() + "/fake-midpoint-home";

        System.setProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY, newMidpointHome);

        service = new LocalizationServiceImpl();
        service.init();
    }

    @AfterClass
    public static void afterClass() {
        System.setProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY, midpointHome);
    }

    @Test
    public void localization() {
        assertTranslation(service, "standardKey", "standardKeyCustomValue");
        assertTranslation(service, "customMidpointKey", "customMidpointValue");
        assertTranslation(service, "otherKey", "otherValue");
        assertTranslation(service, "ObjectType.name", "Nameee");
        assertTranslation(service, "customSchemaKey", "customSchemaValue");
        assertTranslation(service, "ObjectType.description", "Popis");
    }

    @Test
    public void localizationParams2() {
        Object[] params = new Object[2];
        params[0] = "John";
        params[1] = "Couldn't find user with name 'John'";

        String real = service.translate("UserProfileServiceImpl.unknownUser", params, new Locale("sk"));
        String expected = "Couldn't find user with name '" + params[0] + "', reason: " + params[1] + ".";

        AssertJUnit.assertEquals(expected, real);
    }

    @Test
    public void localizationDefaults() {
        assertTranslation(service, "unknownKey", "expectedValues", "expectedValues");
    }

    @Test
    public void localizationParams() {
        Object[] params = new Object[3];
        params[0] = 123;
        params[1] = new LocalizableMessageBuilder().key("someunknownkey").fallbackMessage("fallback").build();
        params[2] = "Joe";

        String real = service.translate("joekey", params, new Locale("sk"));
        String expected = "User Joe with id 123 tried to translate fallback";

        AssertJUnit.assertEquals(expected, real);
    }

    private void assertTranslation(LocalizationService service, String key, String expectedValue) {
        assertTranslation(service, key, null, expectedValue);
    }

    private void assertTranslation(LocalizationService service, String key, String defaultValue, String expectedValue) {
        Locale locale = new Locale("sk");

        String real = service.translate(key, null, locale, defaultValue);

        AssertJUnit.assertEquals("Expected translation for key '" + key + "' was '" + expectedValue
                + "', real '" + real + "'", expectedValue, real);

        LocalizableMessage msg = new LocalizableMessageBuilder().key(key).fallbackMessage(defaultValue).build();
        real = service.translate(msg, locale);
        AssertJUnit.assertEquals("Expected translation for localization message key '" + key + "' was '"
                + expectedValue + "', real '" + real + "'", expectedValue, real);

    }
}
