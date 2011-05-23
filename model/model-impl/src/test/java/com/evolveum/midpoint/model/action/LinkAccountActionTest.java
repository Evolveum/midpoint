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

package com.evolveum.midpoint.model.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

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
import com.evolveum.midpoint.provisioning.objects.ResourceObject;
import com.evolveum.midpoint.provisioning.schema.ResourceSchema;
import com.evolveum.midpoint.provisioning.schema.util.ObjectValueWriter;
import com.evolveum.midpoint.provisioning.service.BaseResourceIntegration;
import com.evolveum.midpoint.provisioning.service.ResourceAccessInterface;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationalResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.ProvisioningPortType;
import com.evolveum.midpoint.xml.ns._public.provisioning.resource_object_change_listener_1.ResourceObjectChangeListenerPortType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;

/**
 *
 * @author Katuska
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:application-context-model.xml", "classpath:application-context-repository.xml", "classpath:application-context-repository-test.xml", "classpath:application-context-provisioning.xml", "classpath:application-context-model-test.xml"})
public class LinkAccountActionTest {

    @Autowired(required = true)
    private ResourceObjectChangeListenerPortType resourceObjectChangeService;
    @Autowired(required = true)
    private RepositoryPortType repositoryService;
    @Autowired(required = true)
    private ResourceAccessInterface rai;
//    @Autowired(required = true)
//    private ProvisioningPortType provisioningService;

    public LinkAccountActionTest() {
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

    private ObjectType addObjectToRepo(ObjectType object) throws Exception {
        ObjectContainerType objectContainer = new ObjectContainerType();
        objectContainer.setObject(object);
        repositoryService.addObject(objectContainer);
        return object;
    }

    @SuppressWarnings("unchecked")
    private ObjectType addObjectToRepo(String fileString) throws Exception {
        ObjectContainerType objectContainer = new ObjectContainerType();
        ObjectType object = ((JAXBElement<ObjectType>) JAXBUtil.unmarshal(new File(fileString))).getValue();
        objectContainer.setObject(object);
        repositoryService.addObject(objectContainer);
        return object;
    }

    @SuppressWarnings("unchecked")
    private ResourceObjectShadowChangeDescriptionType createChangeDescription(String file) throws JAXBException {
        ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil.unmarshal(new File(file))).getValue();
        return change;
    }

    private ResourceObject createSampleResourceObject(ResourceSchema schema, ResourceObjectShadowType shadow ) throws ParserConfigurationException {
        ObjectValueWriter valueWriter = ObjectValueWriter.getInstance();
        return valueWriter.buildResourceObject(shadow, schema);
    }
    
    @Test
    public void testAddUserAction() throws Exception {

        final String resourceOid = "ef2bc95b-76e0-48e2-97e7-3d4f02d3e1a2";
        final String userOid = "12345678-d34d-b33f-f00d-987987987987";
        final String accountOid = "c0c010c0-d34d-b33f-f00d-222333444555";

        try {
            //create additional change
            ResourceObjectShadowChangeDescriptionType change = createChangeDescription("src/test/resources/account-change-add.xml");
            //adding objects to repo
            addObjectToRepo("src/test/resources/user.xml");
            ResourceType resourceType = (ResourceType) addObjectToRepo(change.getResource());
            AccountShadowType accountType = (AccountShadowType) addObjectToRepo(change.getShadow());

            assertNotNull(resourceType);
            //setup provisioning mock
            BaseResourceIntegration bri = new BaseResourceIntegration(resourceType);
            ResourceObject ro = createSampleResourceObject(bri.getSchema(), accountType);
            when(rai.get(
                    any(OperationalResultType.class),
                    any(ResourceObject.class))).thenReturn(ro);
            when(rai.getConnector()).thenReturn(bri);
            
            resourceObjectChangeService.notifyChange(change);

            ObjectContainerType container = repositoryService.getObject(userOid, new PropertyReferenceListType());
            UserType changedUser = (UserType) container.getObject();
            List<ObjectReferenceType> accountRefs = changedUser.getAccountRef();

            assertNotNull(changedUser);
            assertEquals(accountOid, accountRefs.get(0).getOid());

            container = repositoryService.getObject(accountOid, new PropertyReferenceListType());
            AccountShadowType linkedAccount = (AccountShadowType) container.getObject();

            assertNotNull(linkedAccount);
            assertEquals(changedUser.getName(), linkedAccount.getName());

        } finally {
            //cleanup repo
            try {
                repositoryService.deleteObject(accountOid);
            } catch(com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage e) {
            }
            try {
                repositoryService.deleteObject(resourceOid);
            } catch(com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage e) {
            }
            try {
                repositoryService.deleteObject(userOid);
            } catch(com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage e) {
            }
        }

    }
}
