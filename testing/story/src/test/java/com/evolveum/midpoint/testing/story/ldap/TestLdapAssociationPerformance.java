/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.testing.story.ldap;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.sql.SqlPerformanceMonitor;
import com.evolveum.midpoint.repo.sql.SqlRepositoryServiceImpl;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.caching.CachePerformanceCollector;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.opends.server.util.LDIFException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Performance tests for accounts with large number of role assignments and group associations; using assignmentTargetSearch,
 * associationFromLink and similar mappings.
 * 
 * MID-5341
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdapAssociationPerformance extends AbstractLdapTest {

	public static final File TEST_DIR = new File(LDAP_TEST_DIR, "assoc-perf");

	private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
	private static final String RESOURCE_OPENDJ_OID = "aeff994e-381a-4fb3-af3b-f0f5dcdc9653";
	private static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;

	private static final File ROLE_LDAP_FILE = new File(TEST_DIR, "role-ldap.xml");
	private static final File ROLE_META_FILE = new File(TEST_DIR, "role-meta.xml");
	private static final String ROLE_META_OID = "d723af35-857f-4931-adac-07cc66c4c235";
	private static final File USER_TEMPLATE_FILE = new File(TEST_DIR, "user-template.xml");
	private static final String USER_TEMPLATE_OID = "f86c9851-5724-4c6a-a7e8-59e25a6d4fd1";

	private static final File ROLE_TEST_FILE = new File(TEST_DIR, "role-test.xml");
	private static final String ROLE_TEST_OID = "f2ba978d-ef70-436b-83e6-2ca53bdc8cf2";

	private static final File USER_TEST_FILE = new File(TEST_DIR, "user-test.xml");
	private static final String USER_TEST_OID = "4206f8c9-1ef8-4b17-9ca7-52a4b1439e95";

	private static final File TASK_RECOMPUTE_1_FILE = new File(TEST_DIR, "task-recompute-1.xml");
	private static final String TASK_RECOMPUTE_1_OID = "e3a446c5-07ef-4cbd-9bc9-d37fa5a10d70";

	protected static final int NUMBER_OF_GENERATED_USERS = 20;
	protected static final String GENERATED_USER_NAME_FORMAT = "u%06d";
	protected static final String GENERATED_USER_FULL_NAME_FORMAT = "Random J. U%06d";
	protected static final String GENERATED_USER_GIVEN_NAME_FORMAT = "Random";
	protected static final String GENERATED_USER_FAMILY_NAME_FORMAT = "U%06d";
	protected static final String GENERATED_USER_OID_FORMAT = "11111111-0000-ffff-1000-000000%06d";

	protected static final int NUMBER_OF_GENERATED_ROLES = 100;
	protected static final String GENERATED_ROLE_NAME_FORMAT = "role-%06d";
	protected static final String GENERATED_ROLE_OID_FORMAT = "22222222-0000-ffff-1000-000000%06d";

	private static final int RECOMPUTE_TASK_WAIT_TIMEOUT = 60000;

	private static final String SUMMARY_LINE_FORMAT = "%30s: %5d ms (%4d ms/user, %7.2f ms/user/role)\n";
	private static final String REPO_LINE_FORMAT = "%30s: %6d (%8.1f/%s)\n";

	private Map<String,Long> durations = new LinkedHashMap<>();

	private PrismObject<ResourceType> resourceOpenDj;

	@Override
    protected void startResources() throws Exception {
        openDJController.startCleanServerRI();
    }

    @AfterClass
    public static void stopResources() throws Exception {
        openDJController.stop();
    }

	@Override
	protected int getNumberOfRoles() {
		return super.getNumberOfRoles() + 2;            // superuser + ldap, meta
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		// Resources
		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);	
		openDJController.setResource(resourceOpenDj);

		importObjectFromFile(ROLE_LDAP_FILE);
		importObjectFromFile(ROLE_META_FILE);
		importObjectFromFile(USER_TEMPLATE_FILE);
		setDefaultUserTemplate(USER_TEMPLATE_OID);

//		InternalMonitor.setTrace(InternalOperationClasses.CONNECTOR_OPERATIONS, true);
	}

	@Override
	protected String getLdapResourceOid() {
		return RESOURCE_OPENDJ_OID;
	}
	
	@Test
    public void test010Sanity() throws Exception {
		final String TEST_NAME = "test010Sanity";
        displayTestTitle(TEST_NAME);
        
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        addObject(ROLE_TEST_FILE, task, result);
        addObject(USER_TEST_FILE, task, result);

     	// THEN
     	displayThen(TEST_NAME);
     	
        dumpLdap();
		openDJController.assertUniqueMember("cn=role-test,ou=groups,dc=example,dc=com", "uid=user-test,ou=people,dc=example,dc=com");
        assertLdapConnectorInstances(1);

        deleteObject(UserType.class, USER_TEST_OID);
        deleteObject(RoleType.class, ROLE_TEST_OID);
	}

	@Test
	public void test020GenerateRoles() throws Exception {
		final String TEST_NAME = "test020GenerateRoles";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);

		SqlPerformanceMonitor.Snapshot base = getRepoPerformanceMonitor().createSnapshot();
		resetCachePerformanceCollector();

		long startMillis = System.currentTimeMillis();

		generateObjects(RoleType.class, NUMBER_OF_GENERATED_ROLES, GENERATED_ROLE_NAME_FORMAT, GENERATED_ROLE_OID_FORMAT,
				(role, i) ->
						role.beginAssignment()
								.targetRef(ROLE_META_OID, RoleType.COMPLEX_TYPE),
				role -> addObject(role, task, result),
				result);

		// THEN
		displayThen(TEST_NAME);

		long endMillis = System.currentTimeMillis();
		recordDuration(TEST_NAME, (endMillis - startMillis));

		SqlPerformanceMonitor.Snapshot snapshot = getRepoPerformanceMonitor().createDifferenceSnapshot(base);
		dumpRepoSnapshot("SQL operations for " + TEST_NAME, snapshot, "role", NUMBER_OF_GENERATED_ROLES);
		dumpCachePerformanceData(TEST_NAME);

		result.computeStatus();
		assertSuccess(result);

		assertRoles(getNumberOfRoles() + NUMBER_OF_GENERATED_ROLES);

		//dumpLdap();
		assertLdapConnectorInstances(1);
	}

	@Test
	public void test100AddUsers() throws Exception {
		final String TEST_NAME = "test100AddUsers";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);

		SqlPerformanceMonitor.Snapshot base = getRepoPerformanceMonitor().createSnapshot();
		resetCachePerformanceCollector();

		long startMillis = System.currentTimeMillis();

		generateObjects(UserType.class, NUMBER_OF_GENERATED_USERS, GENERATED_USER_NAME_FORMAT, GENERATED_USER_OID_FORMAT,
				(user, i) -> {
					user
							.fullName(String.format(GENERATED_USER_FULL_NAME_FORMAT, i))
							.givenName(String.format(GENERATED_USER_GIVEN_NAME_FORMAT, i))
							.familyName(String.format(GENERATED_USER_FAMILY_NAME_FORMAT, i));
					PrismProperty<Object> memberOf = null;
					try {
						memberOf = user.asPrismObject().createExtension().getValue()
								.findOrCreateProperty(new ItemName("memberOf"));
						for (int roleIndex = 0; roleIndex < NUMBER_OF_GENERATED_ROLES; roleIndex++) {
							memberOf.addRealValue(String.format(GENERATED_ROLE_NAME_FORMAT, roleIndex));
						}
					} catch (SchemaException e) {
						throw new AssertionError(e.getMessage(), e);
					}
				},
				user -> addObject(user, task, result),
				result);

		// THEN
		displayThen(TEST_NAME);

		long endMillis = System.currentTimeMillis();
		recordDuration(TEST_NAME, (endMillis - startMillis));

		SqlPerformanceMonitor.Snapshot snapshot = getRepoPerformanceMonitor().createDifferenceSnapshot(base);
		dumpRepoSnapshotPerUser("SQL operations for " + TEST_NAME, snapshot);
		dumpCachePerformanceData(TEST_NAME);

		result.computeStatus();
		assertSuccess(result);
		//dumpLdap();
		assertLdapConnectorInstances(1);
	}

	private void dumpRepoSnapshotPerUser(String label, SqlPerformanceMonitor.Snapshot snapshot) {
		dumpRepoSnapshot(label, snapshot, "user", NUMBER_OF_GENERATED_USERS);
	}

	private void dumpRepoSnapshot(String label, SqlPerformanceMonitor.Snapshot snapshot, String unit, int unitCount) {
		Map<String, Integer> counters = snapshot.counters;
		ArrayList<String> kinds = new ArrayList<>(counters.keySet());
		kinds.sort(String::compareToIgnoreCase);
		StringBuilder sb = new StringBuilder();
		kinds.forEach(kind -> sb.append(String.format(REPO_LINE_FORMAT, kind, counters.get(kind), (double) counters.get(kind) / unitCount, unit)));
		display(label + " (" + NUMBER_OF_GENERATED_USERS + " users, " + NUMBER_OF_GENERATED_ROLES + " roles)", sb.toString());
	}

	@Test
	public void test110RecomputeUsers() throws Exception {
		final String TEST_NAME = "test110RecomputeUsers";
		displayTestTitle(TEST_NAME);

		rememberConnectorResourceCounters();

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);

		addTask(TASK_RECOMPUTE_1_FILE);

		SqlPerformanceMonitor.Snapshot base = getRepoPerformanceMonitor().createSnapshot();
		resetCachePerformanceCollector();

		waitForTaskFinish(TASK_RECOMPUTE_1_OID, true, RECOMPUTE_TASK_WAIT_TIMEOUT);

		// THEN
		displayThen(TEST_NAME);

		recordDuration(TEST_NAME,getRunDurationMillis(TASK_RECOMPUTE_1_OID));

		SqlPerformanceMonitor.Snapshot snapshot = getRepoPerformanceMonitor().createDifferenceSnapshot(base);
		dumpRepoSnapshotPerUser("SQL operations for " + TEST_NAME, snapshot);
		dumpCachePerformanceData(TEST_NAME);

		assertUsers(getNumberOfUsers() + NUMBER_OF_GENERATED_USERS);

		assertLdapAccounts(getNumberOfLdapAccounts() + NUMBER_OF_GENERATED_USERS);
		assertLdapConnectorInstances(1);

		assertSteadyResource();
		//assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
		assertCounterIncrement(InternalCounters.CONNECTOR_MODIFICATION_COUNT, 0);
	}

	@Test
    public void test900Summarize() throws Exception {
		final String TEST_NAME = "test900Summarize";
        displayTestTitle(TEST_NAME);
        
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Long> entry : durations.entrySet()) {
        	sb.append(summary(entry.getKey(), entry.getValue()));
        }
        display("Summary ("+NUMBER_OF_GENERATED_USERS+" users, "+NUMBER_OF_GENERATED_ROLES+" roles)", sb.toString());
     	
     	// THEN
     	displayThen(TEST_NAME);

     	// TODO: more thresholds

	}
	
	private void rememberConnectorResourceCounters() {
		rememberCounter(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT);
        rememberCounter(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT);
        rememberCounter(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT);
        rememberCounter(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_MODIFICATION_COUNT);
	}
	
	private void assertSteadyResource() {
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT, 0);
	}
	
	private void ruinLdapAccounts() throws DirectoryException, LDIFException, IOException {
		for (Entry entry : openDJController.search("objectclass="+OBJECTCLASS_INETORGPERSON)) {
			String cn = openDJController.getAttributeValue(entry, "cn");
			if (cn.startsWith("Random")) {
				cn = cn.replace("Random", "Broken");
				openDJController.modifyReplace(entry.getDN().toString(), "cn", cn);
//				display("Replaced", openDJController.fetchEntry(entry.getDN().toString()));
//			} else {
//				display("NOT RANDOM: "+cn, entry);
			}
		}
		dumpLdap();
	}
	
//	protected void assertLdapAccounts() throws DirectoryException {
//		List<? extends Entry> entries = openDJController.search("objectclass="+OBJECTCLASS_INETORGPERSON);
//		int randoms = 0;
//		for (Entry entry : openDJController.search("objectclass="+OBJECTCLASS_INETORGPERSON)) {
//			String cn = openDJController.getAttributeValue(entry, "cn");
//			if (cn.startsWith("Broken")) {
//				fail("Broken LDAP account: "+entry);
//			}
//			if (cn.startsWith("Random")) {
//				randoms++;
//			}
//		}
//		assertEquals("Wrong number of Random LDAP accounts", NUMBER_OF_GENERATED_USERS, randoms);
//	}

	private long getRunDurationMillis(String taskReconOpendjOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		PrismObject<TaskType> reconTask = getTask(taskReconOpendjOid);
     	return (XmlTypeConverter.toMillis(reconTask.asObjectable().getLastRunFinishTimestamp()) 
     			- XmlTypeConverter.toMillis(reconTask.asObjectable().getLastRunStartTimestamp()));
	}
	
	protected void assertLdapConnectorInstances() throws NumberFormatException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException, IOException, InterruptedException {
		assertLdapConnectorInstances(2,4);
	}

	private long recordDuration(String label, long duration) {
		durations.put(label, duration);
		return duration;
	}
	
	private Object summary(String label, long duration) {
		return String.format(SUMMARY_LINE_FORMAT, label, duration, duration/NUMBER_OF_GENERATED_USERS, (double) duration/(NUMBER_OF_GENERATED_USERS * NUMBER_OF_GENERATED_ROLES));
	}

	private SqlPerformanceMonitor getRepoPerformanceMonitor() {
		return ((SqlRepositoryServiceImpl) repositoryService).getPerformanceMonitor();
	}

	private void resetCachePerformanceCollector() {
		CachePerformanceCollector.INSTANCE.clear();
	}

	private void dumpCachePerformanceData(String testName) {
		display("Cache performance data for " + testName, CachePerformanceCollector.INSTANCE);
	}
}