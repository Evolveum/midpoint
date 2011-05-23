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

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.model.xpath.SchemaHandling;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectNotFoundFaultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.ProvisioningPortType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import java.io.File;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.ws.Holder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author lazyman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
    "classpath:application-context-model.xml",
    "classpath:application-context-model-unit-test.xml"})
public class ModelDeleteObjectTest {

    private static final File TEST_FOLDER = new File("./src/test/resources/service/model/delete");
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
    public void testDeleteNull() throws FaultMessage {
        modelService.deleteObject(null);
        fail("delete must fail");
    }

    @Test(expected = FaultMessage.class)
    public void testDeleteNonExisting() throws FaultMessage,
            com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage {
        final String oid = "abababab-abab-abab-abab-000000000001";
        when(repositoryService.getObject(eq(oid), any(PropertyReferenceListType.class))).thenThrow(
                new com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage(
                "Object with oid '' not found.", new ObjectNotFoundFaultType()));

        modelService.deleteObject(oid);
        fail("delete must fail");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeleteCorrectRepo() throws FaultMessage, JAXBException,
            com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage {
        ObjectContainerType container = new ObjectContainerType();
        final UserType expectedUser = ((JAXBElement<UserType>) JAXBUtil.unmarshal(
                new File(TEST_FOLDER, "delete-user.xml"))).getValue();
        container.setObject(expectedUser);

        final String oid = "abababab-abab-abab-abab-000000000001";
        when(repositoryService.getObject(eq(oid), any(PropertyReferenceListType.class))).thenReturn(container);
        modelService.deleteObject(oid);

        verify(repositoryService, atLeastOnce()).getObject(eq(oid), any(PropertyReferenceListType.class));
        verify(repositoryService, times(1)).deleteObject(oid);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeleteCorrectProvisioning() throws FaultMessage, JAXBException,
            com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage,
            com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.FaultMessage {
        ObjectContainerType container = new ObjectContainerType();
        final ResourceType expectedUser = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(
                new File(TEST_FOLDER, "delete-resource.xml"))).getValue();
        container.setObject(expectedUser);

        final String oid = "abababab-abab-abab-abab-000000000001";
        when(repositoryService.getObject(eq(oid), any(PropertyReferenceListType.class))).thenReturn(container);
        modelService.deleteObject(oid);

        verify(repositoryService, atLeastOnce()).getObject(eq(oid), any(PropertyReferenceListType.class));
        verify(provisioningService, times(1)).deleteObject(eq(oid), any(ScriptsType.class), any(Holder.class));
    }
}
