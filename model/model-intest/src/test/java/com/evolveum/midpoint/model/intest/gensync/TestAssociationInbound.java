/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.gensync;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyPrivilege;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Created by Viliam Repan (lazyman).
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestAssociationInbound extends AbstractGenericSyncTest {

    public static final File RESOURCE_DUMMY_PURPLE_FILE = new File(TEST_DIR, "resource-dummy-purple.xml");
    public static final String RESOURCE_DUMMY_PURPLE_OID = "10000000-0000-0000-0000-000000001105";
    public static final String RESOURCE_DUMMY_PURPLE_NAME = "purple";

    public static final File ACCOUNT_JDOE_FILE = new File(TEST_DIR, "account-jdoe.xml");
    public static final String ACCOUNT_JDOE_OID = "20000000-0000-0000-3333-000000000047";

    public static final File ACCOUNT_CREW_FILE = new File(TEST_DIR, "account-crew.xml");

    public static final File USER_JDOE_FILE = new File(TEST_DIR, "user-jdoe.xml");
    public static final String USER_JDOE_OID = "fd5039c8-ddc8-11e4-8ec7-001e8c717e5c";

    public static final File ROLE_TEST_MATE_FILE = new File(TEST_DIR, "role-test-mate.xml");
    public static final String ROLE_TEST_MATE_OID = "90c332ec-ddc8-11e4-cc3b-001e8c717e5b";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(RESOURCE_DUMMY_PURPLE_NAME, RESOURCE_DUMMY_PURPLE_FILE, RESOURCE_DUMMY_PURPLE_OID,
                controller -> {
                    controller.extendSchemaPirate();
                    controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                            DUMMY_ACCOUNT_ATTRIBUTE_MATE_NAME, String.class, false, true);
                },
                initTask, initResult);

        importObjectFromFile(ACCOUNT_JDOE_FILE);
        importObjectFromFile(ACCOUNT_CREW_FILE);
        importObjectFromFile(USER_JDOE_FILE);
        importObjectFromFile(ROLE_TEST_MATE_FILE);

        DummyAccount jdoe = new DummyAccount("jdoe");
        jdoe.addAttributeValue("privileges", "test-mate");
        getDummyResource(RESOURCE_DUMMY_PURPLE_NAME).addAccount(jdoe);

        DummyPrivilege crew = new DummyPrivilege("test-mate");
        getDummyResource(RESOURCE_DUMMY_PURPLE_NAME).addPrivilege(crew);
    }

    @Test
    public void test100AssociationInboundMateForOrangeResource() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<?> resource = getDummyResourceObject(RESOURCE_DUMMY_PURPLE_NAME);

        logger.info("Resource {}", resource);

        modelService.importFromResource(ACCOUNT_JDOE_OID, task, result);

        PrismObject<UserType> jdoe = getUser(USER_JDOE_OID);
        assertAssigned(jdoe, ROLE_TEST_MATE_OID, RoleType.COMPLEX_TYPE);
    }
}
