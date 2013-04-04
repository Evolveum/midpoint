/*
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or CDDLv1.0.txt file in the source
 * code distribution. See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * 
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.controller;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.*;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-no-repo.xml" })
public class ControllerListResourceObjectShadowsTest extends AbstractTestNGSpringContextTests {

    private static final File TEST_FOLDER = new File("./src/test/resources/controller/listObjects");
    private static final File TEST_FOLDER_COMMON = new File("./src/test/resources/common");
    private static final Trace LOGGER = TraceManager.getTrace(ControllerListResourceObjectShadowsTest.class);
    @Autowired(required = true)
    private ModelController controller;
    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private RepositoryService repository;
    @Autowired(required = true)
    private ProvisioningService provisioning;
    @Autowired(required = true)
    private TaskManager taskManager;

    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
    
    @BeforeMethod
    public void before() {
        Mockito.reset(repository, provisioning);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullResourceOid() throws Exception {
        controller.listResourceObjectShadows(null, null, null, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void emptyResourceOid() throws Exception {
        controller.listResourceObjectShadows("", null, null, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullClassType() throws Exception {
        controller.listResourceObjectShadows("1", null, null, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullResult() throws Exception {
        controller.listResourceObjectShadows("1", ShadowType.class, null, null);
    }

    @Test
    @SuppressWarnings("unchecked")
    public <T extends ShadowType> void correctList() throws Exception {

        final String resourceOid = "abababab-abab-abab-abab-000000000001";
        final List<PrismObject<? extends Objectable>> expected = PrismTestUtil.parseObjects(new File(TEST_FOLDER, "resource-object-shadow-list.xml"));
        LOGGER.warn("TODO: File resource-object-shadow-list.xml doesn't contain proper resource object shadow list.");

        when(
                repository.listResourceObjectShadows(eq(resourceOid),
                        eq((Class<T>) ShadowType.class), any(OperationResult.class)))
                .thenReturn((List)expected);

        Task task = taskManager.createTaskInstance("List Resource Object Shadows");
        try {
            List<PrismObject<T>> returned = controller.listResourceObjectShadows(resourceOid,
                    (Class<T>) ShadowType.class, task, task.getResult());

            assertNotNull(expected);
            assertNotNull(returned);
            assertShadowListType(expected, returned);
        } finally {
            LOGGER.debug(task.getResult().dump());
        }
    }

    private <T extends ShadowType> void assertShadowListType(List<PrismObject<? extends Objectable>> expectedList,
            List<PrismObject<T>> returnedList) {

        assertTrue(expectedList == null ? returnedList == null : returnedList != null);
        assertEquals("Unexpected number of results", expectedList.size(), expectedList.size());

        for (int i = 0; i < expectedList.size(); i++) {
            PrismAsserts.assertEquals("Shadows do not match", expectedList.get(i),returnedList.get(i));
        }
    }
}
