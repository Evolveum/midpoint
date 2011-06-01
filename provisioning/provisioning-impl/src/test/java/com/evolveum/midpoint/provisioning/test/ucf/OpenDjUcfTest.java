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
package com.evolveum.midpoint.provisioning.test.ucf;

import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TestResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceTestResultType;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorManager;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorManagerImpl;
import com.evolveum.midpoint.provisioning.ucf.api.ConfiguredConnector;
import javax.xml.bind.JAXBElement;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.FileInputStream;
import java.io.File;
import com.evolveum.midpoint.test.repository.BaseXDatabaseFactory;
import com.evolveum.midpoint.test.ldap.OpenDJUtil;
import com.evolveum.midpoint.test.ldap.OpenDJUnitTestAdapter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test UCF implementation with OpenDJ and ICF LDAP connector.
 * 
 * This test is using embedded OpenDJ as a resource and ICF LDAP connector.
 * The test is executed by direct calls to the UCF interface.
 * 
 * @author Radovan Semancik
 */
public class OpenDjUcfTest extends OpenDJUnitTestAdapter {
	
	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/ucf/opendj-resource.xml";
	
    protected static OpenDJUtil djUtil = new OpenDJUtil();
	private JAXBContext jaxbctx;
	private ConnectorManager manager;
	private ConfiguredConnector cc;
	
	public OpenDjUcfTest() throws JAXBException {
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
    public void initUcf() throws Exception {

        File file = new File(FILENAME_RESOURCE_OPENDJ);
        FileInputStream fis = new FileInputStream(file);

        Unmarshaller u = jaxbctx.createUnmarshaller();
        Object object = u.unmarshal(fis);		
		ResourceType resource = (ResourceType) ((JAXBElement) object).getValue();

		ConnectorManagerImpl managerImpl = new ConnectorManagerImpl();
		managerImpl.initialize();
		manager = managerImpl;

		cc = manager.createConfiguredConnector(resource);

		assertNotNull(cc);

    }

    @After
    public void shutdownUcf() throws Exception {
        BaseXDatabaseFactory.XMLServerStop();
    }
	
	@Test
    public void testTestConnection() throws Exception {
        //given

        //when
		
        ResourceTestResultType result = cc.test();

        //then
        assertNotNull(result);
        TestResultType connectorConnectionResult = result.getConnectorConnection();
        assertNotNull(connectorConnectionResult);
		System.out.println("Test \"connector connection\" result: "+DebugUtil.prettyPrint(connectorConnectionResult));
        assertTrue(connectorConnectionResult.isSuccess());

    }


	
}
