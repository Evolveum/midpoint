/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.evolveum.midpoint.provisioning.test.ucf;

import com.evolveum.midpoint.provisioning.ucf.api.ConnectorManager;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorManagerImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author semancik
 */
public class SimpleUcfTest {

	ConnectorManager manager;

	public SimpleUcfTest() {
	}

	@Before
	public void setUp() {
		ConnectorManagerImpl managerImpl = new ConnectorManagerImpl();
		managerImpl.initialize();
		manager = managerImpl;
	}

	@After
	public void tearDown() {
	}

	@Test
	public void testListConnectors() {
		Set<ConnectorType> listConnectors = manager.listConnectors();
		
		assertNotNull(listConnectors);
		assertFalse(listConnectors.isEmpty());
		
		for (ConnectorType connector : listConnectors) {
			assertNotNull(connector.getOid());
			assertNotNull(connector.getName());
			System.out.println("CONNECTOR OID="+connector.getOid()+", name="+connector.getName()+", version="+connector.getVersion());
		}
	}
}
