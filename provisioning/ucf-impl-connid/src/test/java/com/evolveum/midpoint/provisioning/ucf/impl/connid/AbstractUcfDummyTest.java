/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import java.io.File;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Simple UCF tests. No real resource, just basic setup and sanity.
 * 
 * @author Radovan Semancik
 * 
 * This is an UCF test. It shold not need repository or other things from the midPoint spring context
 * except from the provisioning beans. But due to a general issue with spring context initialization
 * this is a lesser evil for now (MID-392)
 */
@ContextConfiguration(locations = { "classpath:ctx-ucf-connid-test.xml" })
public abstract class AbstractUcfDummyTest extends AbstractTestNGSpringContextTests {

	protected static final File RESOURCE_DUMMY_FILE = new File(UcfTestUtil.TEST_DIR, "resource-dummy.xml");
	protected static final File CONNECTOR_DUMMY_FILE = new File(UcfTestUtil.TEST_DIR, "connector-dummy.xml");
	protected static final String ACCOUNT_JACK_USERNAME = "jack";

	protected ConnectorFactory connectorFactory;
	protected PrismObject<ResourceType> resource;
	protected ResourceType resourceType;
	protected ConnectorType connectorType;
	protected ConnectorInstance cc;
	protected ResourceSchema resourceSchema;
	protected static DummyResource dummyResource;
	protected static DummyResourceContoller dummyResourceCtl;

	@Autowired(required = true)
	protected ConnectorFactory connectorFactoryIcfImpl;
	
	@Autowired(required = true)
	protected PrismContext prismContext;
	
	private static Trace LOGGER = TraceManager.getTrace(AbstractUcfDummyTest.class);
	
	@BeforeClass
	public void setup() throws Exception {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
		
		dummyResourceCtl = DummyResourceContoller.create(null);
		dummyResourceCtl.setResource(resource);
		dummyResourceCtl.extendSchemaPirate();
		dummyResource = dummyResourceCtl.getDummyResource();
				
		connectorFactory = connectorFactoryIcfImpl;

		resource = PrismTestUtil.parseObject(RESOURCE_DUMMY_FILE);
		resourceType = resource.asObjectable();

		PrismObject<ConnectorType> connector = PrismTestUtil.parseObject(CONNECTOR_DUMMY_FILE);
		connectorType = connector.asObjectable();
	}
		

	protected void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
	}
	
	protected static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, propValue);
	}
	
	protected void assertContainerDefinition(PrismContainer<?> container, String contName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName qName = new QName(SchemaConstantsGenerated.NS_COMMON, contName);
		PrismAsserts.assertDefinition(container.getDefinition(), qName, xsdType, minOccurs, maxOccurs);
	}
}
