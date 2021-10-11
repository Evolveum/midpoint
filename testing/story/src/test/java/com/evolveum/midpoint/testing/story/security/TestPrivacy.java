/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.security;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests for privacy-enhancing setup. E.g. broad get authorizations, but limited search.
 *
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPrivacy extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "security/privacy");

    protected static final File USERS_FILE = new File(TEST_DIR, "users.xml");

    protected static final String USER_GUYBRUSH_OID = "0cf84e54-b815-11e8-9862-a7c904bd4e94";
    protected static final String USER_ELAINE_OID = "30444e02-b816-11e8-a26d-0380f27eebe6";
    protected static final String USER_RAPP_OID = "353265f2-b816-11e8-91c7-333c643c8719";

    protected static final File ROLE_PRIVACY_END_USER_FILE = new File(TEST_DIR, "role-privacy-end-user.xml");
    protected static final String ROLE_PRIVACY_END_USER_OID = "d6f2c30a-b816-11e8-88c5-4f735c761a81";

    protected static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");
    protected static final String RESOURCE_DUMMY_OID = "dfc012e2-b813-11e8-82af-679b6f0a6ad4";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(null, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);
        getDummyResource().setSyncStyle(DummySyncStyle.SMART);

        repoAddObjectFromFile(ROLE_PRIVACY_END_USER_FILE, initResult);

        importObjectsFromFileNotRaw(USERS_FILE, initTask, initResult);
    }

    /**
     * MID-4892
     */
    @Test
    public void test100AutzJackReadSearch() throws Exception {
        assignRole(USER_JACK_OID, ROLE_PRIVACY_END_USER_OID);

        login(USER_JACK_USERNAME);

        // WHEN
        when();

        assertGetAllow(UserType.class, USER_GUYBRUSH_OID);
        assertGetAllow(UserType.class, USER_ELAINE_OID);
        assertGetAllow(UserType.class, USER_RAPP_OID);

        assertSearch(UserType.class, null,
                USER_ADMINISTRATOR_OID, USER_GUYBRUSH_OID, USER_ELAINE_OID, USER_JACK_OID);

        // THEN
        then();

    }

}
