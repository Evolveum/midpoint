/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.opendj;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.opends.server.types.Entry;
import org.opends.server.util.LDIFException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Test for connector configuration discovery, featuring OpenDJ server.
 *
 * This test works with a rather non-usual use case.
 * It stores all configuration changes in the repo.
 * Usual GUI wizard will not do that, it will keep resource in memory.
 * This test is supposed to test the least traveled path.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestOpenDjDiscovery extends AbstractOpenDjTest {

    private Collection<PrismProperty<Object>> discoveredProperties;

    @BeforeClass
    public void startLdap() throws Exception {
        logger.info("------------------------------------------------------------------------------");
        logger.info("START:  TestOpenDjDiscovery");
        logger.info("------------------------------------------------------------------------------");
        try {
            openDJController.startCleanServer();
        } catch (IOException ex) {
            logger.error("Couldn't start LDAP.", ex);
            throw ex;
        }
    }

    @AfterClass
    public void stopLdap() {
        openDJController.stop();
        logger.info("------------------------------------------------------------------------------");
        logger.info("STOP:  TestOpenDjDiscovery");
        logger.info("------------------------------------------------------------------------------");
    }

    protected static final File RESOURCE_OPENDJ_DISCOVERY_FILE = new File(TEST_DIR, "resource-opendj-discovery.xml");

    @Override
    protected File getResourceOpenDjFile() {
        return RESOURCE_OPENDJ_DISCOVERY_FILE;
    }


    @Test
    public void test010TestPartialConfiguration() throws Exception {
        Task task = getTestTask();
        PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, task.getResult());

        when();
        OperationResult testResult = provisioningService.testPartialConfigurationResource(resource, task);

        then();
        display("Test connection result", testResult);
        TestUtil.assertSuccess("Test connection failed", testResult);
    }

    @Test
    public void test012DiscoverConfiguration() throws Exception {
        Task task = getTestTask();
        PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, task.getResult());

        when();
        discoveredProperties = provisioningService.discoverConfiguration(resource, task.getResult());

        then();
        display("Discovered properties", discoveredProperties);

        // TODO: assert discovered properties
    }

    /**
     * We are trying to test the connector with an incomplete configuration (we have not applied the discovered configuration yet).
     * Therefore the test should fal.
     */
    @Test
    public void test012TestConnectionFail() throws Exception {
        Task task = getTestTask();

        when();
        OperationResult testResult = provisioningService.testResource(RESOURCE_OPENDJ_OID, task);

        then();
        display("Test connection result", testResult);
        // baseContext is not configured, connector is not fully functional without it.
        // Hence the failure.
        TestUtil.assertFailure("Test connection result (failure expected)", testResult);
    }


    // TODO

    @Test(enabled = false)
    public void test016ApplyDiscoverConfiguration() throws Exception {
        // TODO: Apply discovered properties to resource configuration
    }

    @Test(enabled = false)
    public void test020TestConnection() throws Exception {
        Task task = getTestTask();

        when();
        OperationResult testResult = provisioningService.testResource(RESOURCE_OPENDJ_OID, task);

        then();
        display("Test connection result", testResult);
        TestUtil.assertSuccess("Test connection failed", testResult);
    }
}
