/**
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.notifications.impl;

import com.evolveum.midpoint.notifications.impl.formatters.TextFormatter;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author mederly
 */

@ContextConfiguration(locations = {"classpath:ctx-task.xml",
        "classpath:ctx-repo-cache.xml",
        "classpath:ctx-provisioning.xml",
        "classpath*:ctx-repository-test.xml",
        "classpath:ctx-configuration-test.xml",
        "classpath:ctx-common.xml",
        "classpath:ctx-repo-common.xml",
        "classpath:ctx-security.xml",
        "classpath:ctx-security-enforcer.xml",
        "classpath:ctx-audit.xml",
        "classpath:ctx-model.xml",
        "classpath:ctx-model-common.xml",
        "classpath:ctx-notifications-test.xml",
        "classpath*:ctx-notifications.xml"})
public class TestTextFormatter extends AbstractTestNGSpringContextTests {

    public static final String OBJECTS_DIR_NAME = "src/test/resources/objects";
    public static final String USER_JACK_FILE = OBJECTS_DIR_NAME + "/user-jack.xml";
    public static final String ACCOUNT_JACK_FILE = OBJECTS_DIR_NAME + "/account-jack.xml";

    public static final String CHANGES_DIR_NAME = "src/test/resources/changes";
    public static final String USER_JACK_MODIFICATION_FILE = CHANGES_DIR_NAME + "/user-jack-modification.xml";

	protected static final List<ItemPath> auxiliaryPaths = Arrays.asList(
            new ItemPath(UserType.F_FAMILY_NAME),               // for testing purposes
            new ItemPath(ShadowType.F_METADATA),
            new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS),
            new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP),
            new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
            new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
            new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP),
            new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP),
            new ItemPath(ShadowType.F_ITERATION),
            new ItemPath(ShadowType.F_ITERATION_TOKEN),
            new ItemPath(UserType.F_LINK_REF),
            new ItemPath(ShadowType.F_TRIGGER)
    );

    private static final List<ItemPath> synchronizationPaths = Arrays.asList(
            new ItemPath(ShadowType.F_SYNCHRONIZATION_SITUATION),
            new ItemPath(ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION),
            new ItemPath(ShadowType.F_SYNCHRONIZATION_TIMESTAMP),
            new ItemPath(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP));


    @Autowired
    private TextFormatter textFormatter;

    @Autowired
    private PrismContext prismContext;

    static {
        // We set the locale to US to avoid translation of item names.
        // It is crucial that this method is called before TextFormatter class is loaded.
        // Currently this solution suffices but it is quite fragile. If something would change
        // in this respect and breaking it, a different mechanism to set correct locale would need to be used.
        Locale.setDefault(Locale.US);
    }

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void test010FormatUser() throws Exception {

        // GIVEN

        PrismObject<UserType> jack = PrismTestUtil.parseObject(new File(USER_JACK_FILE));
        System.out.println(jack.debugDump());
        // WHEN

        String jackFormattedHideNone = textFormatter.formatObject(jack, null, true);
        System.out.println("no hidden paths + show operational attributes: " + jackFormattedHideNone);

        String jackFormattedHideOper = textFormatter.formatObject(jack, null, false);
        System.out.println("no hidden paths + hide operational attributes: " + jackFormattedHideOper);

        String jackFormattedHideAux = textFormatter.formatObject(jack, auxiliaryPaths, true);
        System.out.println("hide auxiliary paths + show operational attributes: " + jackFormattedHideAux);

        String jackFormattedHideAuxAndOper = textFormatter.formatObject(jack, auxiliaryPaths, false);
        System.out.println("hide auxiliary paths + hide operational attributes: " + jackFormattedHideAuxAndOper);

        // THEN
        // if fails with hidden operational attribute when it should be shown ('hide none'), check the schema.properties 
		final String CREATE_TIMESTAMP = "Created at:";
		final String EFFECTIVE_STATUS = "Effective status: ENABLED";
		final String FAMILY_NAME = "Family name: Sparrow";
		final String SHIP = "ship: Black Pearl";

		assertTrue("hidden operational attribute when it should be shown ('hide none')", jackFormattedHideNone.contains(CREATE_TIMESTAMP));
		assertTrue("hidden auxiliary attribute (effective status) when it should be shown ('hide none')", jackFormattedHideNone.contains(EFFECTIVE_STATUS));
		assertTrue("hidden auxiliary attribute (family name) when it should be shown ('hide none')", jackFormattedHideNone.contains(FAMILY_NAME));
		assertTrue("hidden standard attribute when it should be shown ('hide none')", jackFormattedHideNone.contains(SHIP));

        assertTrue("shown operational attribute when it should be hidden ('hide oper')", !jackFormattedHideOper.contains(CREATE_TIMESTAMP));
        assertTrue("shown operational attribute when it should be shown ('hide oper')", !jackFormattedHideOper.contains(EFFECTIVE_STATUS));
        assertTrue("hidden auxiliary attribute (family name) when it should be shown ('hide oper')", jackFormattedHideOper.contains(FAMILY_NAME));
        assertTrue("hidden standard attribute when it should be shown ('hide oper')", jackFormattedHideOper.contains(SHIP));

        assertTrue("shown auxiliary attribute (metadata) when it should be hidden ('hide aux')", !jackFormattedHideAux.contains(CREATE_TIMESTAMP));
        assertTrue("shown auxiliary attribute (family name) when it should be hidden ('hide aux')", !jackFormattedHideAux.contains(FAMILY_NAME));
        assertTrue("shown auxiliary attribute (effective status) when it should be hidden ('hide aux')", !jackFormattedHideAux.contains(EFFECTIVE_STATUS));
        assertTrue("hidden standard attribute when it should be shown ('hide aux')", jackFormattedHideAux.contains(SHIP));

        assertTrue("shown operational attribute when it should be hidden ('hide aux and oper')", !jackFormattedHideAuxAndOper.contains(CREATE_TIMESTAMP));
        assertTrue("shown auxiliary attribute (effective status) when it should be hidden ('hide aux and oper')", !jackFormattedHideAuxAndOper.contains(EFFECTIVE_STATUS));
        assertTrue("shown auxiliary attribute (family name) when it should be hidden ('hide aux and oper')", !jackFormattedHideAuxAndOper.contains(FAMILY_NAME));
        assertTrue("hidden standard attribute when it should be shown ('hide aux and oper')", jackFormattedHideAuxAndOper.contains(SHIP));

    }


    @Test
    public void test020FormatUserModification() throws Exception {

        // GIVEN

        ObjectDelta<UserType> delta = parseDelta(USER_JACK_MODIFICATION_FILE);
        PrismObject<UserType> jack = PrismTestUtil.parseObject(new File(USER_JACK_FILE));

        System.out.println(delta.debugDump());
        // WHEN

        String deltaFormattedHideNone = textFormatter.formatObjectModificationDelta(delta, null, true, jack, null);
        System.out.println("no hidden paths + show operational attributes: " + deltaFormattedHideNone);

        String deltaFormattedHideOper = textFormatter.formatObjectModificationDelta(delta, null, false, jack, null);
        System.out.println("no hidden paths + hide operational attributes: " + deltaFormattedHideOper);

        String deltaFormattedHideAux = textFormatter.formatObjectModificationDelta(delta, auxiliaryPaths, true, jack, null);
        System.out.println("hide auxiliary paths + show operational attributes: " + deltaFormattedHideAux);

        String deltaFormattedHideAuxAndOper = textFormatter.formatObjectModificationDelta(delta, auxiliaryPaths, false, jack, null);
        System.out.println("hide auxiliary paths + hide operational attributes: " + deltaFormattedHideAuxAndOper);

        // THEN

        checkNotes(deltaFormattedHideAux);
        checkNotes(deltaFormattedHideAuxAndOper);
        checkNotes(deltaFormattedHideNone);
        checkNotes(deltaFormattedHideOper);
        
        // if fails with hidden operational attribute when it should be shown ('hide none'), check the schema.properties  
        final String CREATE_TIMESTAMP = "Created at:";

        assertTrue("hidden operational attribute when it should be shown ('hide none')", deltaFormattedHideNone.contains(CREATE_TIMESTAMP));
        assertTrue("hidden auxiliary attribute (family name) when it should be shown ('hide none')", deltaFormattedHideNone.contains("SPARROW"));
        assertTrue("hidden password change when it should be shown ('hide none')", deltaFormattedHideNone.contains("(protected string)"));
        assertTrue("hidden standard attribute when it should be shown ('hide none')", deltaFormattedHideNone.contains("BLACK PEARL"));

        assertTrue("shown operational attribute when it should be hidden ('hide oper')", !deltaFormattedHideOper.contains(CREATE_TIMESTAMP));
        assertTrue("hidden auxiliary attribute (family name) when it should be shown ('hide oper')", deltaFormattedHideOper.contains("SPARROW"));
        assertTrue("hidden password change when it should be shown ('hide oper')", deltaFormattedHideOper.contains("(protected string)"));
        assertTrue("hidden standard attribute when it should be shown ('hide oper')", deltaFormattedHideOper.contains("BLACK PEARL"));

        assertTrue("shown auxiliary attribute (metadata) when it should be hidden ('hide aux')", !deltaFormattedHideAux.contains(CREATE_TIMESTAMP));
        assertTrue("shown auxiliary attribute (family name) when it should be hidden ('hide aux')", !deltaFormattedHideAux.contains("SPARROW"));
        assertTrue("hidden standard attribute when it should be shown ('hide aux')", deltaFormattedHideAux.contains("BLACK PEARL"));

        assertTrue("shown operational attribute when it should be hidden ('hide aux and oper')", !deltaFormattedHideAuxAndOper.contains(CREATE_TIMESTAMP));
        assertTrue("shown auxiliary attribute (family name) when it should be hidden ('hide aux and oper')", !deltaFormattedHideAuxAndOper.contains("SPARROW"));
        assertTrue("hidden standard attribute when it should be shown ('hide aux and oper')", deltaFormattedHideAuxAndOper.contains("BLACK PEARL"));

    }

    private void checkNotes(String notification) {
        String NOTES_DELIMITER = "Notes:";
        int i = notification.indexOf(NOTES_DELIMITER);
        if (i == -1) {
            throw new AssertionError("No Notes section in " + notification);
        }
        String notes = notification.substring(i);
        assertFalse(notes.contains("Assignment #1"));
        assertFalse(notes.contains("Assignment #2"));
        assertFalse(notes.contains("Assignment #3"));
        assertTrue(notes.contains("Assignment[1]"));
        assertTrue(notes.contains("Assignment[2]"));
        assertFalse(notes.contains("Assignment[3]"));
    }

    @Test
    public void test030FormatAccount() throws Exception {

        // GIVEN

        PrismObject<ShadowType> jack = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_FILE));
        System.out.println(jack.debugDump());
        // WHEN

        String jackFormattedHideNone = textFormatter.formatAccountAttributes(jack.asObjectable(), null, true);
        System.out.println("no hidden paths + show operational attributes: " + jackFormattedHideNone);

        String jackFormattedHideAux = textFormatter.formatAccountAttributes(jack.asObjectable(), auxiliaryPaths, true);
        System.out.println("hide auxiliary paths + show operational attributes: " + jackFormattedHideAux);

        // THEN

		final String NAME = "Name: jack";
		final String PASSWORD = "(protected string)";
		final String ADMINISTRATIVE_STATUS = "Administrative status: ENABLED";
		final String EFFECTIVE_STATUS = "Effective status: ENABLED";

		assertTrue("account name is not shown", jackFormattedHideNone.contains(NAME));
		assertTrue("account password is not shown", jackFormattedHideNone.contains(PASSWORD));
		assertTrue("administrative status is not shown", jackFormattedHideNone.contains(ADMINISTRATIVE_STATUS));
		assertTrue("effective status is not shown", jackFormattedHideNone.contains(EFFECTIVE_STATUS));

        assertTrue("account name is not shown", jackFormattedHideAux.contains(NAME));
        assertTrue("account password is not shown", jackFormattedHideAux.contains(PASSWORD));
        assertTrue("administrative status is not shown", jackFormattedHideAux.contains(ADMINISTRATIVE_STATUS));
        assertTrue("effective status is shown although it should be hidden", !jackFormattedHideAux.contains(EFFECTIVE_STATUS));

//        AssertJUnit.assertTrue("hidden operational attribute when it should be shown ('hide none')", jackFormattedHideNone.contains("createTimestamp:"));
//        AssertJUnit.assertTrue("hidden auxiliary attribute (effective status) when it should be shown ('hide none')", jackFormattedHideNone.contains("Effective Status: ENABLED"));
//        AssertJUnit.assertTrue("hidden auxiliary attribute (family name) when it should be shown ('hide none')", jackFormattedHideNone.contains("Family Name: Sparrow"));
//        AssertJUnit.assertTrue("hidden standard attribute when it should be shown ('hide none')", jackFormattedHideNone.contains("ship: Black Pearl"));
//
//        AssertJUnit.assertTrue("shown operational attribute when it should be hidden ('hide oper')", !jackFormattedHideOper.contains("createTimestamp:"));
//        AssertJUnit.assertTrue("hidden auxiliary attribute when it should be shown ('hide oper')", jackFormattedHideOper.contains("Effective Status: ENABLED"));
//        AssertJUnit.assertTrue("hidden auxiliary attribute (family name) when it should be shown ('hide oper')", jackFormattedHideOper.contains("Family Name: Sparrow"));
//        AssertJUnit.assertTrue("hidden standard attribute when it should be shown ('hide oper')", jackFormattedHideOper.contains("ship: Black Pearl"));
//
//        AssertJUnit.assertTrue("shown auxiliary attribute (metadata) when it should be hidden ('hide aux')", !jackFormattedHideAux.contains("createTimestamp:"));
//        AssertJUnit.assertTrue("shown auxiliary attribute (family name) when it should be hidden ('hide aux')", !jackFormattedHideAux.contains("Family Name: Sparrow"));
//        AssertJUnit.assertTrue("shown auxiliary attribute (effective status) when it should be hidden ('hide aux')", !jackFormattedHideAux.contains("Effective Status: ENABLED"));
//        AssertJUnit.assertTrue("hidden standard attribute when it should be shown ('hide aux')", jackFormattedHideAux.contains("ship: Black Pearl"));
//
//        AssertJUnit.assertTrue("shown operational attribute when it should be hidden ('hide aux and oper')", !jackFormattedHideAuxAndOper.contains("createTimestamp:"));
//        AssertJUnit.assertTrue("shown auxiliary attribute (effective status) when it should be hidden ('hide aux and oper')", !jackFormattedHideAuxAndOper.contains("Effective Status: ENABLED"));
//        AssertJUnit.assertTrue("shown auxiliary attribute (family name) when it should be hidden ('hide aux and oper')", !jackFormattedHideAuxAndOper.contains("Family Name: Sparrow"));
//        AssertJUnit.assertTrue("hidden standard attribute when it should be shown ('hide aux and oper')", jackFormattedHideAuxAndOper.contains("ship: Black Pearl"));

    }


    private ObjectDelta<UserType> parseDelta(String filename) throws JAXBException, SchemaException, IOException {
        ObjectModificationType modElement = PrismTestUtil.parseAtomicValue(new File(filename), ObjectModificationType.COMPLEX_TYPE);
        return DeltaConvertor.createObjectDelta(modElement, UserType.class, prismContext);
    }

}
