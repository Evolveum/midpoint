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
package com.evolveum.midpoint.model.intest.manual;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSemiManual extends AbstractManualResourceTest {
	
	private static final File CSV_SOURCE_FILE = new File(TEST_DIR, "semi-manual.csv");
	private static final File CSV_TARGET_FILE = new File("target/semi-manual.csv");
	
	private static final Trace LOGGER = TraceManager.getTrace(TestSemiManual.class);
	
	protected static final String ATTR_DISABLED = "disabled";
	protected static final QName ATTR_DISABLED_QNAME = new QName(MidPointConstants.NS_RI, ATTR_DISABLED);
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		FileUtils.copyFile(CSV_SOURCE_FILE, CSV_TARGET_FILE);
	}
	
	@Override
	protected String getResourceOid() {
		return RESOURCE_SEMI_MANUAL_OID;
	}

	@Override
	protected File getResourceFile() {
		return RESOURCE_SEMI_MANUAL_FILE;
	}
	
	@Override
	protected boolean supportsBackingStore() {
		return true;
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
		appendToCsv(new String[]{USER_WILL_NAME, USER_WILL_FULL_NAME, ACCOUNT_WILL_DESCRIPTION_MANUAL, "", "false", USER_WILL_PASSWORD_OLD});
	}
	
	@Override
	protected void backingStoreUpdateWill(String newFullName, ActivationStatusType newAdministrativeStatus, String password) throws IOException {
		String disabled;
		if (newAdministrativeStatus == ActivationStatusType.ENABLED) {
			disabled = "false";
		} else {
			disabled = "true";
		}
		replaceInCsv(new String[]{USER_WILL_NAME, newFullName, ACCOUNT_WILL_DESCRIPTION_MANUAL, "", disabled, password});
	}
	
	@Override
	protected void backingStoreDeleteWill() throws IOException {
		deleteInCsv(USER_WILL_NAME);
	}

	private void appendToCsv(String[] data) throws IOException {
		String line = formatCsvLine(data) + "\n";
		Files.write(Paths.get(CSV_TARGET_FILE.getPath()), line.getBytes(), StandardOpenOption.APPEND);
	}
	
	private void replaceInCsv(String[] data) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get(CSV_TARGET_FILE.getPath()));
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String[] cols = line.split(",");
			if (cols[0].matches("\""+data[0]+"\"")) {
				lines.set(i, formatCsvLine(data));
			}
		}
		Files.write(Paths.get(CSV_TARGET_FILE.getPath()), lines, StandardOpenOption.WRITE);
	}
	
	private void deleteInCsv(String username) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get(CSV_TARGET_FILE.getPath()));
		Iterator<String> iterator = lines.iterator();
		while (iterator.hasNext()) {
			String line = iterator.next();
			String[] cols = line.split(",");
			if (cols[0].matches("\""+username+"\"")) {
				iterator.remove();
			}
		}
		Files.write(Paths.get(CSV_TARGET_FILE.getPath()), lines, StandardOpenOption.WRITE);
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