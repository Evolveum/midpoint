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

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.model.test.util.UserTypeComparator;
import com.evolveum.midpoint.model.xpath.SchemaHandling;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.ProvisioningPortType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 *
 * @author lazyman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
    "classpath:application-context-model.xml",
    "classpath:application-context-model-unit-test.xml"})
public class ModelListObjectsTest {

    private static final File TEST_FOLDER = new File("./src/test/resources/service/model/list");
    @Autowired(required = true)
    ModelPortType modelService;
    @Autowired(required = true)
    ProvisioningPortType provisioningService;
    @Autowired(required = true)
    RepositoryPortType repositoryService;
//    @Autowired(required = true)
//    SchemaHandling schemaHandling;

    @Before
    public void before() {
        Mockito.reset(provisioningService, repositoryService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullObjectType() throws FaultMessage {
        modelService.listObjects(null, new PagingType());
        fail("Illegal argument exception was not thrown.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullObjectTypeAndPaging() throws FaultMessage {
        modelService.listObjects(null, null);
        fail("Illegal argument exception was not thrown.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullPaging() throws FaultMessage {
        modelService.listObjects("", null);
        fail("Illegal argument exception was not thrown.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void badPaging() throws FaultMessage {
        PagingType paging = new PagingType();
        paging.setMaxSize(BigInteger.valueOf(-1));
        paging.setOffset(BigInteger.valueOf(-1));

        modelService.listObjects(Utils.getObjectType("UserType"), paging);
        fail("Illegal argument exception was not thrown.");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void userList() throws FaultMessage, JAXBException, com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage {
        final ObjectListType expectedUserList = ((JAXBElement<ObjectListType>) JAXBUtil.unmarshal(
                new File(TEST_FOLDER, "user-list.xml"))).getValue();

        when(repositoryService.listObjects(eq(Utils.getObjectType("UserType")), any(PagingType.class))).thenReturn(expectedUserList);
        final ObjectListType returnedUserList = modelService.listObjects(Utils.getObjectType("UserType"), new PagingType());

        verify(repositoryService, times(1)).listObjects(eq(Utils.getObjectType("UserType")), any(PagingType.class));
        testObjectListTypes(expectedUserList, returnedUserList);
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	private void testObjectListTypes(ObjectListType expected, ObjectListType returned) {
        assertNotNull(expected);
        assertNotNull(returned);

        List<ObjectType> expectedList = expected.getObject();
        List<ObjectType> returnedList = returned.getObject();

        assertTrue(expectedList == null ? returnedList == null : returnedList != null);
        if (expectedList == null) {
            return;
        }
        assertEquals(expectedList.size(), returnedList.size());
        if (expectedList.size() == 0) {
            return;
        }

        if (expectedList.get(0) instanceof UserType) {
            testUserLists(new ArrayList(expectedList), new ArrayList(returnedList));
        }
    }

    private void testUserLists(List<UserType> expected, List<UserType> returned) {
        UserTypeComparator comp = new UserTypeComparator();
        for (int i = 0; i < expected.size(); i++) {
            UserType u1 = expected.get(i);
            UserType u2 = returned.get(i);

            assertTrue(comp.areEqual(u1, u2));
        }
    }
}
