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

package com.evolveum.midpoint.provisioning.service;

import com.evolveum.midpoint.xml.ns._public.provisioning.resource_object_change_listener_1.ResourceObjectChangeListenerPortType;
import org.junit.Ignore;
import org.junit.AfterClass;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.types.SearchScope;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import com.evolveum.midpoint.test.ldap.OpenDJUnitTestAdapter;
import com.evolveum.midpoint.test.ldap.OpenDJUtil;
import javax.xml.bind.JAXBException;
import org.junit.Test;
import java.io.InputStream;
import javax.xml.bind.JAXBContext;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.midpoint.test.repository.BaseXDatabaseFactory;
import com.evolveum.midpoint.test.util.SampleObjects;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import javax.xml.bind.JAXBElement;
import static org.mockito.Mockito.*;
import javax.xml.ws.Holder;
import static org.junit.Assert.*;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage;

/**
 * Integration like unit tests with embedded LDAP.
 * 
 * @author elek
 */
public class ProvisioningServiceIntegrationTest extends OpenDJUnitTestAdapter {

    private static JAXBContext ctx;
    private RepositoryPortType repositoryPort;
    protected static OpenDJUtil djUtil = new OpenDJUtil();

    public ProvisioningServiceIntegrationTest() throws JAXBException {
        ctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
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
    public void initRepository() throws Exception {
        //Create Repository mock
        repositoryPort = BaseXDatabaseFactory.getRepositoryPort();
        //Make suere the Repository is mocked
        assertNotNull(repositoryPort);

    }

    @After
    public void resetRepository() throws Exception {
        BaseXDatabaseFactory.XMLServerStop();
    }


    

    @Test
    public void testModifyObjectLDAP() throws Exception {
        //GIVEN
        OperationalResultType opResult = new OperationalResultType();
        ProvisioningService service = new ProvisioningService();

        //Create Sample Request
        InputStream in = getClass().getResourceAsStream("/modifyShadow.xml");
        JAXBElement<ObjectModificationType> o = (JAXBElement<ObjectModificationType>) ctx.createUnmarshaller().unmarshal(in);
        ObjectModificationType oct = o.getValue();

        service.setRepositoryPort(repositoryPort);

        assertTrue(new OpenDJUtil().ldapCompare("givenName: James", "uid=jbond,ou=People,dc=example,dc=com"));

        // TODO: Handle scripts
        ScriptsType scripts = new ScriptsType();

        //WHEN        
        service.modifyObject(oct, scripts, new Holder<OperationalResultType>(opResult));

        //THEN
        assertTrue(djUtil.existsDN("uid=jbond,ou=People,dc=example,dc=com"));
        assertTrue(new OpenDJUtil().ldapCompare("givenName: newGivenName", "uid=jbond,ou=People,dc=example,dc=com"));
    }

    @Test
    @Ignore
    public void testModifyObjectWithScriptLDAP() throws Exception {
        //GIVEN
        OperationalResultType opResult = new OperationalResultType();
        ProvisioningService service = new ProvisioningService();

        //Create Sample Request
        InputStream in = getClass().getResourceAsStream("/modifyShadow.xml");
        JAXBElement<ObjectModificationType> o = (JAXBElement<ObjectModificationType>) ctx.createUnmarshaller().unmarshal(in);
        ObjectModificationType oct = o.getValue();

        service.setRepositoryPort(repositoryPort);

        assertTrue(new OpenDJUtil().ldapCompare("givenName: James", "uid=jbond,ou=People,dc=example,dc=com"));

        // TODO: Handle scripts
        InputStream sin = getClass().getResourceAsStream("/script.xml");
        JAXBElement<ScriptsType> so = (JAXBElement<ScriptsType>) ctx.createUnmarshaller().unmarshal(sin);
        ScriptsType scripts = so.getValue();

        //WHEN
        service.modifyObject(oct, scripts, new Holder<OperationalResultType>(opResult));

        //THEN
        assertTrue(djUtil.existsDN("uid=jbond,ou=People,dc=example,dc=com"));
        assertTrue(new OpenDJUtil().ldapCompare("givenName: newGivenName", "uid=jbond,ou=People,dc=example,dc=com"));
    }

    @Test
    public void testModifyObjectPwdLDAP() throws Exception {
        //GIVEN
        OperationalResultType opResult = new OperationalResultType();
        ProvisioningService service = new ProvisioningService();

        //Create Sample Request
        InputStream in = getClass().getResourceAsStream("/modifyShadowPwd.xml");
        JAXBElement<ObjectModificationType> o = (JAXBElement<ObjectModificationType>) ctx.createUnmarshaller().unmarshal(in);
        ObjectModificationType oct = o.getValue();

        service.setRepositoryPort(repositoryPort);

        assertTrue(new OpenDJUtil().ldapCompare("givenName: James", "uid=jbond,ou=People,dc=example,dc=com"));

        // TODO: Handle scripts
        ScriptsType scripts = new ScriptsType();

        //WHEN
        service.modifyObject(oct, scripts, new Holder<OperationalResultType>(opResult));

        //THEN
        assertTrue(djUtil.existsDN("uid=jbond,ou=People,dc=example,dc=com"));
        //assertTrue(new OpenDJUtil().ldapCompare("givenName: newGivenName", "uid=jbond,ou=People,dc=example,dc=com"));
    }

    @Test
    public void testModifyObjectPwdPathLDAP() throws Exception {
        //GIVEN
        OperationalResultType opResult = new OperationalResultType();
        ProvisioningService service = new ProvisioningService();

        //Create Sample Request
        InputStream in = getClass().getResourceAsStream("/modifyShadowPwdWithPath.xml");
        JAXBElement<ObjectModificationType> o = (JAXBElement<ObjectModificationType>) ctx.createUnmarshaller().unmarshal(in);
        ObjectModificationType oct = o.getValue();

        service.setRepositoryPort(repositoryPort);

        assertTrue(new OpenDJUtil().ldapCompare("givenName: James", "uid=jbond,ou=People,dc=example,dc=com"));

        // TODO: Handle scripts
        ScriptsType scripts = new ScriptsType();

        //WHEN
        service.modifyObject(oct, scripts, new Holder<OperationalResultType>(opResult));

        //THEN
        assertTrue(djUtil.existsDN("uid=jbond,ou=People,dc=example,dc=com"));
        //assertTrue(new OpenDJUtil().ldapCompare("givenName: newGivenName", "uid=jbond,ou=People,dc=example,dc=com"));
    }

    @Test
    public void testAddObjectLDAP() throws Exception {
        //given
        OperationalResultType opResult = new OperationalResultType();
        ProvisioningService service = new ProvisioningService();

        //Create Sample Request
        InputStream in = getClass().getResourceAsStream("/addShadow.xml");
        JAXBElement<ResourceObjectShadowType> o = (JAXBElement<ResourceObjectShadowType>) ctx.createUnmarshaller().unmarshal(in);
        ObjectContainerType container = new ObjectContainerType();
        container.setObject(o.getValue());

        ObjectContainerType resourceContainer = repositoryPort.getObject(o.getValue().getResourceRef().getOid(), new PropertyReferenceListType());
        ResourceType resource = (ResourceType) resourceContainer.getObject();
        assertNotNull(resource);
        service.setRepositoryPort(repositoryPort);

        assertFalse(djUtil.existsDN("uid=ahoi,ou=People,dc=example,dc=com"));

        // TODO: Handle scripts
        ScriptsType scripts = new ScriptsType();

        //when
        String result = service.addObject(container, scripts, new Holder<OperationalResultType>(opResult));
//
        assertNotNull(result);
        ObjectContainerType accountContainer = repositoryPort.getObject(result, new PropertyReferenceListType());
        assertNotNull(accountContainer.getObject());

        assertTrue(djUtil.existsDN("uid=ahoi,ou=People,dc=example,dc=com"));
        //TODO more assetions
    }

    @Test
    public void testAddObjectLDAPGroup() throws Exception {
        //given
        OperationalResultType opResult = new OperationalResultType();
        ProvisioningService service = new ProvisioningService();

        //Create Sample Request
        InputStream in = getClass().getResourceAsStream("/addShadowRO.xml");
        JAXBElement<ResourceObjectShadowType> o = (JAXBElement<ResourceObjectShadowType>) ctx.createUnmarshaller().unmarshal(in);
        ObjectContainerType container = new ObjectContainerType();
        container.setObject(o.getValue());

        ObjectContainerType resourceContainer = repositoryPort.getObject(o.getValue().getResourceRef().getOid(), new PropertyReferenceListType());
        ResourceType resource = (ResourceType) resourceContainer.getObject();
        assertNotNull(resource);
        service.setRepositoryPort(repositoryPort);

        assertFalse(djUtil.existsDN("cn=Test,ou=People,dc=example,dc=com"));

        // TODO: Handle scripts
        ScriptsType scripts = new ScriptsType();

        //when
        String result = service.addObject(container, scripts, new Holder<OperationalResultType>(opResult));
//
        assertNotNull(result);
        ObjectContainerType accountContainer = repositoryPort.getObject(result, new PropertyReferenceListType());
        assertNotNull(accountContainer.getObject());

        assertTrue(djUtil.existsDN("cn=Test,ou=People,dc=example,dc=com"));
        //TODO more assetions
    }

    @Test
    public void testDeleteObjectLDAP() throws Exception {

        //GIVEN
        OperationalResultType opResult = new OperationalResultType();

        ProvisioningService service = new ProvisioningService();
        service.setRepositoryPort(repositoryPort);

        ObjectContainerType accountBefore = repositoryPort.getObject(SampleObjects.ACCOUNTSHADOWTYPE_OPENDJ_JBOND.getOID(), new PropertyReferenceListType());
        assertNotNull(accountBefore.getObject());

        // TODO: Handle scripts
        ScriptsType scripts = new ScriptsType();

        //still exists
        assertTrue(djUtil.existsDN("uid=jbond,ou=People,dc=example,dc=com"));

        //WHEN
        service.deleteObject(SampleObjects.ACCOUNTSHADOWTYPE_OPENDJ_JBOND.getOID(), scripts, new Holder<OperationalResultType>(opResult));

        //THEN
        try {
        	ObjectContainerType accountAfter = repositoryPort.getObject(SampleObjects.ACCOUNTSHADOWTYPE_OPENDJ_JBOND.getOID(), new PropertyReferenceListType());
        	assertTrue(false);
        } catch (FaultMessage fault) {
        	// This is expected
        }

        //deleted
        InternalSearchOperation op = controller.getInternalConnection().processSearch(
                "dc=example,dc=com",
                SearchScope.WHOLE_SUBTREE,
                "(uid=jbond)");

        assertEquals(0, op.getEntriesSent());

        assertFalse(djUtil.existsDN("uid=jbond,ou=People,dc=example,dc=com"));

    }

    @Test
    public void testTestConnection() throws Exception {
        //given

        ProvisioningService service = new ProvisioningService();
        service.setRepositoryPort(repositoryPort);

        //when
        ResourceTestResultType result = service.testResource("ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2");

        //then
        assertNotNull(result);
        TestResultType connectorConnectionResult = result.getConnectorConnection();
        assertNotNull(connectorConnectionResult);
        assertTrue(connectorConnectionResult.isSuccess());

    }

}
