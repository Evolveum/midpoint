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
package com.evolveum.midpoint.model.impl.misc;

import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.controller.ModelController;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestCaseManagement extends AbstractInternalModelIntegrationTest {

	protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "misc");
	protected static final File USER1_FILE = new File(TEST_DIR, "user1.xml");
	protected static final File USER2_FILE = new File(TEST_DIR, "user2.xml");
	protected static final File CASE1_FILE = new File(TEST_DIR, "case1.xml");
	protected static final File CASE2_FILE = new File(TEST_DIR, "case2.xml");
	protected static final File CASE3_FILE = new File(TEST_DIR, "case3.xml");

	@Autowired protected ModelController controller;
	@Autowired protected Projector projector;
	@Autowired protected Clockwork clockwork;
	@Autowired protected TaskManager taskManager;

	private PrismObject<UserType> user1, user2;
	private PrismObject<CaseType> case1, case2, case3;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		user1 = repoAddObjectFromFile(USER1_FILE, initResult);
		user2 = repoAddObjectFromFile(USER2_FILE, initResult);
		case1 = repoAddObjectFromFile(CASE1_FILE, initResult);
		case2 = repoAddObjectFromFile(CASE2_FILE, initResult);
		case3 = repoAddObjectFromFile(CASE3_FILE, initResult);
	}

	@Test
	public void test100SearchCases() throws Exception {
		final String TEST_NAME = "test100SearchCases";

		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();

		login(userAdministrator);

		SearchResultList<PrismObject<CaseType>> cases = controller.searchObjects(CaseType.class, null, null, task, result);
		display("objects", cases);
		assertEquals(3, cases.size());
	}

	@Test
	public void test110SearchWorkItems() throws Exception {
		final String TEST_NAME = "test110SearchWorkItems";

		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();

		login(userAdministrator);

		SearchResultList<CaseWorkItemType> workItems = controller.searchContainers(CaseWorkItemType.class, null, null, task, result);
		display("objects", workItems);
		assertEquals(4, workItems.size());
	}

	@Test
	public void test120SearchCasesFiltered() throws Exception {
		final String TEST_NAME = "test120SearchCasesFiltered";

		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();

		login(userAdministrator);

		ObjectQuery query = QueryBuilder.queryFor(CaseType.class, prismContext)
				.item(CaseType.F_OBJECT_REF).ref(userAdministrator.getOid())
				.build();
		SearchResultList<PrismObject<CaseType>> cases = controller.searchObjects(CaseType.class, query, null, task, result);
		display("objects", cases);
		assertEquals(1, cases.size());
	}

	@Test
	public void test130SearchWorkItemsFiltered() throws Exception {
		final String TEST_NAME = "test130SearchWorkItemsFiltered";

		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();

		login(userAdministrator);

		ObjectQuery query = QueryBuilder.queryFor(CaseWorkItemType.class, prismContext)
				.item(CaseWorkItemType.F_ASSIGNEE_REF).ref(user1.getOid())
				.build();

		SearchResultList<CaseWorkItemType> workItems = controller.searchContainers(CaseWorkItemType.class, query, null, task, result);
		display("objects", workItems);
		assertEquals(2, workItems.size());
	}

	@Test
	public void test140SearchCasesSortedPaged() throws Exception {
		final String TEST_NAME = "test140SearchCasesSortedPaged";

		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();

		login(userAdministrator);

		ObjectQuery query = QueryBuilder.queryFor(CaseType.class, prismContext)
				.desc(CaseType.F_NAME)
				.offset(1)
				.maxSize(1)
				.build();
		SearchResultList<PrismObject<CaseType>> cases = controller.searchObjects(CaseType.class, query, null, task, result);
		display("objects", cases);
		assertEquals(1, cases.size());
		assertEquals(case2, cases.get(0));
	}

	@Test
	public void test150SearchWorkItemsSortedPaged() throws Exception {
		final String TEST_NAME = "test150SearchWorkItemsSortedPaged";

		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();

		login(userAdministrator);

		ObjectQuery query = QueryBuilder.queryFor(CaseWorkItemType.class, prismContext)
				.desc(CaseWorkItemType.F_DEADLINE)
				.maxSize(1)
				.build();

		SearchResultList<CaseWorkItemType> workItems = controller.searchContainers(CaseWorkItemType.class, query, null, task, result);
		display("objects", workItems);
		assertEquals(1, workItems.size());
		assertEquals(2, workItems.get(0).getId().longValue());
	}

	@Test
	public void test160SearchWorkItemsSorted() throws Exception {
		final String TEST_NAME = "test160SearchWorkItemsSorted";

		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();

		login(userAdministrator);

		ObjectQuery query = QueryBuilder.queryFor(CaseWorkItemType.class, prismContext)
				.desc(CaseWorkItemType.F_DEADLINE)
				.build();

		SearchResultList<CaseWorkItemType> workItems = controller.searchContainers(CaseWorkItemType.class, query, null, task, result);
		display("objects", workItems);
		assertEquals(4, workItems.size());
	}

}
