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

package com.evolveum.midpoint.provisioning.integration.identityconnector;

import com.evolveum.midpoint.api.exceptions.MidPointException;
import com.evolveum.midpoint.test.util.SampleObjects;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.idconnector.configuration_1.ConnectorConfiguration;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author laszlohordos
 */
public class ConnectorUtilTest {

    private JAXBContext context = null;
    private com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory commonObjectFactory = new com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory();
    private com.evolveum.midpoint.xml.ns._public.resource.idconnector.configuration_1.ObjectFactory icfObjectFactory = new com.evolveum.midpoint.xml.ns._public.resource.idconnector.configuration_1.ObjectFactory();
    private Marshaller marshaller;
    private ConnectorConfiguration connectorConfiguration;
    private ResourceType resource;

    public ConnectorUtilTest() throws JAXBException {
        context = JAXBContext.newInstance("com.evolveum.midpoint.xml.ns._public.common.common_1:com.evolveum.midpoint.xml.ns._public.resource.idconnector.configuration_1");
        marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    }

    

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
   

    /**
     * Test of getBundleURLs method, of class ConnectorUtil.
     */
    @Test
    public void testGetBundleURLs() {
        System.out.println("getBundleURLs");

        URL[] result = ConnectorUtil.getBundleURLs();
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    /**
     * Test of clearManagerCaches method, of class ConnectorUtil.
     */
    @Test
    @Ignore
    public void testClearManagerCaches() {
        System.out.println("clearManagerCaches");
        ConnectorUtil.clearManagerCaches();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    @Test
    public void testCreateConnectorFacade() throws JAXBException {
        System.out.println("createConnectorFacade");
        ResourceType resource = (ResourceType) TestUtil.getSampleObject(SampleObjects.RESOURCETYPE_LOCALHOST_OPENDJ);
        IdentityConnector c = new IdentityConnector(resource);
        assertNotNull(c.getConfiguration());
        ConnectorFacade result = null;
        try {
            result = ConnectorUtil.createConnectorFacade(c);
        } catch (MidPointException ex) {
            Logger.getLogger(ConnectorUtilTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertNotNull(result);        
    }
}
