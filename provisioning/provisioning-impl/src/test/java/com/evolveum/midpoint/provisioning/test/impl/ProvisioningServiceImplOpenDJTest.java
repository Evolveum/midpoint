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

import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import java.io.FileNotFoundException;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import java.io.FileInputStream;
import java.io.File;
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
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Radovan Semancik
 */
public class ProvisioningServiceImplOpenDJTest extends OpenDJUnitTestAdapter {
	
	// Let's reuse the resource definition from UCF tests ... for now
	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/ucf/opendj-resource.xml";
	private static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-48e2-86d6-3d4f02d3eeee";
	private static final String FILENAME_ACCOUNT1 = "src/test/resources/impl/account1.xml";
	private static final String ACCOUNT1_OID = "dbb0c37d-9ee6-44a4-8d39-016dbce1cccc";
	
    protected static OpenDJUtil djUtil = new OpenDJUtil();
	private JAXBContext jaxbctx;
	private ResourceType resource;
	private ConnectorManager manager;
	private ShadowCache shadowCache;
	private RepositoryPortType repositoryPort;
	private ProvisioningService provisioningService;
	private Unmarshaller unmarshaller;
	
	public ProvisioningServiceImplOpenDJTest() throws JAXBException {
		jaxbctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		unmarshaller = jaxbctx.createUnmarshaller();
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
		
		// The default repository content is using old format of resource configuration
		// We need a sample data in the new format, so we need to set it up
		// manually.

		resource = (ResourceType) addObjectFromFile(FILENAME_RESOURCE_OPENDJ);
		addObjectFromFile(FILENAME_ACCOUNT1);
		
		shadowCache = new ShadowCache();
		shadowCache.setConnectorManager(manager);
		shadowCache.setRepositoryService(repositoryPort);
		
		ProvisioningServiceImpl provisioningServiceImpl = new ProvisioningServiceImpl();
		provisioningServiceImpl.setShadowCache(shadowCache);
		provisioningServiceImpl.setRepositoryService(repositoryPort);
		provisioningService = provisioningServiceImpl;
		
		assertNotNull(provisioningService);
    }

	private ObjectType addObjectFromFile(String filePath) throws FileNotFoundException, JAXBException, com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage {
		File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
		Object object = unmarshaller.unmarshal(fis);		
		ObjectType objectType = (ObjectType) ((JAXBElement) object).getValue();
		ObjectContainerType container = new ObjectContainerType();
		container.setObject(objectType);
		repositoryPort.addObject(container);
		return objectType;
	}
	
    @After
    public void shutdownUcf() throws Exception {
        BaseXDatabaseFactory.XMLServerStop();
    }

    @Ignore
	@Test
	public void getObjectTest() throws Exception {
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()+".getObjectTest");
		PropertyReferenceListType resolve = new PropertyReferenceListType();
		
		ObjectType object = provisioningService.getObject(ACCOUNT1_OID, resolve, result);
		
		assertNotNull(object);
		
		System.out.println(DebugUtil.prettyPrint(object));
	}
}
