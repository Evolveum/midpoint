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

/**
 * 
 */
package com.evolveum.midpoint.provisioning.impl.csv;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.getAttributeValue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.provisioning.impl.opendj.TestOpenDj;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType.Host;

/**
 * The test of Provisioning service on the API level. The test is using CSV resource.
 * 
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestCsvGuid extends AbstractCsvTest {

	private static final File RESOURCE_CSV_GUID_FILE = new File(TEST_DIR, "resource-csv-guid.xml");
	private static final String RESOURCE_CSV_GUID_OID = "b39e0b10-2449-11e7-b0c1-73f52bb2a496";
	
	private static final File ACCOUNT_JACK_FILE = new File(TEST_DIR, "account-jack-guid.xml");;
	private static final String ACCOUNT_JACK_OID = "2db718b6-243a-11e7-a9e5-bbb2545f80ed";
	private static final String ACCOUNT_JACK_GUID = "007";
	private static final String ACCOUNT_JACK_UNAME = "jack";
	
	private static final File CSV_SOURCE_FILE = new File(TEST_DIR, "midpoint-guid.csv");
	
	protected static final String ATTR_GUID = "guid";
	protected static final QName ATTR_GUID_QNAME = new QName(RESOURCE_NS, ATTR_GUID);
	
	protected static final String ATTR_UNAME = "uname";
	protected static final QName ATTR_UNAME_QNAME = new QName(RESOURCE_NS, ATTR_UNAME);

	private static final Trace LOGGER = TraceManager.getTrace(TestCsvGuid.class);

	@Override
	protected File getResourceFile() {
		return RESOURCE_CSV_GUID_FILE;
	}

	@Override
	protected String getResourceOid() {
		return RESOURCE_CSV_GUID_OID;
	}

	@Override
	protected File getSourceCsvFile() {
		return CSV_SOURCE_FILE;
	}
	
	@Override
	protected File getAccountJackFile() {
		return ACCOUNT_JACK_FILE;
	}

	@Override
	protected String getAccountJackOid() {
		return ACCOUNT_JACK_OID;
	}	

	@Override
	protected void assertAccountDefinition(ObjectClassComplexTypeDefinition accountDef) {
		
		assertEquals("Unexpected number of definitions", 5, accountDef.getDefinitions().size());
				
		ResourceAttributeDefinition<String> guidDef = accountDef.findAttributeDefinition(ATTR_GUID);
		assertNotNull("No definition for guid", guidDef);
		assertEquals(1, guidDef.getMaxOccurs());
		assertEquals(1, guidDef.getMinOccurs());
		assertTrue("No guid create", guidDef.canAdd());
		assertTrue("No guid update", guidDef.canModify());
		assertTrue("No guid read", guidDef.canRead());
		
		ResourceAttributeDefinition<String> unameDef = accountDef.findAttributeDefinition(ATTR_UNAME);
		assertNotNull("No definition for uname", unameDef);
		assertEquals(1, unameDef.getMaxOccurs());
		assertEquals(0, unameDef.getMinOccurs()); // TODO: should be 1
		assertTrue("No uname create", unameDef.canAdd());
		assertTrue("No uname update", unameDef.canModify());
		assertTrue("No uname read", unameDef.canRead());
		
		assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
		assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
	}

	@Override
	protected void assertAccountJackAttributes(ShadowType shadowType) {
		assertEquals("Wrong guid", ACCOUNT_JACK_GUID, getAttributeValue(shadowType, ATTR_GUID_QNAME));
		assertEquals("Wrong uname", ACCOUNT_JACK_UNAME, getAttributeValue(shadowType, ATTR_UNAME_QNAME));
        assertEquals("Wrong firstname", ACCOUNT_JACK_FIRSTNAME, getAttributeValue(shadowType, ATTR_FIRSTNAME_QNAME));
        assertEquals("Wrong lastname", ACCOUNT_JACK_LASTNAME, getAttributeValue(shadowType, ATTR_LASTNAME_QNAME));
	}

	@Override
	protected void assertAccountJackAttributesRepo(ShadowType repoShadowType) {
		assertEquals("Wrong guid (repo)", ACCOUNT_JACK_GUID, getAttributeValue(repoShadowType, ATTR_GUID_QNAME));
        assertEquals("Wrong uname (repo)", ACCOUNT_JACK_UNAME, getAttributeValue(repoShadowType, ATTR_UNAME_QNAME));
	}
}
