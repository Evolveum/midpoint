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

import static org.junit.Assert.fail;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.model.xpath.SchemaHandling;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.ProvisioningPortType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;

/**
 *
 * @author lazyman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
    "classpath:application-context-model.xml",    
    "classpath:application-context-model-unit-test.xml"})
public class ModelSearchObjectsTest {

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
    public void nullQueryType() throws FaultMessage {
        modelService.searchObjects(null, new PagingType());
        fail("Illegal argument exception was not thrown.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullQueryTypeAndPaging() throws FaultMessage {
        modelService.searchObjects(null, null);
        fail("Illegal argument exception was not thrown.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullPaging() throws FaultMessage {
        modelService.searchObjects(new QueryType(), null);
        fail("Illegal argument exception was not thrown.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void badPaging() throws FaultMessage {
        PagingType paging = new PagingType();
        paging.setMaxSize(BigInteger.valueOf(-1));
        paging.setOffset(BigInteger.valueOf(-1));

        modelService.searchObjects(new QueryType(), paging);
        fail("Illegal argument exception was not thrown.");
    }

    @Ignore
    @Test
    public void correctSearch() {
        fail("not implemented yet.");
    }
}
