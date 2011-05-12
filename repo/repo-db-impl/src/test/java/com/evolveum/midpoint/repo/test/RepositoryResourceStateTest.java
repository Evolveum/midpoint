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

package com.evolveum.midpoint.repo.test;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.diff.CalculateXmlDiff;
import com.evolveum.midpoint.util.jaxb.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
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

import static org.junit.Assert.*;

/**
 *
 * @author sleepwalker
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"../../../../../application-context-repository.xml", "../../../../../application-context-repository-test.xml"})
public class RepositoryResourceStateTest {

    @Autowired(required = true)
    private RepositoryPortType repositoryService;

    public RepositoryPortType getRepositoryService() {
        return repositoryService;
    }

    public void setRepositoryService(RepositoryPortType repositoryService) {
        this.repositoryService = repositoryService;
    }

    public RepositoryResourceStateTest() {
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

    @Test
    @SuppressWarnings("unchecked")
    public void testResourceState() throws Exception {
        final String resourceStateOid = "97fa8fd2-0462-42c9-9ca0-e2d317c3d93f";
        final String resourceOid = "aae7be60-df56-11df-8608-0002a5d5c51b";
        try {
            //add resource referenced by resourcestate
            ObjectContainerType objectContainer = new ObjectContainerType();
            ResourceType resource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File("src/test/resources/aae7be60-df56-11df-8608-0002a5d5c51b.xml"))).getValue();
            objectContainer.setObject(resource);
            repositoryService.addObject(objectContainer);
            ObjectContainerType retrievedObjectContainer = repositoryService.getObject(resourceOid, new PropertyReferenceListType());
            assertEquals(resource.getOid(), ((ResourceType) (retrievedObjectContainer.getObject())).getOid());

            objectContainer = new ObjectContainerType();
            ResourceStateType resourceState = ((JAXBElement<ResourceStateType>) JAXBUtil.unmarshal(new File("src/test/resources/resource-state.xml"))).getValue();
            objectContainer.setObject(resourceState);
            repositoryService.addObject(objectContainer);
            retrievedObjectContainer = repositoryService.getObject(resourceStateOid, new PropertyReferenceListType());
            assertEquals(resourceState.getOid(), ((ResourceStateType) (retrievedObjectContainer.getObject())).getOid());
            ObjectListType objects = repositoryService.listObjects(QNameUtil.qNameToUri(SchemaConstants.I_RESOURCE_STATE_TYPE), new PagingType());
            assertEquals(1, objects.getObject().size());
            assertEquals(resourceStateOid, objects.getObject().get(0).getOid());
        } finally {
            repositoryService.deleteObject(resourceOid);
            repositoryService.deleteObject(resourceStateOid);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testResourceStateModification() throws Exception {
        final String resourceStateOid = "97fa8fd2-0462-42c9-9ca0-e2d317c3d93f";
        final String resourceOid = "aae7be60-df56-11df-8608-0002a5d5c51b";
        try {
            //add resource referenced by resourcestate
            ObjectContainerType objectContainer = new ObjectContainerType();
            ResourceType resource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File("src/test/resources/aae7be60-df56-11df-8608-0002a5d5c51b.xml"))).getValue();
            objectContainer.setObject(resource);
            repositoryService.addObject(objectContainer);
            ObjectContainerType retrievedObjectContainer = repositoryService.getObject(resourceOid, new PropertyReferenceListType());
            assertEquals(resource.getOid(), ((ResourceType) (retrievedObjectContainer.getObject())).getOid());

            objectContainer = new ObjectContainerType();
            ResourceStateType resourceState = ((JAXBElement<ResourceStateType>) JAXBUtil.unmarshal(new File("src/test/resources/resource-state.xml"))).getValue();
            objectContainer.setObject(resourceState);
            repositoryService.addObject(objectContainer);
            retrievedObjectContainer = repositoryService.getObject(resourceStateOid, new PropertyReferenceListType());
            assertEquals(resourceState.getOid(), ((ResourceStateType) (retrievedObjectContainer.getObject())).getOid());

            ResourceStateType resourceStateAfterSync = ((JAXBElement<ResourceStateType>) JAXBUtil.unmarshal(new File("src/test/resources/resource-state-after-sync.xml"))).getValue();
            ObjectModificationType objectModificationType = CalculateXmlDiff.calculateChanges(new File("src/test/resources/resource-state.xml"), new File("src/test/resources/resource-state-after-sync.xml"));
            repositoryService.modifyObject(objectModificationType);
            retrievedObjectContainer = repositoryService.getObject(resourceStateOid, new PropertyReferenceListType());
            assertEquals(resourceStateAfterSync.getOid(), ((ResourceStateType) (retrievedObjectContainer.getObject())).getOid());
            assertEquals(resourceStateAfterSync.getSynchronizationState().getAny().get(0).getTextContent(), ((ResourceStateType) (retrievedObjectContainer.getObject())).getSynchronizationState().getAny().get(0).getTextContent());

        } finally {
            repositoryService.deleteObject(resourceOid);
            repositoryService.deleteObject(resourceStateOid);
        }
    }
}
