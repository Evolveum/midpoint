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

import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType.Value;
import java.util.List;
import java.util.Set;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import com.evolveum.midpoint.provisioning.util.ShadowUtil;
import com.evolveum.midpoint.provisioning.objects.ResourceAttribute;
import org.junit.After;
import javax.xml.bind.JAXBException;
import org.junit.Test;
import java.io.InputStream;
import java.io.File;
import javax.xml.bind.JAXBContext;
import com.evolveum.midpoint.provisioning.objects.ResourceObject;
import javax.xml.parsers.ParserConfigurationException;
import com.evolveum.midpoint.provisioning.schema.ResourceSchema;
import com.evolveum.midpoint.provisioning.schema.util.ObjectValueWriter;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.midpoint.test.repository.BaseXDatabaseFactory;
import com.evolveum.midpoint.test.util.SampleObjects;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathSegment;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import com.evolveum.midpoint.xml.schema.XPathType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage;

import java.util.ArrayList;
import javax.xml.ws.Holder;
import org.junit.Before;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author elek
 */
public class ProvisioningServiceTest {

    private JAXBContext ctx;
    private RepositoryPortType repositoryPort;

    public ProvisioningServiceTest() throws JAXBException {
        ctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
    }

    @Before
    public void initXmlDatabasep() throws Exception {
        //Create Repository mock
        repositoryPort = BaseXDatabaseFactory.getRepositoryPort();

        //Make suere the Repository is mocked
        assertNotNull(repositoryPort);
    }

    @After
    public void resetXmlDatabase() {
        BaseXDatabaseFactory.XMLServerStop();
    }

    @Test
    public void testGetObject() throws Exception {
        //given
        final ResourceAccessInterface rai = mock(ResourceAccessInterface.class);

        OperationalResultType opResult = new OperationalResultType();

        ProvisioningService service = new ProvisioningService() {

            @Override
            protected ResourceAccessInterface getResourceAccessInterface(ResourceObjectShadowType shadow) throws com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.FaultMessage {
                return rai;
            }
        };

        BaseResourceIntegration bri = new BaseResourceIntegration((ResourceType) TestUtil.getSampleObject(SampleObjects.RESOURCETYPE_LOCALHOST_OPENDJ));
        ResourceObject ro = createSampleResourceObject(bri.getSchema());


        when(rai.get(
                any(OperationalResultType.class),
                any(ResourceObject.class))).thenReturn(ro);

        when(rai.getConnector()).thenReturn(bri);


        service.setRepositoryPort(repositoryPort);

        //when
        ObjectContainerType result = service.getObject(SampleObjects.ACCOUNTSHADOWTYPE_OPENDJ_JBOND.getOID(), new PropertyReferenceListType(), new Holder<OperationalResultType>(opResult));

        //then
        JAXBContext jaxbContext = JAXBContext.newInstance(Result.class);
        Marshaller m = jaxbContext.createMarshaller();
        m.marshal(new Result(result), new File("target/getObjectResult.xml"));
    }

    @XmlRootElement
    protected static class Result {

        private ObjectContainerType objectContainer;

        public Result() {
        }

        public Result(ObjectContainerType objectContainer) {
            this.objectContainer = objectContainer;
        }

        public ObjectContainerType getObjectContainer() {
            return objectContainer;
        }

        public void setObjectContainer(ObjectContainerType objectContainer) {
            this.objectContainer = objectContainer;
        }
    }

    @Test
    public void testAddObject() throws Exception {
        //given
        final ResourceAccessInterface rai = mock(ResourceAccessInterface.class);

        OperationalResultType opResult = new OperationalResultType();


        ProvisioningService service = new ProvisioningService() {

            @Override
            protected ResourceAccessInterface getResourceAccessInterface(ResourceObjectShadowType shadow) throws com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.FaultMessage {
                return rai;
            }
        };



        //Create Sample Request
        InputStream in = getClass().getResourceAsStream("/addShadow.xml");
        JAXBElement<ResourceObjectShadowType> o = (JAXBElement<ResourceObjectShadowType>) ctx.createUnmarshaller().unmarshal(in);
        ObjectContainerType container = new ObjectContainerType();
        container.setObject(o.getValue());

        ObjectContainerType resourceContainer = repositoryPort.getObject(o.getValue().getResourceRef().getOid(), new PropertyReferenceListType());
        ResourceType resource = (ResourceType) resourceContainer.getObject();
        assertNotNull(resource);

        //TODO: Fix it when the ResourceSchema parser is fixed
        BaseResourceIntegration bri = new BaseResourceIntegration(resource);

        ResourceObject ro = createSampleResourceObject(bri.getSchema());

        //when
        when(rai.add(
                any(OperationalResultType.class),
                any(ResourceObject.class),
                any(ResourceObjectShadowType.class))).thenReturn(ro);
        when(rai.get(
                any(OperationalResultType.class),
                any(ResourceObject.class))).thenReturn(null);
        when(rai.getConnector()).thenReturn(bri);

        
        service.setRepositotyService(repositoryPort);

        // TODO: Handle scripts
        ScriptsType scripts = new ScriptsType();

        String result = service.addObject(container, scripts, new Holder<OperationalResultType>(opResult));

        assertNotNull(result);
        ObjectContainerType accountContainer = repositoryPort.getObject(result, new PropertyReferenceListType());
        assertNotNull(accountContainer.getObject());
    }

    @Test
    public void testAddResourceObject() throws Exception {
        //given
        final ResourceAccessInterface rai = mock(ResourceAccessInterface.class);


        OperationalResultType opResult = new OperationalResultType();


        ProvisioningService service = new ProvisioningService() {

            @Override
            protected ResourceAccessInterface getResourceAccessInterface(ResourceObjectShadowType shadow) throws com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.FaultMessage {
                return rai;
            }
        };



        //Create Sample Request
        InputStream in = getClass().getResourceAsStream("/addShadowRO.xml");
        JAXBElement<ResourceObjectShadowType> o = (JAXBElement<ResourceObjectShadowType>) ctx.createUnmarshaller().unmarshal(in);
        ObjectContainerType container = new ObjectContainerType();
        container.setObject(o.getValue());

        ObjectContainerType resourceContainer = repositoryPort.getObject(o.getValue().getResourceRef().getOid(), new PropertyReferenceListType());
        ResourceType resource = (ResourceType) resourceContainer.getObject();
        assertNotNull(resource);

        //TODO: Fix it when the ResourceSchema parser is fixed
        BaseResourceIntegration bri = new BaseResourceIntegration(resource);

        ResourceObject ro = createSampleResourceObject(bri.getSchema());

        //when
        when(rai.add(
                any(OperationalResultType.class),
                any(ResourceObject.class),
                any(ResourceObjectShadowType.class))).thenReturn(ro);
        when(rai.get(
                any(OperationalResultType.class),
                any(ResourceObject.class))).thenReturn(null);
        when(rai.getConnector()).thenReturn(bri);

        
        service.setRepositotyService(repositoryPort);

        // TODO: Handle scripts
        ScriptsType scripts = new ScriptsType();

        String result = service.addObject(container, scripts, new Holder<OperationalResultType>(opResult));

        assertNotNull(result);
        ObjectContainerType accountContainer = repositoryPort.getObject(result, new PropertyReferenceListType());
        assertNotNull(accountContainer.getObject());
    }

    @Test
    public void testDeleteObject() throws Exception {
        //given
        final ResourceAccessInterface rai = mock(ResourceAccessInterface.class);

        OperationalResultType opResult = new OperationalResultType();

        ProvisioningService service = new ProvisioningService() {

            @Override
            protected ResourceAccessInterface getResourceAccessInterface(ResourceObjectShadowType shadow) throws com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.FaultMessage {
                return rai;
            }
        };
        ResourceType resource = (ResourceType) TestUtil.getSampleObject(SampleObjects.RESOURCETYPE_LOCALHOST_OPENDJ);
        assertNotNull(resource);
        BaseResourceIntegration bri = new BaseResourceIntegration(resource);

        when(rai.getConnector()).thenReturn(bri);

        service.setRepositoryPort(repositoryPort);

        // TODO: Handle scripts
        ScriptsType scripts = new ScriptsType();

        //when
        service.deleteObject(SampleObjects.ACCOUNTSHADOWTYPE_OPENDJ_JBOND.getOID(), scripts, new Holder<OperationalResultType>(opResult));

        try {
        	ObjectContainerType accountContainer = repositoryPort.getObject(SampleObjects.ACCOUNTSHADOWTYPE_OPENDJ_JBOND.getOID(), new PropertyReferenceListType());
        	assertTrue(false);
        } catch (FaultMessage fault) {
        	// This is expected
        }
    }

    private ResourceObject createSampleResourceObject(ResourceSchema schema) throws ParserConfigurationException {
        ObjectValueWriter valueWriter = ObjectValueWriter.getInstance();
        ResourceObjectShadowType shadow = (ResourceObjectShadowType) TestUtil.getSampleObject(SampleObjects.ACCOUNTSHADOWTYPE_OPENDJ_JBOND);
        return valueWriter.buildResourceObject(shadow, schema);
    }

    private ResourceSchema createSampleResourceSchema() {
        return new BaseResourceIntegration((ResourceType) TestUtil.getSampleObject(SampleObjects.RESOURCETYPE_LOCALHOST_OPENDJ)).getSchema();
    }

    @Test
    public void processObjectChanges() throws Exception {
        //GIVEN
        ResourceSchema schema = createSampleResourceSchema();
        ResourceObject obj = createSampleResourceObject(schema);



        ObjectModificationType oct = new ObjectModificationType();

        PropertyModificationType pct = new PropertyModificationType();
        pct.setModificationType(PropertyModificationTypeType.add);
        Document doc = ShadowUtil.getXmlDocument();

        List<XPathSegment> segments = new ArrayList<XPathSegment>();
        segments.add(new XPathSegment(SchemaConstants.I_ATTRIBUTES));
        XPathType xpath = new XPathType(segments);
        pct.setPath(xpath.toElement(SchemaConstants.NS_C, "path", doc));

        //<dj:givenName>test...</dj:givenName>
        Element e = doc.createElementNS(schema.getResourceNamespace(), "givenName");
        e.appendChild(doc.createTextNode("testtesttest"));

        pct.setValue(new Value());
        pct.getValue().getAny().add(e);

        oct.getPropertyModification().add(pct);

        ProvisioningService service = new ProvisioningService();

        //WHEN
        Set<AttributeChange> attributeChanges = service.processObjectChanges(schema, obj.getDefinition(), oct);

        //THEN
        assertNotNull(attributeChanges);
        assertEquals(1, attributeChanges.size());
        ResourceAttribute ra = attributeChanges.iterator().next().getAttribute();
        assertNotNull(ra);
        assertEquals(new QName(schema.getResourceNamespace(), "givenName"), ra.getDefinition().getQName());


        assertEquals("testtesttest", ra.getSingleJavaValue(String.class));



    }
}
