/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.util.AbstractSpringTest;
import com.evolveum.midpoint.transport.impl.TransportUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralTransportConfigurationType;

@ContextConfiguration(locations = { "classpath:ctx-notifications-test.xml" })
public class TestTransportUtils extends AbstractSpringTest {

    @Autowired protected ExpressionFactory expressionFactory;
    @Autowired protected TaskManager taskManager;

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void test010CheckVariablesWhiteList() {
        given();
        GeneralTransportConfigurationType config = new GeneralTransportConfigurationType();

        config.getWhiteList().add("*@evolveum.com");
        config.getWhiteList().add("janko@evodevel.com");
        config.getWhiteList().add("vi*@evodevel.com");
        config.getWhiteList().add("majka@evodevel.*");

        Task task = taskManager.createTaskInstance();
        List<String> allowRecipient = new ArrayList<>();
        List<String> forbiddenRecipient = new ArrayList<>();

        List<String> recipients = new ArrayList<>();
        recipients.add("janko@evodevel.com");
        recipients.add("janko@evolveum.com");
        recipients.add("viliam@evodevel.com");
        recipients.add("majka@evodevel.eu");
        recipients.add("jack@evodevel.sk");
        recipients.add("janko@evolveum.eu");

        when();
        TransportUtil.validateRecipient(allowRecipient, forbiddenRecipient, recipients, config, task,
                task.getResult(), expressionFactory, MiscSchemaUtil.getExpressionProfile(), logger);

        then();
        assertThat(allowRecipient).as("allowRecipient").hasSize(4);
        assertTrue("janko@evodevel.com should be allowed, but isn't.", allowRecipient.contains("janko@evodevel.com"));
        assertTrue("janko@evolveum.com should be allowed, but isn't.", allowRecipient.contains("janko@evolveum.com"));
        assertTrue("viliam@evodevel.com should be allowed, but isn't.", allowRecipient.contains("viliam@evodevel.com"));
        assertTrue("majka@evodevel.eu should be allowed, but isn't.", allowRecipient.contains("majka@evodevel.eu"));

        assertThat(forbiddenRecipient).as("forbiddenRecipient").hasSize(2);
        assertTrue("jack@evodevel.sk should be forbidden, but isn't.", forbiddenRecipient.contains("jack@evodevel.sk"));
        assertTrue("janko@evolveum.eu should be forbidden, but isn't.", forbiddenRecipient.contains("janko@evolveum.eu"));
    }


    @Test
    public void test020CheckVariablesBlackList() {
        given();
        GeneralTransportConfigurationType config = new GeneralTransportConfigurationType();

        config.getBlackList().add("*@evolveum.com");
        config.getBlackList().add("janko@evodevel.com");
        config.getBlackList().add("vi*@evodevel.com");
        config.getBlackList().add("majka@evodevel.*");

        Task task = taskManager.createTaskInstance();
        List<String> allowRecipient = new ArrayList<>();
        List<String> forbiddenRecipient = new ArrayList<>();

        List<String> recipients = new ArrayList<>();
        recipients.add("janko@evodevel.com");
        recipients.add("janko@evolveum.com");
        recipients.add("viliam@evodevel.com");
        recipients.add("majka@evodevel.eu");
        recipients.add("jack@evodevel.sk");
        recipients.add("janko@evolveum.eu");

        when();
        TransportUtil.validateRecipient(allowRecipient, forbiddenRecipient, recipients, config, task,
                task.getResult(), expressionFactory, MiscSchemaUtil.getExpressionProfile(), logger);

        then();
        assertThat(forbiddenRecipient).withFailMessage("forbiddenRecipient").hasSize(4);
        assertTrue("janko@evodevel.com should be forbidden, but isn't.", forbiddenRecipient.contains("janko@evodevel.com"));
        assertTrue("janko@evolveum.com should be forbidden, but isn't.", forbiddenRecipient.contains("janko@evolveum.com"));
        assertTrue("viliam@evodevel.com should be forbidden, but isn't.", forbiddenRecipient.contains("viliam@evodevel.com"));
        assertTrue("majka@evodevel.eu should be forbidden, but isn't.", forbiddenRecipient.contains("majka@evodevel.eu"));

        assertThat(allowRecipient).withFailMessage("allowRecipient").hasSize(2);
        assertTrue("jack@evodevel.sk should be allowed, but isn't.", allowRecipient.contains("jack@evodevel.sk"));
        assertTrue("janko@evolveum.eu should be allowed, but isn't.", allowRecipient.contains("janko@evolveum.eu"));
    }
}
