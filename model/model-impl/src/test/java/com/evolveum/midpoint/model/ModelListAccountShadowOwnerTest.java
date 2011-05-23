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
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.ProvisioningPortType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import java.io.File;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
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
public class ModelListAccountShadowOwnerTest {

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
    public void nullAccountOid() throws FaultMessage {
        modelService.listAccountShadowOwner(null);
        fail("Illegal argument excetion must be thrown");
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyAccountOid() throws FaultMessage {
        modelService.listAccountShadowOwner("");
        fail("Illegal argument excetion must be thrown");
    }

    @Test
    public void accountWithoutOwner() throws FaultMessage,
            com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage {
        final String accountOid = "1";
        final UserContainerType expected = new UserContainerType();
        when(repositoryService.listAccountShadowOwner(accountOid)).thenReturn(expected);
        final UserContainerType returned = modelService.listAccountShadowOwner("1");
        assertNotNull(returned);
        assertNull(returned.getUser());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void correctListAccountShadowOwner() throws FaultMessage, JAXBException,
            com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage {
        final String accountOid = "acc11111-76e0-48e2-86d6-3d4f02d3e1a2";
        final UserContainerType expected = new UserContainerType();
        expected.setUser(((JAXBElement<UserType>) JAXBUtil.unmarshal(
                new File(TEST_FOLDER, "list-account-shadow-owner.xml"))).getValue());

        when(repositoryService.listAccountShadowOwner(accountOid)).thenReturn(expected);
        final UserContainerType returned = modelService.listAccountShadowOwner(accountOid);
        assertNotNull(returned);
        assertNotNull(returned.getUser());
        assertEquals(expected.getUser(), returned.getUser());
    }
}
