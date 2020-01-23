/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl;

import com.evolveum.midpoint.notifications.impl.api.transports.TransportUtil;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationTransportConfigurationType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertTrue;

/**
 * @author skublik
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
public class TestTransportUtils extends AbstractTestNGSpringContextTests {

    private static final Trace LOGGER = TraceManager.getTrace(TestTransportUtils.class);

    @Autowired protected ExpressionFactory expressionFactory;
    @Autowired protected TaskManager taskManager;

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void test010CheckVariablesWhiteList() {
        final String TEST_NAME = "test010CheckVariablesWhiteList";

        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        NotificationTransportConfigurationType config = new NotificationTransportConfigurationType();

        config.getWhiteList().add("*@evolveum.com");
        config.getWhiteList().add("janko@evodevel.com");
        config.getWhiteList().add("vi*@evodevel.com");
        config.getWhiteList().add("majka@evodevel.*");

        Task task = taskManager.createTaskInstance();
        List<String> allowRecipient = new ArrayList<String>();
        List<String> forbiddenRecipient = new ArrayList<String>();

        List<String> recipients = new ArrayList<String>();
        recipients.add("janko@evodevel.com");
        recipients.add("janko@evolveum.com");
        recipients.add("viliam@evodevel.com");
        recipients.add("majka@evodevel.eu");
        recipients.add("jack@evodevel.sk");
        recipients.add("janko@evolveum.eu");

        // WHEN
        TransportUtil.validateRecipient(allowRecipient, forbiddenRecipient, recipients, config, task, task.getResult(), expressionFactory, MiscSchemaUtil.getExpressionProfile(), LOGGER);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        assertTrue("Expected <4> allowed recipient(s), but was <" +allowRecipient.size()+ ">", allowRecipient.size() == 4);
        assertTrue("janko@evodevel.com shoud be allowed, but isn't.", allowRecipient.contains("janko@evodevel.com"));
        assertTrue("janko@evolveum.com shoud be allowed, but isn't.", allowRecipient.contains("janko@evolveum.com"));
        assertTrue("viliam@evodevel.com shoud be allowed, but isn't.", allowRecipient.contains("viliam@evodevel.com"));
        assertTrue("majka@evodevel.eu shoud be allowed, but isn't.", allowRecipient.contains("majka@evodevel.eu"));

        assertTrue("Expected <2> forbidden recipient(s), but was <" +forbiddenRecipient.size()+ ">", forbiddenRecipient.size() == 2);
        assertTrue("jack@evodevel.sk shoud be forbidden, but isn't.", forbiddenRecipient.contains("jack@evodevel.sk"));
        assertTrue("janko@evolveum.eu shoud be forbidden, but isn't.", forbiddenRecipient.contains("janko@evolveum.eu"));
    }

    @Test
    public void test020CheckVariablesBlackList() {
        final String TEST_NAME = "test020CheckVariablesBlackList";

        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        NotificationTransportConfigurationType config = new NotificationTransportConfigurationType();

        config.getBlackList().add("*@evolveum.com");
        config.getBlackList().add("janko@evodevel.com");
        config.getBlackList().add("vi*@evodevel.com");
        config.getBlackList().add("majka@evodevel.*");

        Task task = taskManager.createTaskInstance();
        List<String> allowRecipient = new ArrayList<String>();
        List<String> forbiddenRecipient = new ArrayList<String>();

        List<String> recipients = new ArrayList<String>();
        recipients.add("janko@evodevel.com");
        recipients.add("janko@evolveum.com");
        recipients.add("viliam@evodevel.com");
        recipients.add("majka@evodevel.eu");
        recipients.add("jack@evodevel.sk");
        recipients.add("janko@evolveum.eu");

        // WHEN
        TransportUtil.validateRecipient(allowRecipient, forbiddenRecipient, recipients, config, task, task.getResult(), expressionFactory, MiscSchemaUtil.getExpressionProfile(), LOGGER);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        assertTrue("Expected <4> forbidden recipient(s), but was <" +forbiddenRecipient.size()+ ">", forbiddenRecipient.size() == 4);
        assertTrue("janko@evodevel.com shoud be forbidden, but isn't.", forbiddenRecipient.contains("janko@evodevel.com"));
        assertTrue("janko@evolveum.com shoud be forbidden, but isn't.", forbiddenRecipient.contains("janko@evolveum.com"));
        assertTrue("viliam@evodevel.com shoud be forbidden, but isn't.", forbiddenRecipient.contains("viliam@evodevel.com"));
        assertTrue("majka@evodevel.eu shoud be forbidden, but isn't.", forbiddenRecipient.contains("majka@evodevel.eu"));

        assertTrue("Expected <2> allowed recipient(s), but was <" +allowRecipient.size()+ ">", allowRecipient.size() == 2);
        assertTrue("jack@evodevel.sk shoud be allowed, but isn't.", allowRecipient.contains("jack@evodevel.sk"));
        assertTrue("janko@evolveum.eu shoud be allowed, but isn't.", allowRecipient.contains("janko@evolveum.eu"));
    }

}
