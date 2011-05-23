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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model;

import static org.junit.Assert.assertEquals;

import java.io.File;

import javax.xml.bind.JAXBElement;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.model.xpath.SchemaHandling;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;

/**
 *
 * @author sleepwalker
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:application-context-model.xml", "classpath:application-context-repository.xml", "classpath:application-context-repository-test.xml", "classpath:application-context-provisioning.xml", "classpath:application-context-model-test.xml"})
public class SchemaHandlingUserDefinedVariablesTest {

    @Autowired
    private SchemaHandling schemaHandling;
    @Autowired
    private RepositoryPortType repositoryService;

    public SchemaHandlingUserDefinedVariablesTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private ObjectType addObjectToRepo(String fileString) throws Exception {
        ObjectContainerType objectContainer = new ObjectContainerType();
        ObjectType object = ((JAXBElement<ObjectType>) JAXBUtil.unmarshal(new File(fileString))).getValue();
        objectContainer.setObject(object);
        repositoryService.addObject(objectContainer);
        return object;
    }

    @Test
    public void testApplyOutboundSchemaHandlingWithUserDefinedVariablesOnAccount() throws Exception {
        final String myConfigOid = "c0c010c0-d34d-b33f-f00d-999111111111";
        try {
            addObjectToRepo("src/test/resources/generic-object-my-config.xml");

            JAXBElement<AccountShadowType> accountJaxb = (JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File("src/test/resources/account-resource-schema-handling-custom-variables.xml"));
            JAXBElement<UserType> userJaxb = (JAXBElement<UserType>) JAXBUtil.unmarshal(new File("src/test/resources/user-new.xml"));
            ResourceObjectShadowType appliedAccountShadow = schemaHandling.applyOutboundSchemaHandlingOnAccount(userJaxb.getValue(), accountJaxb.getValue(), accountJaxb.getValue().getResource());
            assertEquals(2, appliedAccountShadow.getAttributes().getAny().size());
            assertEquals("l", appliedAccountShadow.getAttributes().getAny().get(1).getLocalName());
            assertEquals("Here", appliedAccountShadow.getAttributes().getAny().get(1).getTextContent());
        } finally {
            repositoryService.deleteObject(myConfigOid);
        }
    }
}
