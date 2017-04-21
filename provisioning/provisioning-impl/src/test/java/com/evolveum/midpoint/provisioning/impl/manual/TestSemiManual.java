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
package com.evolveum.midpoint.provisioning.impl.manual;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestSemiManual extends AbstractManualResourceTest {
	
	private static final File CSV_SOURCE_FILE = new File(TEST_DIR, "semi-manual.csv");
	private static final File CSV_TARGET_FILE = new File("target/semi-manual.csv");
	
	private static final Trace LOGGER = TraceManager.getTrace(TestSemiManual.class);
	
	protected static final String ATTR_DISABLED = "disabled";
	protected static final QName ATTR_DISABLED_QNAME = new QName(MidPointConstants.NS_RI, ATTR_DISABLED);
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		resource = addResource(initResult);
		resourceType = resource.asObjectable();
		
		FileUtils.copyFile(CSV_SOURCE_FILE, CSV_TARGET_FILE);
	}

	@Override
	protected File getResourceFile() {
		return RESOURCE_SEMI_MANUAL_FILE;
	}
	
	@Override
	protected boolean supportsBackingStore() {
		return true;
	}
	
	protected PrismObject<ResourceType> addResource(OperationResult result)
			throws JAXBException, SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
		PrismObject<ResourceType> resource = prismContext.parseObject(getResourceFile());
		fillInConnectorRef(resource, MANUAL_CONNECTOR_TYPE, result);
		fillInAdditionalConnectorRef(resource, "csv", CSV_CONNECTOR_TYPE, result);
		CryptoUtil.encryptValues(protector, resource);
		display("Adding resource ", resource);
		String oid = repositoryService.addObject(resource, null, result);
		resource.setOid(oid);
		return resource;
	}
	
	@Override
	protected void assertResourceSchemaBeforeTest(Element resourceXsdSchemaElementBefore) {
		AssertJUnit.assertNull("Resource schema sneaked in before test connection", resourceXsdSchemaElementBefore);
	}
	
	@Override
	protected int getNumberOfAccountAttributeDefinitions() {
		return 5;
	}

	@Override
	protected void backingStoreAddWill() throws IOException {
		appendToCsv(new String[]{ACCOUNT_WILL_USERNAME, ACCOUNT_WILL_FULLNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL, "", "false", ACCOUNT_WILL_PASSWORD});
	}

	private void appendToCsv(String[] data) throws IOException {
		String line = formatCsvLine(data) + "\n";
		Files.write(Paths.get(CSV_TARGET_FILE.getPath()), line.getBytes(), StandardOpenOption.APPEND);
	}

	private String formatCsvLine(String[] data) {
		return Arrays.stream(data).map(s -> "\""+s+"\"").collect(Collectors.joining(","));
	}

	@Override
	protected void assertShadowPassword(PrismObject<ShadowType> shadow) {
		// CSV password is readable
		PrismProperty<PolyStringType> passValProp = shadow.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
		assertNotNull("No password value property in "+shadow+": "+passValProp, passValProp);
	}
}