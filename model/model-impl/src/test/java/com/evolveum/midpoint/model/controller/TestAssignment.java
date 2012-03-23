/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.controller;

import com.evolveum.midpoint.model.test.util.ModelTUtil;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.util.XmlAsserts;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import java.io.File;
import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author lazyman
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:application-context-model.xml",
        "classpath:application-context-model-unit-test.xml",
        "classpath:application-context-configuration-test-no-repo.xml",
        "classpath:application-context-task.xml"})
public class TestAssignment extends AbstractTestNGSpringContextTests {

    private static final File TEST_FOLDER = new File("./src/test/resources/assignment/simple");
    private static final File TEST_FOLDER_COMMON = new File("./src/test/resources/common");
    private static final Trace LOGGER = TraceManager.getTrace(TestAssignment.class);

    @Autowired(required = true)
    private ModelController model;

    @Autowired(required = true)
    @Qualifier("repositoryService")
    private RepositoryService repository;

    @Autowired(required = true)
    private ProvisioningService provisioning;

    @Autowired(required = true)
    private TaskManager taskManager;
    
    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
    
    @BeforeMethod
    public void before() {
        Mockito.reset(repository, provisioning);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void simpleRoleAssignment() throws Exception {

        ModelTUtil.mockGetSystemConfiguration(repository, new File(
                TEST_FOLDER_COMMON, "system-configuration.xml"));
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_FOLDER, "user.xml"));
        final UserType userType = user.asObjectable();
        PrismObject<RoleType> role = PrismTestUtil.parseObject(new File(TEST_FOLDER, "role.xml"));
        final RoleType roleType = role.asObjectable();
        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(TEST_FOLDER_COMMON, "resource-opendj.xml"));
        final ResourceType resourceType = resource.asObjectable();

        when(
                repository.getObject(eq(RoleType.class), eq(roleType.getOid()),
                        any(PropertyReferenceListType.class), any(OperationResult.class))).thenReturn(roleType.asPrismObject());
        when(
                provisioning.getObject(eq(ResourceType.class), eq(resourceType.getOid()),
                        any(PropertyReferenceListType.class), any(OperationResult.class))).thenReturn(
                resourceType.asPrismObject());

        when(
                provisioning.addObject(any(PrismObject.class), any(ScriptsType.class),
                        any(OperationResult.class))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                PrismObject<AccountShadowType> account = (PrismObject<AccountShadowType>) invocation.getArguments()[0];
                LOGGER.info("Created account:\n{}", account.dump());
                PrismAsserts.assertEquivalent("Unexpected account as argument in provisionig.addObject", new File(TEST_FOLDER, "account-expected.xml"), account);

                return "12345678-d34d-b33f-f00d-987987987989";
            }
        });

        when(repository.addObject(any(PrismObject.class), any(OperationResult.class))).thenAnswer(
                new Answer<String>() {
                    @Override
                    public String answer(InvocationOnMock invocation) throws Throwable {
                        PrismObject<UserType> user = (PrismObject<UserType>) invocation.getArguments()[0];
                        PrismAsserts.assertEquivalent("Unexpected user as argument in repository.addObject", new File(TEST_FOLDER, "user-expected.xml"), user);
                        return "12345678-d34d-b33f-f00d-987987987987";
                    }
                });

        OperationResult result = new OperationResult("Simple Role Assignment");
        Task task = taskManager.createTaskInstance();
        try {

            //WHEN
            model.addObject(userType.asPrismObject(), task, result);

        } finally {
            LOGGER.debug(result.dump());
        }
    }

    /*
      * Test disabled Oct 18 2011. No point in fixing it. It is no longer compatible with model implementation.
      * And as model implementation will be changed soon it would be a wasted work.
      */
    @SuppressWarnings("unchecked")
    @Test(enabled = false)
    public void accountAssignment() throws Exception {
        try {

            ModelTUtil.mockGetSystemConfiguration(repository, new File(TEST_FOLDER_COMMON,
                    "system-configuration.xml"));
            final UserType user = PrismTestUtil.unmarshalObject(new File(TEST_FOLDER,
                    "user-account-assignment.xml"), UserType.class);

            final ResourceType resource = PrismTestUtil.unmarshalObject(new File(
                    TEST_FOLDER_COMMON, "resource-opendj.xml"), ResourceType.class);

            when(
                    provisioning.getObject(eq(ResourceType.class), eq(resource.getOid()),
                            any(PropertyReferenceListType.class), any(OperationResult.class))).thenReturn(
                    resource.asPrismObject());
            when(
                    provisioning.addObject(any(PrismObject.class), any(ScriptsType.class),
                            any(OperationResult.class))).thenAnswer(new Answer<String>() {
                @Override
                public String answer(InvocationOnMock invocation) throws Throwable {
                    AccountShadowType account = (AccountShadowType) invocation.getArguments()[0];
                    LOGGER.info("Created account:\n{}", account);
                    PrismAsserts.assertEquals(new File(TEST_FOLDER, "account-expected.xml"), account);
                    return "12345678-d34d-b33f-f00d-987987987989";
                }
            });
            when(repository.addObject(any(PrismObject.class), any(OperationResult.class))).thenAnswer(
                    new Answer<String>() {
                        @Override
                        public String answer(InvocationOnMock invocation) throws Throwable {
                            UserType returnedUser = (UserType) invocation.getArguments()[0];

                            final UserType userExpected = PrismTestUtil.unmarshalObject(new File(TEST_FOLDER,
                                    "user-expected.xml"), UserType.class);
                            userExpected.getAssignment().clear();
                            userExpected.getAssignment().add(user.getAssignment().get(0));

//							System.out.println("XXXXXXXXXXXX");
                            System.out.println(returnedUser);

                            PrismAsserts.assertEquals(userExpected, returnedUser);

                            return "12345678-d34d-b33f-f00d-987987987988";
                        }
                    });

            OperationResult result = new OperationResult("Account Assignment");
            Task task = taskManager.createTaskInstance();
            try {
                model.addObject(user.asPrismObject(), task, result);
            } finally {
                LOGGER.debug(result.dump());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
