package com.evolveum.midpoint.model.impl.lens;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.io.File;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordHistoryEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPasswordPolicyProcessor extends AbstractLensTest {

	private static final String BASE_PATH = "src/test/resources/lens";

	private static final String PASSWORD_HISTORY_POLICY_OID = "policy00-0000-0000-0000-000000000003";
	private static final String PASSWORD_HISTORY_POLICY_NAME = "password-policy-history.xml";
	private static final File PASSWORD_HISTORY_POLICY_FILE = new File(BASE_PATH,
			PASSWORD_HISTORY_POLICY_NAME);

	private static final String PASSWORD_NO_HISTORY_POLICY_OID = "policy00-0000-0000-0000-000000000004";
	private static final String PASSWORD_NO_HISTORY_POLICY_NAME = "password-policy-no-history.xml";
	private static final File PASSWORD_NO_HISTORY_POLICY_FILE = new File(BASE_PATH,
			PASSWORD_NO_HISTORY_POLICY_NAME);

	// private static final String SECURITY_POLICY_PASSWORD_POLICY_OID =
	// "policy00-0000-0000-0000-000000000003";
	// private static final String SECURITY_POLICY_PASSWORD_POLICY_NAME =
	// "password-policy-history.xml";
	// private static final File SECURITY_POLICY_PASSWORD_POLICY_FILE = new
	// File(BASE_PATH, SECURITY_POLICY_PASSWORD_POLICY_NAME);

	private static final String OPERATION_REPLACE_PASSWORD_POLICY = "initSystemPasswordPolicy";

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		repoAddObjectFromFile(PASSWORD_HISTORY_POLICY_FILE, ValuePolicyType.class, initResult);
		repoAddObjectFromFile(PASSWORD_NO_HISTORY_POLICY_FILE, ValuePolicyType.class, initResult);

		deleteObject(UserType.class, USER_JACK_OID);

	}

	@Test
	public void test000initPasswordPolicyForHistory() throws Exception {
		String title = "test000initPasswordPolicyForHistory";
		initPasswordPolicy(title, PASSWORD_HISTORY_POLICY_OID);

	}

	@Test
	public void test100createUserWithPassword() throws Exception {
		display("test100createUserWithPassword");
		// WHEN
		addObject(USER_JACK_FILE);

		// THEN
		PrismObject<UserType> jack = getObject(UserType.class, USER_JACK_OID);
		assertNotNull("User Jack was not found.", jack);

		UserType jackType = jack.asObjectable();
		CredentialsType credentialsType = jackType.getCredentials();
		assertNotNull("No credentials set for user Jack", credentialsType);

		PasswordType passwordType = credentialsType.getPassword();
		assertNotNull("No password set for user Jack", passwordType);

		List<PasswordHistoryEntryType> historyEntriesType = passwordType.getHistoryEntry();
		assertEquals("Unexpected number of history entries", 1, historyEntriesType.size());
		PasswordHistoryEntryType historyEntryType = historyEntriesType.get(0);

		ProtectedStringType historyPassword = historyEntryType.getValue();
		ProtectedStringType currentPassword = passwordType.getValue();

		assertEquals("Passwords don't match", currentPassword, historyPassword);

	}

	@Test
	public void test101modifyUserPassword() throws Exception {
		String title = "test100modifyUserPassword";
		display(title);
		Task task = taskManager.createTaskInstance(title);
		OperationResult result = task.getResult();

		// WHEN
		ProtectedStringType newValue = new ProtectedStringType();
		newValue.setClearValue("ch4nGedPa33word");

		modifyObjectReplaceProperty(UserType.class, USER_JACK_OID,
				new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE), task,
				result, newValue);

		// THEN
		PrismObject<UserType> jack = getObject(UserType.class, USER_JACK_OID);
		assertNotNull("User Jack was not found.", jack);

		UserType jackType = jack.asObjectable();
		CredentialsType credentialsType = jackType.getCredentials();
		assertNotNull("No credentials set for user Jack", credentialsType);

		PasswordType passwordType = credentialsType.getPassword();
		assertNotNull("No password set for user Jack", passwordType);
		ProtectedStringType passwordAfterChange = passwordType.getValue();
		assertNotNull("Password musn't be null", passwordAfterChange);
		assertEquals("Password doesn't match", "ch4nGedPa33word",
				protector.decryptString(passwordAfterChange));

		List<PasswordHistoryEntryType> historyEntriesType = passwordType.getHistoryEntry();
		assertEquals("Unexpected number of history entries", 2, historyEntriesType.size());

		checkHistoryEntriesValue(historyEntriesType,
				new String[] { "deadmentellnotales", "ch4nGedPa33word" });

	}

	@Test
	public void test102modifyUserPassword() throws Exception {
		String title = "test102removeUserPassword";
		display(title);
		Task task = taskManager.createTaskInstance(title);
		OperationResult result = task.getResult();

		// WHEN
		ProtectedStringType newValue = new ProtectedStringType();
		newValue.setClearValue("ch4nGedPa33w0rd");

		modifyObjectReplaceProperty(UserType.class, USER_JACK_OID,
				new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE), task,
				result, newValue);

		// THEN
		PrismObject<UserType> jack = getObject(UserType.class, USER_JACK_OID);
		assertNotNull("User Jack was not found.", jack);

		UserType jackType = jack.asObjectable();
		CredentialsType credentialsType = jackType.getCredentials();
		assertNotNull("No credentials set for user Jack", credentialsType);

		PasswordType passwordType = credentialsType.getPassword();
		assertNotNull("No password set for user Jack", passwordType);
		ProtectedStringType passwordAfterChange = passwordType.getValue();
		assertNotNull("Password musn't be null", passwordAfterChange);
		assertEquals("Password doesn't match", "ch4nGedPa33w0rd",
				protector.decryptString(passwordAfterChange));

		List<PasswordHistoryEntryType> historyEntriesType = passwordType.getHistoryEntry();
		assertEquals("Unexpected number of history entries", 3, historyEntriesType.size());

		checkHistoryEntriesValue(historyEntriesType,
				new String[] { "deadmentellnotales", "ch4nGedPa33w0rd", "ch4nGedPa33word" });

	}

	@Test
	public void test103modifyUserPasswordAgain() throws Exception {
		String title = "test103modifyUserPasswordAgain";
		Task task = taskManager.createTaskInstance(title);
		OperationResult result = task.getResult();

		// WHEN
		ProtectedStringType newValue = new ProtectedStringType();
		newValue.setClearValue("ch4nGedP433w0rd");
		modifyObjectReplaceProperty(UserType.class, USER_JACK_OID,
				new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE), task,
				result, newValue);

		// THEN
		PrismObject<UserType> jackAfterSecondChange = getObject(UserType.class, USER_JACK_OID);
		assertNotNull("User Jack was not found.", jackAfterSecondChange);

		UserType jackTypeAfterSecondChange = jackAfterSecondChange.asObjectable();
		CredentialsType credentialsTypeAfterSecondChange = jackTypeAfterSecondChange.getCredentials();
		assertNotNull("No credentials set for user Jack", credentialsTypeAfterSecondChange);

		PasswordType passwordTypeAfterSecondChnage = credentialsTypeAfterSecondChange.getPassword();
		assertNotNull("No password set for user Jack", passwordTypeAfterSecondChnage);
		ProtectedStringType passwordAfterSecondChange = passwordTypeAfterSecondChnage.getValue();
		assertNotNull("Password musn't be null", passwordAfterSecondChange);
		assertEquals("Password doesn't match", "ch4nGedP433w0rd",
				protector.decryptString(passwordAfterSecondChange));

		List<PasswordHistoryEntryType> historyEntriesTypeAfterSecondChange = passwordTypeAfterSecondChnage
				.getHistoryEntry();
		assertEquals("Unexpected number of history entries", 3, historyEntriesTypeAfterSecondChange.size());

		checkHistoryEntriesValue(historyEntriesTypeAfterSecondChange,
				new String[] { "ch4nGedP433w0rd", "ch4nGedPa33w0rd", "ch4nGedPa33word" });

	}

	@Test
	public void test104modifyUserPasswordSamePassword() throws Exception {
		String title = "test103modifyUserPasswordAgain";
		Task task = taskManager.createTaskInstance(title);
		OperationResult result = task.getResult();

		// WHEN
		ProtectedStringType newValue = new ProtectedStringType();
		newValue.setClearValue("ch4nGedP433w0rd");
		try {
			modifyObjectReplaceProperty(UserType.class, USER_JACK_OID,
					new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE),
					task, result, newValue);
			fail("Expected PolicyViolationException but didn't get one.");
		} catch (PolicyViolationException ex) {
			// THIS IS OK
		}
	}

	@Test
	public void test200initNoHistoryPasswordPolicy() throws Exception {
		String title = "test200initNoHistoryPasswordPolicy";
		initPasswordPolicy(title, PASSWORD_NO_HISTORY_POLICY_OID);
	}

	@Test
	public void test201deleteUserJack() throws Exception {
		String title = "test201deleteUserJack";
		display(title);

		// WHEN
		deleteObject(UserType.class, USER_JACK_OID);

		try {
			getObject(UserType.class, USER_JACK_OID);
			fail("Unexpected user Jack, should be deleted.");
		} catch (ObjectNotFoundException ex) {
			// this is OK;
		}

	}

	@Test
	public void test202createUserJackNoPasswordHisotry() throws Exception {
		String title = "test201createUserJackNoPasswordHisotry";
		display(title);

		// WHEN
		addObject(USER_JACK_FILE);

		// THEN
		PrismObject<UserType> userJack = getObject(UserType.class, USER_JACK_OID);
		assertNotNull("Expected to find user Jack, but no one exists here", userJack);

		UserType userJackType = userJack.asObjectable();
		CredentialsType credentials = userJackType.getCredentials();
		assertNotNull("User Jack has no credentials", credentials);

		PasswordType password = credentials.getPassword();
		assertNotNull("User Jack has no password", password);

		List<PasswordHistoryEntryType> historyEntries = password.getHistoryEntry();
		assertEquals("Expected no history entries, but found: " + historyEntries.size(), 0,
				historyEntries.size());

	}

	@Test
	public void test203modifyUserJackPasswordNoPasswordHisotry() throws Exception {
		String title = "test202modifyUserJackPasswordNoPasswordHisotry";
		display(title);
		Task task = taskManager.createTaskInstance(title);
		OperationResult result = task.getResult();

		// WHEN
		ProtectedStringType newValue = new ProtectedStringType();
		newValue.setClearValue("n0Hist0ryEntr7");

		modifyObjectReplaceProperty(UserType.class, USER_JACK_OID,
				new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE), task,
				result, newValue);

		// THEN
		PrismObject<UserType> userJack = getObject(UserType.class, USER_JACK_OID);
		assertNotNull("Expected to find user Jack, but no one exists here", userJack);

		UserType userJackType = userJack.asObjectable();
		CredentialsType credentials = userJackType.getCredentials();
		assertNotNull("User Jack has no credentials", credentials);

		PasswordType password = credentials.getPassword();
		assertNotNull("User Jack has no password", password);

		List<PasswordHistoryEntryType> historyEntries = password.getHistoryEntry();
		assertEquals("Expected no history entries, but found: " + historyEntries.size(), 0,
				historyEntries.size());

	}

	private void checkHistoryEntriesValue(List<PasswordHistoryEntryType> historyEntriesType,
			String[] changedPasswords) {
		for (PasswordHistoryEntryType historyEntry : historyEntriesType) {
			boolean found = false;
			try {
				String clearValue = protector.decryptString(historyEntry.getValue());
				for (String changedPassword : changedPasswords) {
					if (changedPassword.equals(clearValue)) {
						found = true;
					}
				}

				if (!found) {
					fail("Unexpected value saved in between password hisotry entries: " + clearValue);
				}
			} catch (EncryptionException e) {
				AssertJUnit.fail("Could not encrypt password");
			}

		}
	}

	private void initPasswordPolicy(String title, String passwordPolicyOid) throws Exception {
		display(title);
		Task task = createTask(title);
		OperationResult result = task.getResult();

		ObjectReferenceType passwordPolicyRef = ObjectTypeUtil.createObjectRef(passwordPolicyOid,
				ObjectTypes.PASSWORD_POLICY);
		modifyObjectReplaceReference(SystemConfigurationType.class, SYSTEM_CONFIGURATION_OID,
				SystemConfigurationType.F_GLOBAL_PASSWORD_POLICY_REF, task, result,
				passwordPolicyRef.asReferenceValue());

		PrismObject<SystemConfigurationType> systemConfiguration = getObject(SystemConfigurationType.class,
				SYSTEM_CONFIGURATION_OID);
		assertNotNull("System configuration cannot be null", systemConfiguration);

		SystemConfigurationType systemConfigurationType = systemConfiguration.asObjectable();
		ObjectReferenceType globalPasswordPolicy = systemConfigurationType.getGlobalPasswordPolicyRef();
		assertNotNull("Expected that global password policy is configured", globalPasswordPolicy);
		assertEquals("Password policies don't match", passwordPolicyOid, globalPasswordPolicy.getOid());

	}

}
