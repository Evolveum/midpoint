/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.model.sync;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestCorrelationConfiramtionEvaluator extends AbstractInternalModelIntegrationTest{
  
	private static final String TEST_DIR = "src/test/resources/sync";
	private static final String CORRELATION_OR_FILTER = TEST_DIR + "/correlation-or-filter.xml";
	private static final String CORRELATION_FIRST_FILTER = TEST_DIR + "/correlation-first-filter.xml";
	private static final String CORRELATION_SECOND_FILTER = TEST_DIR + "/correlation-second-filter.xml";
	private static final String CORRELATION_WITH_CONDITION = TEST_DIR + "/correlation-with-condition.xml";
	private static final String CORRELATION_WITH_CONDITION_EMPL_NUMBER = TEST_DIR + "/correlation-with-condition-emplNumber.xml";
	
	@Autowired(required=true)
	private RepositoryService repositoryService;
	
	@Autowired(required = true)
	private CorrelationConfirmationEvaluator evaluator;
	
	

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		
	}
	
	@Test
	public void test001CorrelationOrFilter() throws Exception{
		String TEST_NAME = "testCorrelationOrFilter";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();
		
		importObjectFromFile(USER_JACK_FILENAME);
			
		PrismObject<UserType> userType = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
		//assert jack
		assertNotNull(userType);
			
		ShadowType shadow = parseObjectType(new File(ACCOUNT_SHADOW_JACK_DUMMY_FILENAME), ShadowType.class);
		
		QueryType query = PrismTestUtil.unmarshalObject(new File(CORRELATION_OR_FILTER), QueryType.class);
		List<QueryType> queries = new ArrayList<QueryType>();
		queries.add(query);
		
		ResourceType resourceType = parseObjectType(new File(RESOURCE_DUMMY_FILENAME), ResourceType.class);
		List<PrismObject<UserType>> matchedUsers = evaluator.findUsersByCorrelationRule(shadow, queries, resourceType, result);
		
		assertNotNull("Correlation evaluator returned null collection of matched users.", matchedUsers);
		assertEquals("Found more than one user.", 1, matchedUsers.size());
		
		PrismObject<UserType> jack = matchedUsers.get(0);
		assertUser(jack, "c0c010c0-d34d-b33f-f00d-111111111111", "jack", "Jack Sparrow", "Jack", "Sparrow");
		
	}
	
	@Test
	public void test002CorrelationMoreThanOne() throws Exception{
		String TEST_NAME = "testCorrelationMoreThanOne";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();
		
//		importObjectFromFile(USER_JACK_FILENAME);
			
		PrismObject<UserType> userType = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
		//assert jack
		assertNotNull(userType);
			
		ShadowType shadow = parseObjectType(new File(ACCOUNT_SHADOW_JACK_DUMMY_FILENAME), ShadowType.class);
		
		List<QueryType> queries = new ArrayList<QueryType>();
		QueryType query = PrismTestUtil.unmarshalObject(new File(CORRELATION_FIRST_FILTER), QueryType.class);		
		queries.add(query);
		
		query = PrismTestUtil.unmarshalObject(new File(CORRELATION_SECOND_FILTER), QueryType.class);		
		queries.add(query);
		
		ResourceType resourceType = parseObjectType(new File(RESOURCE_DUMMY_FILENAME), ResourceType.class);
		List<PrismObject<UserType>> matchedUsers = evaluator.findUsersByCorrelationRule(shadow, queries, resourceType, result);
		
		assertNotNull("Correlation evaluator returned null collection of matched users.", matchedUsers);
		assertEquals("Found more than one user.", 1, matchedUsers.size());
		
		PrismObject<UserType> jack = matchedUsers.get(0);
		assertUser(jack, "c0c010c0-d34d-b33f-f00d-111111111111", "jack", "Jack Sparrow", "Jack", "Sparrow");
		
	}
	
	@Test
	public void test002CorrelationWithCondition() throws Exception{
		String TEST_NAME = "testCorrelationMoreThanOne";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();
		
//		importObjectFromFile(USER_JACK_FILENAME);
			
		PrismObject<UserType> userType = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
		//assert jack
		assertNotNull(userType);
			
		ShadowType shadow = parseObjectType(new File(ACCOUNT_SHADOW_JACK_DUMMY_FILENAME), ShadowType.class);
		
		List<QueryType> queries = new ArrayList<QueryType>();
		QueryType query = PrismTestUtil.unmarshalObject(new File(CORRELATION_WITH_CONDITION), QueryType.class);		
		queries.add(query);
		
		query = PrismTestUtil.unmarshalObject(new File(CORRELATION_WITH_CONDITION_EMPL_NUMBER), QueryType.class);		
		queries.add(query);
		
		ResourceType resourceType = parseObjectType(new File(RESOURCE_DUMMY_FILENAME), ResourceType.class);
		List<PrismObject<UserType>> matchedUsers = evaluator.findUsersByCorrelationRule(shadow, queries, resourceType, result);
		
		assertNotNull("Correlation evaluator returned null collection of matched users.", matchedUsers);
		assertEquals("Found more than one user.", 1, matchedUsers.size());
		
		PrismObject<UserType> jack = matchedUsers.get(0);
		assertUser(jack, "c0c010c0-d34d-b33f-f00d-111111111111", "jack", "Jack Sparrow", "Jack", "Sparrow");
		
	}
}
