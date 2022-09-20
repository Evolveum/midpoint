package com.evolveum.midpoint.testing.story;
/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

import static org.testng.AssertJUnit.*;

import java.io.File;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.impl.polystring.Ascii7PolyStringNormalizer;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestNormalizers extends AbstractModelIntegrationTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "normalizers");

    public static final File SYSTEM_CONFIGURATION_NORMALIZER_ASCII7_FILE = new File(TEST_DIR, "system-configuration-normalizer-ascii7.xml");

    protected static final File USER_TELEKE_FILE = new File(TEST_DIR, "user-teleke.xml");
    protected static final String USER_TELEKE_OID = "e1b0b0a4-17c6-11e8-bfc9-efaa710b614a";
    protected static final String USER_TELEKE_USERNAME = "Téleké";

    protected PrismObject<UserType> userAdministrator;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // System Configuration
        try {
            repoAddObjectFromFile(getSystemConfigurationFile(), initResult);
        } catch (ObjectAlreadyExistsException e) {
            throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
                    "looks like the previous test haven't cleaned it up", e);
        }

        provisioningService.postInit(initResult);
        modelService.postInit(initResult);

        // User administrator
        userAdministrator = repoAddObjectFromFile(AbstractStoryTest.USER_ADMINISTRATOR_FILE, initResult);
        repoAddObjectFromFile(AbstractStoryTest.ROLE_SUPERUSER_FILE, initResult);
        login(userAdministrator);
    }

    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_NORMALIZER_ASCII7_FILE;
    }

    @Test
    public void test000Sanity() throws Exception {
        PolyStringNormalizer prismNormalizer = prismContext.getDefaultPolyStringNormalizer();
        assertTrue("Wrong normalizer class, expected Ascii7PolyStringNormalizer, but was " + prismNormalizer.getClass(),
                prismNormalizer instanceof Ascii7PolyStringNormalizer);
    }

    @Test
    public void test100AddUserJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        addObject(AbstractStoryTest.USER_JACK_FILE, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUserFromRepo(AbstractStoryTest.USER_JACK_OID);
        display("User after jack (repo)", userAfter);
        assertPolyString(userAfter, UserType.F_NAME, AbstractStoryTest.USER_JACK_USERNAME, AbstractStoryTest.USER_JACK_USERNAME.toLowerCase());
    }

    @Test
    public void test110AddUserTeleke() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        addObject(USER_TELEKE_FILE, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUserFromRepo(USER_TELEKE_OID);
        display("User after (repo)", userAfter);
        assertPolyString(userAfter, UserType.F_NAME, USER_TELEKE_USERNAME, "tlek");
        assertPolyString(userAfter, UserType.F_FULL_NAME, "Grafula Félix Téleké z Tölökö", "grafula flix tlek z tlk");
        assertPolyString(userAfter, UserType.F_HONORIFIC_PREFIX, "Grf.", "grf.");
    }

    private void assertPolyString(PrismObject<UserType> user, QName propName, String expectedOrig, String expectedNorm) {
        PrismProperty<PolyString> prop = user.findProperty(ItemName.fromQName(propName));
        PolyString polyString = prop.getRealValue();
        assertNotNull(polyString);
        assertEquals("Wrong user " + propName.getLocalPart() + ".orig", expectedOrig, polyString.getOrig());
        assertEquals("Wrong user \"+propName.getLocalPart()+\".norm", expectedNorm, polyString.getNorm());
    }
}
