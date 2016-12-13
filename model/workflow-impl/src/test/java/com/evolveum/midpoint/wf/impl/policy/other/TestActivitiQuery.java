/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.wf.impl.policy.other;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.wf.impl.policy.AbstractWfTestPolicy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestActivitiQuery extends AbstractWfTestPolicy {

	@Override
	protected PrismObject<UserType> getDefaultActor() {
		return userAdministrator;
	}

	@Test
	public void test100SearchByMoreAssignees() throws Exception {
		final String TEST_NAME = "test100SearchByMoreAssignees";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		assignRole(userJackOid, roleRole1aOid, task, result);				// should start approval process
		assertNotAssignedRole(userJackOid, roleRole1aOid, task, result);

		{
			SearchResultList<WorkItemType> itemsAll = modelService.searchContainers(WorkItemType.class, null, null, task, result);
			assertEquals("Wrong # of total work items", 1, itemsAll.size());
		}

		{
			ObjectQuery query2 = QueryBuilder.queryFor(WorkItemType.class, prismContext)
					.item(WorkItemType.F_ASSIGNEE_REF).ref(userLead1Oid)
					.build();
			SearchResultList<WorkItemType> items2 = modelService.searchContainers(WorkItemType.class, query2, null, task, result);
			assertEquals("Wrong # of work items found using single-assignee query", 1, items2.size());
		}

		{
			List<PrismReferenceValue> refs = new ArrayList<>();
			refs.add(ObjectTypeUtil.createObjectRef("oid-number-1", ObjectTypes.USER).asReferenceValue());
			refs.add(ObjectTypeUtil.createObjectRef(userLead1Oid, ObjectTypes.USER).asReferenceValue());
			refs.add(ObjectTypeUtil.createObjectRef("oid-number-3", ObjectTypes.USER).asReferenceValue());
			ObjectQuery query3 = QueryBuilder.queryFor(WorkItemType.class, prismContext)
					.item(WorkItemType.F_ASSIGNEE_REF).ref(refs)
					.build();
			SearchResultList<WorkItemType> items3 = modelService
					.searchContainers(WorkItemType.class, query3, null, task, result);
			assertEquals("Wrong # of work items found using multi-assignee query", 1, items3.size());
		}
	}

}
