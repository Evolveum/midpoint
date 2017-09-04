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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

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
import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestSemiManual extends AbstractManualResourceTest {

	protected static final File CSV_SOURCE_FILE = new File(TEST_DIR, "semi-manual.csv");
	protected static final File CSV_TARGET_FILE = new File("target/semi-manual.csv");

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
	protected String getRoleOneOid() {
		return ROLE_ONE_SEMI_MANUAL_OID;
	}

	@Override
	protected File getRoleOneFile() {
		return ROLE_ONE_SEMI_MANUAL_FILE;
	}

	@Override
	protected String getRoleTwoOid() {
		return ROLE_TWO_SEMI_MANUAL_OID;
	}

	@Override
	protected File getRoleTwoFile() {
		return ROLE_TWO_SEMI_MANUAL_FILE;
	}

	@Override
	protected boolean supportsBackingStore() {
		return true;
	}

	@Override
	protected boolean hasMultivalueInterests() {
		return false;
	}

	@Override
	protected void assertResourceSchemaBeforeTest(Element resourceXsdSchemaElementBefore) {
		AssertJUnit.assertNull("Resource schema sneaked in before test connection", resourceXsdSchemaElementBefore);
	}

	@Override
	protected int getNumberOfAccountAttributeDefinitions() {
		return 5;
	}

	/**
	 * MID-4002
	 */
	@Test
	public void test700AssignAccountJackExisting() throws Exception {
		final String TEST_NAME = "test700AssignAccountJack";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		if (accountJackOid != null) {
			PrismObject<ShadowType> shadowRepoBefore = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
			display("Repo shadow before", shadowRepoBefore);
			assertPendingOperationDeltas(shadowRepoBefore, 0);
		}

		backingStoreAddJack();

		clock.overrideDuration("PT5M");

		accountJackReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		assignAccount(USER_JACK_OID, getResourceOid(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		assertSuccess(result);
		assertNull("Unexpected ticket in result", result.getAsynchronousOperationReference());

		accountJackReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		accountJackOid = getSingleLinkOid(userAfter);

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
		display("Repo shadow", shadowRepo);
		assertPendingOperationDeltas(shadowRepo, 0);
		assertShadowExists(shadowRepo, true);
		assertNoShadowPassword(shadowRepo);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountJackOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_JACK_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_JACK_USERNAME);
		assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_JACK_FULL_NAME);
		assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
		assertShadowExists(shadowModel, true);

		assertPendingOperationDeltas(shadowModel, 0);
	}

	/**
	 * MID-4002
	 */
	@Test
	public void test710UnassignAccountJack() throws Exception {
		final String TEST_NAME = "test710UnassignAccountJack";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clock.overrideDuration("PT5M");

		accountJackReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		unassignAccount(USER_JACK_OID, getResourceOid(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		jackLastCaseOid = assertInProgress(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		accountJackOid = getSingleLinkOid(userAfter);

		accountJackReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
		display("Repo shadow", shadowRepo);

		assertPendingOperationDeltas(shadowRepo, 1);
		PendingOperationType pendingOperation = findPendingOperation(shadowRepo, OperationResultStatusType.IN_PROGRESS);
		assertPendingOperation(shadowRepo, pendingOperation, accountJackReqestTimestampStart, accountJackReqestTimestampEnd);
		assertNotNull("No ID in pending operation", pendingOperation.getId());

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountJackOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_JACK_USERNAME);
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
		assertShadowPassword(shadowModel);

		assertPendingOperationDeltas(shadowModel, 1);
		pendingOperation = findPendingOperation(shadowModel, OperationResultStatusType.IN_PROGRESS);
		assertPendingOperation(shadowModel, pendingOperation, accountJackReqestTimestampStart, accountJackReqestTimestampEnd);

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountJackOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertShadowName(shadowModelFuture, USER_JACK_USERNAME);
		assertUnassignedFuture(shadowModelFuture, true);

		assertCase(jackLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
	}

	/**
	 * MID-4002
	 */
	@Test
	public void test712CloseCaseAndRecomputeJack() throws Exception {
		final String TEST_NAME = "test712CloseCaseAndRecomputeJack";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		backingStoreDeleteJack();

		closeCase(jackLastCaseOid);

		accountJackCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		// We need reconcile and not recompute here. We need to fetch the updated case status.
		reconcileUser(USER_JACK_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		assertSuccess(result);

		accountJackCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

		PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
		display("Repo shadow", shadowRepo);
		assertSinglePendingOperation(shadowRepo,
				accountJackReqestTimestampStart, accountJackReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountJackCompletionTimestampStart, accountJackCompletionTimestampEnd);
		assertUnassignedShadow(shadowRepo, null);

		PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
				accountJackOid, null, task, result);

		display("Model shadow", shadowModel);
		ShadowType shadowTypeModel = shadowModel.asObjectable();
		assertShadowName(shadowModel, USER_JACK_USERNAME);
		assertEquals("Wrong kind (model)", ShadowKindType.ACCOUNT, shadowTypeModel.getKind());
		assertUnassignedShadow(shadowModel, ActivationStatusType.DISABLED);

		PendingOperationType pendingOperation = assertSinglePendingOperation(shadowModel,
				accountJackReqestTimestampStart, accountJackReqestTimestampEnd,
				OperationResultStatusType.SUCCESS,
				accountJackCompletionTimestampStart, accountJackCompletionTimestampEnd);

		PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
				accountJackOid,
				SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
				task, result);
		display("Model shadow (future)", shadowModelFuture);
		assertShadowName(shadowModelFuture, USER_JACK_USERNAME);
		assertUnassignedFuture(shadowModelFuture, false);

		assertCase(jackLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	/**
	 * MID-4002
	 */
	@Test
	public void test717RecomputeJackAfter30min() throws Exception {
		final String TEST_NAME = "test717RecomputeJackAfter30min";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clock.overrideDuration("PT30M");

		// WHEN
		displayWhen(TEST_NAME);
		// We need reconcile and not recompute here. We need to fetch the updated case status.
		reconcileUser(USER_JACK_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		display("result", result);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertDeprovisionedTimedOutUser(userAfter, accountJackOid);

		assertCase(jackLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
	}

	/**
	 * Put everything in a clean state so we can start over.
	 */
	@Test
	public void test719CleanUp() throws Exception {
		final String TEST_NAME = "test719CleanUp";
		displayTestTitle(TEST_NAME);

		cleanupUser(TEST_NAME, USER_JACK_OID, USER_JACK_USERNAME, accountJackOid);
	}

	@Override
	protected void backingStoreProvisionWill(String interest) throws IOException {
		appendToCsv(new String[]{USER_WILL_NAME, USER_WILL_FULL_NAME, ACCOUNT_WILL_DESCRIPTION_MANUAL, interest, "false", USER_WILL_PASSWORD_OLD});
	}

	@Override
	protected void backingStoreUpdateWill(String newFullName, String interest, ActivationStatusType newAdministrativeStatus, String password) throws IOException {
		String disabled;
		if (newAdministrativeStatus == ActivationStatusType.ENABLED) {
			disabled = "false";
		} else {
			disabled = "true";
		}
		replaceInCsv(new String[]{USER_WILL_NAME, newFullName, ACCOUNT_WILL_DESCRIPTION_MANUAL, interest, disabled, password});
	}

	@Override
	protected void backingStoreDeprovisionWill() throws IOException {
		deprovisionInCsv(USER_WILL_NAME);
	}

	protected void backingStoreAddJack() throws IOException {
		appendToCsv(new String[]{USER_JACK_USERNAME, USER_JACK_FULL_NAME, ACCOUNT_JACK_DESCRIPTION_MANUAL, "", "false", USER_JACK_PASSWORD_OLD});
	}

	protected void backingStoreDeleteJack() throws IOException {
		deprovisionInCsv(USER_JACK_USERNAME);
	}

	protected void deprovisionInCsv(String username) throws IOException {
		deleteInCsv(username);
	}

	protected void disableInCsv(String username) throws IOException {
		String[] data = readFromCsv(username);
		data[4] = "true";
		replaceInCsv(data);
	}

	protected String[] readFromCsv(String username) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get(CSV_TARGET_FILE.getPath()));
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String[] cols = line.split(",");
			if (cols[0].matches("\""+username+"\"")) {
				return unescape(cols);
			}
		}
		return null;
	}

	private String[] unescape(String[] cols) {
		String[] out = new String[cols.length];
		for (int i = 0; i < cols.length; i++) {
			if (cols[i] != null && !cols[i].isEmpty()) {
				out[i] = cols[i].substring(1, cols[i].length() - 1);
			}
		}
		return out;
	}

	protected void appendToCsv(String[] data) throws IOException {
		String line = formatCsvLine(data);
		Files.write(Paths.get(CSV_TARGET_FILE.getPath()), line.getBytes(), StandardOpenOption.APPEND);
	}

	protected void replaceInCsv(String[] data) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get(CSV_TARGET_FILE.getPath()));
		boolean found = false;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String[] cols = line.split(",");
			if (cols[0].matches("\""+data[0]+"\"")) {
				lines.set(i, formatCsvLine(data));
				found = true;
			}
		}
		if (!found) {
			throw new IllegalStateException("Not found in CSV: "+data[0]);
		}
		Files.write(Paths.get(CSV_TARGET_FILE.getPath()), lines,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	protected void deleteInCsv(String username) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get(CSV_TARGET_FILE.getPath()));
		Iterator<String> iterator = lines.iterator();
		while (iterator.hasNext()) {
			String line = iterator.next();
			String[] cols = line.split(",");
			if (cols[0].matches("\""+username+"\"")) {
				iterator.remove();
			}
		}
		Files.write(Paths.get(CSV_TARGET_FILE.getPath()), lines,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	private String formatCsvLine(String[] data) {
		return Arrays.stream(data).map(s -> "\""+s+"\"").collect(Collectors.joining(","));
	}

	@Override
	protected void displayBackingStore() throws IOException {
		display("CSV", dumpCsv());
	}

	protected String dumpCsv() throws IOException {
		return StringUtils.join(Files.readAllLines(Paths.get(CSV_TARGET_FILE.getPath())), "\n");
	}

	@Override
	protected void assertShadowPassword(PrismObject<ShadowType> shadow) {
		// CSV password is readable
		PrismProperty<PolyStringType> passValProp = shadow.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
		assertNotNull("No password value property in "+shadow+": "+passValProp, passValProp);
	}
}