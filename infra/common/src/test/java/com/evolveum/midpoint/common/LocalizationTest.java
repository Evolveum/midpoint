/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static org.testng.AssertJUnit.assertEquals;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LocalizationTest {

    private static String midpointHome;

    private static LocalizationServiceImpl service;

    private static final File TEST_DIR = new File("src/test/resources/localization");
    private static final File CASE_WITH_LOCALIZED_NAME_FILE = new File(TEST_DIR, "case-with-localized-name.xml");

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

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

        assertEquals(expected, real);
    }

    @Test
    public void localizationDefaults() throws Exception {
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

        assertEquals(expected, real);
    }

    @Test
    public void translationWithArguments() throws SchemaException, IOException {
        PrismObject<CaseType> object = getPrismContext().parserFor(CASE_WITH_LOCALIZED_NAME_FILE).parse();
        PolyString name = object.getName();

        String translated = service.translate(name, new Locale("sk"), false);
        assertEquals("Assignment of Role to User failed.", translated);
    }

    private void assertTranslation(LocalizationService service, String key, String expectedValue) {
        assertTranslation(service, key, null, expectedValue);
    }

    private void assertTranslation(LocalizationService service, String key, String defaultValue, String expectedValue) {
        Locale locale = new Locale("sk");

        String real = service.translate(key, null, locale, defaultValue);

        assertEquals("Expected translation for key '" + key + "' was '" + expectedValue
                + "', real '" + real + "'", expectedValue, real);

        LocalizableMessage msg = new LocalizableMessageBuilder().key(key).fallbackMessage(defaultValue).build();
        real = service.translate(msg, locale);
        assertEquals("Expected translation for localization message key '" + key + "' was '"
                + expectedValue + "', real '" + real + "'", expectedValue, real);

    }
}
