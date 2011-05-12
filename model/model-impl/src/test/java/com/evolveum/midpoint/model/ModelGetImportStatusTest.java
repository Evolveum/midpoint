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

import com.evolveum.midpoint.model.xpath.SchemaHandling;
import com.evolveum.midpoint.util.jaxb.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemFaultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskStatusType;
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
public class ModelGetImportStatusTest {

    private static final File TEST_FOLDER = new File("./src/test/resources/service/model/import");
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
    public void nullResourceOid() throws FaultMessage {
        modelService.getImportStatus(null);
        fail("Illegal argument exception was not thrown.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyResourceOid() throws FaultMessage {
        modelService.getImportStatus("");
        fail("Illegal argument exception was not thrown.");
    }

    @Test(expected = FaultMessage.class)
    public void nonExistingResourceOid() throws FaultMessage, 
            com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.FaultMessage {
        
        final String nonExistingUid = "1";
        when(provisioningService.getImportStatus(nonExistingUid)).thenThrow(
                new com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.FaultMessage(
                "Resource with uid '" + nonExistingUid + "' doesn't exist.", new SystemFaultType()));

        modelService.getImportStatus(nonExistingUid);
        fail("Fault exception was not thrown.");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void correctResourceOid() throws FaultMessage, JAXBException,
            com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.FaultMessage {
        final String resourceOid = "abababab-abab-abab-abab-000000000001";
        final TaskStatusType expectedTaskStatus = ((JAXBElement<TaskStatusType>) JAXBUtil.unmarshal(
                new File(TEST_FOLDER, "import-status-correct.xml"))).getValue();
        when(provisioningService.getImportStatus(resourceOid)).thenReturn(expectedTaskStatus);

        final TaskStatusType taskStatus = modelService.getImportStatus(resourceOid);
        assertNotNull(taskStatus);
        assertEquals(expectedTaskStatus.getName(), taskStatus.getName());
        assertEquals(expectedTaskStatus.getFinishTime(), taskStatus.getFinishTime());
        assertEquals(expectedTaskStatus.getLastStatus(), taskStatus.getLastStatus());
        assertEquals(expectedTaskStatus.getLaunchTime(), taskStatus.getLaunchTime());
        assertEquals(expectedTaskStatus.getNumberOfErrors(), taskStatus.getNumberOfErrors());
        assertEquals(expectedTaskStatus.getProgress(), taskStatus.getProgress());

        verify(provisioningService, atLeastOnce()).getImportStatus(resourceOid);
        
    }
}
