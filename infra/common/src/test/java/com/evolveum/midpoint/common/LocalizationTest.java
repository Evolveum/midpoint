/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.common;

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
public class LocalizationTest {

    private static final String MIDPOINT_HOME_PROPERTY = "midpoint.home";

    private static String midpointHome;

    private static LocalizationServiceImpl service;

    @BeforeClass
    public static void beforeClass() {
        midpointHome = System.getProperty(MIDPOINT_HOME_PROPERTY);

        File file = new File(".");
        String newMidpointHome = file.getAbsolutePath() + "/fake-midpoint-home";

        System.setProperty(MIDPOINT_HOME_PROPERTY, newMidpointHome);

        service = new LocalizationServiceImpl();
        service.init();
    }

    @AfterClass
    public static void afterClass() {
        System.setProperty(MIDPOINT_HOME_PROPERTY, midpointHome);
    }

    @Test
    public void localization() throws Exception {
        assertTranslation(service, "standardKey", "standardKeyCustomValue");
        assertTranslation(service, "customMidpointKey", "customMidpointValue");
        assertTranslation(service, "otherKey", "otherValue");
        assertTranslation(service, "ObjectType.name", "Nameee");
        assertTranslation(service, "customSchemaKey", "customSchemaValue");
        assertTranslation(service, "ObjectType.description", "Popis");
    }

    @Test
    public void localizationParams2() throws Exception {
        Object[] params = new Object[2];
        params[0] = "John";
        params[1] = "Couldn't find user with name 'John'";

        String real = service.translate("UserProfileServiceImpl.unknownUser", params, new Locale("sk"));
        String expected = "Couldn't find user with name '" + params[0] + "', reason: " + params[1] + ".";

        AssertJUnit.assertEquals(expected, real);
    }

    @Test
    public void localizationDefaults() throws Exception {
        assertTranslation(service, "unknownKey", "expectedValues", "expectedValues");
    }

    @Test
    public void localizationParams() throws Exception {
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
