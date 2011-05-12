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

import org.junit.Ignore;
import com.evolveum.midpoint.provisioning.conversion.JAXBConverter;
import com.evolveum.midpoint.xml.ns._public.resource.idconnector.configuration_1.ConnectorRef;
import com.evolveum.midpoint.provisioning.objects.ResourceAttribute;
import com.evolveum.midpoint.provisioning.objects.ResourceObject;
import com.evolveum.midpoint.provisioning.schema.AttributeFlag;
import com.evolveum.midpoint.provisioning.schema.ResourceAttributeDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceObjectDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceSchema;
import com.evolveum.midpoint.provisioning.schema.util.ObjectValueWriter;
import com.evolveum.midpoint.test.repository.BaseXDatabaseFactory;
import com.evolveum.midpoint.test.util.SampleObjects;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import java.util.List;
import javax.xml.namespace.QName;
import org.junit.After;
import org.junit.Before;
import org.w3c.dom.Element;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 
 * @author elek
 */
public class IdentityConnectorTest {

	private RepositoryPortType repositoryPort;

	@Before
	public void initXmlDatabasep() throws Exception {
		// Create Repository mock
		repositoryPort = BaseXDatabaseFactory.getRepositoryPort();

		// Make suere the Repository is mocked
		assertNotNull(repositoryPort);
	}

	@After
	public void resetXmlDatabase() {
		BaseXDatabaseFactory.XMLServerStop();
	}

	public IdentityConnectorTest() {
	}

	@Test
	@Ignore("It required non-canonical example data. Should be refactored after the new schema born")
	public void buildConfigurationObject() throws Exception {
		// GIVEN
		ObjectValueWriter ovw = new ObjectValueWriter();

		// schema creation
		String ICCI = "http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/configuration-1.xsd";

		ResourceSchema schema = new ResourceSchema(ICCI);
		schema.addConverter(new JAXBConverter(ICCI, ConnectorRef.class));

		ResourceObjectDefinition def = new ResourceObjectDefinition(new QName(ICCI, "configuration"));

		ResourceAttributeDefinition connectorRef = new ResourceAttributeDefinition(new QName(ICCI,
				"ConnectorRef"));
		connectorRef.setType(new QName(ICCI, "ConnectorRef"));
		connectorRef.getAttributeFlag().add(AttributeFlag.NOT_UPDATEABLE);

		def.addAttribute(connectorRef);

		schema.addObjectClass(def);

		assertNotNull(schema.getConverterFactory().getConverter(new QName(ICCI, "ConnectorRef")));

		// load values
		ResourceType rt = (ResourceType) repositoryPort.getObject(
				SampleObjects.RESOURCETYPE_LOCALHOST_OPENDJ.getOID(), new PropertyReferenceListType())
				.getObject();
		List<Element> values = rt.getConfiguration().getAny();

		// when
		ResourceObject ro = ovw.readValues(def, values);

		// then
		ResourceAttribute a = ro.getValue(new QName(ICCI, "ConnectorRef"));
		assertNotNull(a);
		ConnectorRef ref = a.getSingleJavaValue(ConnectorRef.class);
		assertEquals("1.0.5531", ref.getBundleVersion());
	}
}
