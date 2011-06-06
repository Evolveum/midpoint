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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.test.impl;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.ProvisioningServiceImpl;
import com.evolveum.midpoint.test.repository.BaseXDatabaseFactory;
import com.evolveum.midpoint.provisioning.impl.ShadowCache;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorManagerIcfImpl;
import com.evolveum.midpoint.test.ldap.OpenDJUnitTestAdapter;
import javax.xml.bind.JAXBException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import javax.xml.bind.JAXBContext;
import com.evolveum.midpoint.test.ldap.OpenDJUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Radovan Semancik
 */
public class ProvisioningServiceImplOpenDJTest extends OpenDJUnitTestAdapter {
	
	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/ucf/opendj-resource.xml";
	private static final String RESOURCE_OPENDJ_OID = "";
	
    protected static OpenDJUtil djUtil = new OpenDJUtil();
	private JAXBContext jaxbctx;
	private ResourceType resource;
	private ConnectorManager manager;
	private ShadowCache shadowCache;
	private RepositoryPortType repositoryPort;
	private ProvisioningService provisioningService;
	
	public ProvisioningServiceImplOpenDJTest() throws JAXBException {
		jaxbctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@BeforeClass
    public static void startLdap() throws Exception{
        startACleanDJ();
    }
    
    @AfterClass
    public static void stopLdap() throws Exception{
        stopDJ();
    }

    @Before
    public void initProvisioning() throws Exception {

		ConnectorManagerIcfImpl managerImpl = new ConnectorManagerIcfImpl();
		managerImpl.initialize();
		manager = managerImpl;
		assertNotNull(manager);
		
		repositoryPort = BaseXDatabaseFactory.getRepositoryPort();
		
		// TODO: setup repository content

		shadowCache = new ShadowCache();
		shadowCache.setConnectorManager(manager);
		shadowCache.setRepositoryService(repositoryPort);
		
		ProvisioningServiceImpl provisioningServiceImpl = new ProvisioningServiceImpl();
		provisioningServiceImpl.setShadowCache(shadowCache);
		provisioningServiceImpl.setRepositoryService(repositoryPort);
		provisioningService = provisioningServiceImpl;
		
//		File file = new File(FILENAME_RESOURCE_OPENDJ);
//        FileInputStream fis = new FileInputStream(file);
//
//        Unmarshaller u = jaxbctx.createUnmarshaller();
//        Object object = u.unmarshal(fis);		
//		resource = (ResourceType) ((JAXBElement) object).getValue();
				

    }

    @After
    public void shutdownUcf() throws Exception {
        BaseXDatabaseFactory.XMLServerStop();
    }

	@Test
	public void trivialTest() {
		assertNotNull(provisioningService);
	}
}
