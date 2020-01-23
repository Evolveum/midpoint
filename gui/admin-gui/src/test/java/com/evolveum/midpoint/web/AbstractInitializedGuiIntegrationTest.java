/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web;

import static org.testng.AssertJUnit.assertNotNull;

import static com.evolveum.midpoint.web.AdminGuiTestConstants.*;

import com.evolveum.midpoint.prism.path.ItemName;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public abstract class AbstractInitializedGuiIntegrationTest extends AbstractGuiIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractInitializedGuiIntegrationTest.class);

//    protected final static String PIRACY_NS = "http://midpoint.evolveum.com/xml/ns/samples/piracy";
//    protected final static ItemName PIRACY_WEAPON = new ItemName(PIRACY_NS, "weapon");


    protected DummyResource dummyResource;
    protected DummyResourceContoller dummyResourceCtl;
    protected ResourceType resourceDummyType;
    protected PrismObject<ResourceType> resourceDummy;

    protected PrismObject<UserType> userAdministrator;
    protected String accountJackOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        LOGGER.trace("initSystem");
        super.initSystem(initTask, initResult);

        modelService.postInit(initResult);
        userAdministrator = repositoryService.getObject(UserType.class, USER_ADMINISTRATOR_OID, null, initResult);
        login(userAdministrator);

        dummyResourceCtl = DummyResourceContoller.create(null);
        dummyResourceCtl.extendSchemaPirate();
        dummyResource = dummyResourceCtl.getDummyResource();
        resourceDummy = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);
        resourceDummyType = resourceDummy.asObjectable();
        dummyResourceCtl.setResource(resourceDummy);

        repoAddObjectFromFile(USER_JACK_FILE, true, initResult);
        repoAddObjectFromFile(USER_EMPTY_FILE, true, initResult);

        importObjectFromFile(ROLE_MAPMAKER_FILE);

        repoAddObjectsFromFile(ORG_MONKEY_ISLAND_FILE, OrgType.class, initResult);
    }

    @Test
    public void test000PreparationAndSanity() throws Exception {
        final String TEST_NAME = "test000PreparationAndSanity";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assertNotNull("No model service", modelService);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        display(result);
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

    }

}
