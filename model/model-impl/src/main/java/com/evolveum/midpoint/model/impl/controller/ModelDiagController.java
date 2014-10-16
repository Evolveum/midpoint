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
package com.evolveum.midpoint.model.impl.controller;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelDiagnosticService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.RandomString;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 *
 */
@Component
public class ModelDiagController implements ModelDiagnosticService {
	
	public static final String CLASS_NAME_WITH_DOT = ModelDiagController.class.getName() + ".";
	private static final String REPOSITORY_SELF_TEST_USER = CLASS_NAME_WITH_DOT + "repositorySelfTest.user";
	
	private static final String NAME_PREFIX = "selftest";
	private static final int NAME_RANDOM_LENGTH = 5;
	
	private static final String USER_FULL_NAME = "Grăfula Fèlix Teleke z Tölökö";
	private static final String USER_GIVEN_NAME = "Fëľïx";
	private static final String USER_FAMILY_NAME = "Ţæĺêké";
	private static final String[] USER_ORGANIZATION = {"COMITATVS NOBILITVS HVNGARIÆ", "Salsa Verde ğomorula prïvatûła"};
	private static final String[] USER_EMPLOYEE_TYPE = {"Ģŗąfųŀą", "CANTATOR"};

	private static final String INSANE_NATIONAL_STRING = "Pørúga ném nå väšȍm apârátula";
	
	private static final Trace LOGGER = TraceManager.getTrace(ModelDiagController.class);

	
	@Autowired(required = true)
	private PrismContext prismContext;
	
	@Autowired(required = true)
	@Qualifier("repositoryService")
	private transient RepositoryService repositoryService;
	
	@Autowired(required = true)
	private ProvisioningService provisioningService;
	
	private RandomString randomString;

	ModelDiagController() {
		randomString = new RandomString(NAME_RANDOM_LENGTH, true);
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.ModelDiagnosticService#getRepositoryDiag(com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public RepositoryDiag getRepositoryDiag(Task task, OperationResult parentResult) {
		return repositoryService.getRepositoryDiag();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.api.ModelDiagnosticService#repositorySelfTest(com.evolveum.midpoint.task.api.Task)
	 */
	@Override
	public OperationResult repositorySelfTest(Task task) {
		OperationResult testResult = new OperationResult(REPOSITORY_SELF_TEST);
		// Give repository chance to run its own self-test if available
		repositoryService.repositorySelfTest(testResult);
		
		repositorySelfTestUser(task, testResult);
		
		testResult.computeStatus();
		return testResult;
	}

    @Override
    public OperationResult repositoryTestOrgClosureConsistency(Task task, boolean repairIfNecessary) {
        OperationResult testResult = new OperationResult(REPOSITORY_TEST_ORG_CLOSURE_CONSISTENCY);
        repositoryService.testOrgClosureConsistency(repairIfNecessary, testResult);
        testResult.computeStatus();
        return testResult;
    }

    @Override
	public OperationResult provisioningSelfTest(Task task) {
		OperationResult testResult = new OperationResult(PROVISIONING_SELF_TEST);
		// Give provisioning chance to run its own self-test
		provisioningService.provisioningSelfTest(testResult, task);
		
		testResult.computeStatus();
		return testResult;
	}

	private void repositorySelfTestUser(Task task, OperationResult testResult) {
		OperationResult result = testResult.createSubresult(REPOSITORY_SELF_TEST_USER);
		
		PrismObject<UserType> user = getObjectDefinition(UserType.class).instantiate(); 
		UserType userType = user.asObjectable();
		
		String name = generateRandomName();
		PolyStringType namePolyStringType = toPolyStringType(name);
		userType.setName(namePolyStringType);
		result.addContext("name", name);
		userType.setDescription(SelfTestData.POLICIJA);
		userType.setFullName(toPolyStringType(USER_FULL_NAME));
		userType.setGivenName(toPolyStringType(USER_GIVEN_NAME));
		userType.setFamilyName(toPolyStringType(USER_FAMILY_NAME));
		userType.setTitle(toPolyStringType(INSANE_NATIONAL_STRING));
		userType.getEmployeeType().add(USER_EMPLOYEE_TYPE[0]);
		userType.getEmployeeType().add(USER_EMPLOYEE_TYPE[1]);
		userType.getOrganization().add(toPolyStringType(USER_ORGANIZATION[0]));
		userType.getOrganization().add(toPolyStringType(USER_ORGANIZATION[1]));
		
		String oid;
		try {
			oid = repositoryService.addObject(user, null, result);
		} catch (ObjectAlreadyExistsException e) {
			result.recordFatalError(e);
			return;
		} catch (SchemaException e) {
			result.recordFatalError(e);
			return;
		} catch (RuntimeException e) {
			result.recordFatalError(e);
			return;
		}
		
		try {

			{
				OperationResult subresult = result.createSubresult(result.getOperation()+".getObject");
				
				PrismObject<UserType> userRetrieved;
				try {
					userRetrieved = repositoryService.getObject(UserType.class, oid, null, subresult);
				} catch (ObjectNotFoundException e) {
					result.recordFatalError(e);
					return;
				} catch (SchemaException e) {
					result.recordFatalError(e);
					return;
				} catch (RuntimeException e) {
					result.recordFatalError(e);
					return;
				}
				
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Self-test:user getObject:\n{}", userRetrieved.debugDump());
				}
				
				checkUser(userRetrieved, name, subresult);
				
				subresult.recordSuccessIfUnknown();
			}
			
			{
				OperationResult subresult = result.createSubresult(result.getOperation()+".searchObjects.fullName");
				try {
					
					ObjectQuery query = new ObjectQuery();
					ObjectFilter filter = EqualFilter.createEqual(UserType.F_FULL_NAME, UserType.class, prismContext, null,
							toPolyString(USER_FULL_NAME));
					query.setFilter(filter);
					subresult.addParam("query", query);
					List<PrismObject<UserType>> foundObjects = repositoryService.searchObjects(UserType.class, query , null, subresult);
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Self-test:user searchObjects:\n{}", DebugUtil.debugDump(foundObjects));
					}
					assertSingleSearchResult("user", foundObjects, subresult);
					
					PrismObject<UserType> userRetrieved = foundObjects.iterator().next();
					checkUser(userRetrieved, name, subresult);
					
					subresult.recordSuccessIfUnknown();
				} catch (SchemaException e) {
					subresult.recordFatalError(e);
					return;
				} catch (RuntimeException e) {
					subresult.recordFatalError(e);
					return;
				}
			}

			// MID-1116
			{
				OperationResult subresult = result.createSubresult(result.getOperation()+".searchObjects.employeeType");
				try {
					
					ObjectQuery query = new ObjectQuery();
					ObjectFilter filter = EqualFilter.createEqual(UserType.F_EMPLOYEE_TYPE, UserType.class, prismContext, null,
							USER_EMPLOYEE_TYPE[0]);
					query.setFilter(filter);
					subresult.addParam("query", query);
					List<PrismObject<UserType>> foundObjects = repositoryService.searchObjects(UserType.class, query , null, subresult);
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Self-test:user searchObjects:\n{}", DebugUtil.debugDump(foundObjects));
					}
					assertSingleSearchResult("user", foundObjects, subresult);
					
					PrismObject<UserType> userRetrieved = foundObjects.iterator().next();
					checkUser(userRetrieved, name, subresult);
					
					subresult.recordSuccessIfUnknown();
				} catch (SchemaException e) {
					subresult.recordFatalError(e);
					return;
				} catch (RuntimeException e) {
					subresult.recordFatalError(e);
					return;
				}
			}
			
// MID-1116
			{
				OperationResult subresult = result.createSubresult(result.getOperation()+".searchObjects.organization");
				try {
					
					ObjectQuery query = new ObjectQuery();
					ObjectFilter filter = EqualFilter.createEqual(UserType.F_ORGANIZATION, UserType.class, prismContext, null,
							toPolyString(USER_ORGANIZATION[1]));
					query.setFilter(filter);
					subresult.addParam("query", query);
					List<PrismObject<UserType>> foundObjects = repositoryService.searchObjects(UserType.class, query , null, subresult);
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Self-test:user searchObjects:\n{}", DebugUtil.debugDump(foundObjects));
					}
					assertSingleSearchResult("user", foundObjects, subresult);
					
					PrismObject<UserType> userRetrieved = foundObjects.iterator().next();
					checkUser(userRetrieved, name, subresult);
					
					subresult.recordSuccessIfUnknown();
				} catch (SchemaException e) {
					subresult.recordFatalError(e);
					return;
				} catch (RuntimeException e) {
					subresult.recordFatalError(e);
					return;
				}
			}
			

		} finally {
			
			try {
				repositoryService.deleteObject(UserType.class, oid, testResult);
			} catch (ObjectNotFoundException e) {
				result.recordFatalError(e);
				return;
			} catch (RuntimeException e) {
				result.recordFatalError(e);
				return;
			}
			
			result.computeStatus();
		}
		
	}

	private void checkUser(PrismObject<UserType> userRetrieved, String name, OperationResult subresult) {
		checkUserPropertyPolyString(userRetrieved, UserType.F_NAME, subresult, name);
		checkUserProperty(userRetrieved, UserType.F_DESCRIPTION, subresult, SelfTestData.POLICIJA);
		checkUserPropertyPolyString(userRetrieved, UserType.F_FULL_NAME, subresult, USER_FULL_NAME);
		checkUserPropertyPolyString(userRetrieved, UserType.F_GIVEN_NAME, subresult, USER_GIVEN_NAME);
		checkUserPropertyPolyString(userRetrieved, UserType.F_FAMILY_NAME, subresult, USER_FAMILY_NAME);
		checkUserPropertyPolyString(userRetrieved, UserType.F_TITLE, subresult, INSANE_NATIONAL_STRING);
		checkUserProperty(userRetrieved, UserType.F_EMPLOYEE_TYPE, subresult, USER_EMPLOYEE_TYPE);
		checkUserPropertyPolyString(userRetrieved, UserType.F_ORGANIZATION, subresult, USER_ORGANIZATION);
	}

	private void assertSingleSearchResult(String objectTypeMessage, List<PrismObject<UserType>> foundObjects, OperationResult parentResult) {
		OperationResult result = parentResult.createSubresult(parentResult.getOperation()+".numberOfResults");
		assertTrue("Found no "+objectTypeMessage, !foundObjects.isEmpty(), result);
		assertTrue("Expected to find a single "+objectTypeMessage+" but found "+foundObjects.size(), foundObjects.size() == 1, result);
		result.recordSuccessIfUnknown();
	}
	
	private <O extends ObjectType,T> void checkUserProperty(PrismObject<O> object, QName propQName, OperationResult parentResult, T... expectedValues) {
		String propName = propQName.getLocalPart();
		OperationResult result = parentResult.createSubresult(parentResult.getOperation() + "." + propName);
		PrismProperty<T> prop = object.findProperty(propQName);
		Collection<T> actualValues = prop.getRealValues();
		result.addArbitraryCollectionAsParam("actualValues", actualValues);
		assertMultivalue("User, property '"+propName+"'", expectedValues, actualValues, result);
		result.recordSuccessIfUnknown();
	}
	
	private <T> void assertMultivalue(String message, T expectedVals[], Collection<T> actualVals, OperationResult result) {
		if (expectedVals.length != actualVals.size()) {
			fail(message+": expected "+expectedVals.length+" values but has "+actualVals.size()+" values: "+actualVals, result);
			return;
		}
		for (T expected: expectedVals) {
			boolean found = false;
			for (T actual: actualVals) {
				if (expected.equals(actual)) {
					found = true;
					break;
				}
			}
			if (!found) {
				fail(message+": expected value '"+expected+"' not found in actual values "+actualVals, result);
				return;
			}
		}
	}

	private <O extends ObjectType> void checkUserPropertyPolyString(PrismObject<O> object, QName propQName, OperationResult parentResult, String... expectedValues) {
		String propName = propQName.getLocalPart();
		OperationResult result = parentResult.createSubresult(parentResult.getOperation() + "." + propName);
		PrismProperty<PolyString> prop = object.findProperty(propQName);
		Collection<PolyString> actualValues = prop.getRealValues();
		result.addArbitraryCollectionAsParam("actualValues", actualValues);
		assertMultivaluePolyString("User, property '"+propName+"'", expectedValues, actualValues, result);
		result.recordSuccessIfUnknown();
	}
	
	private void assertMultivaluePolyString(String message, String expectedOrigs[], Collection<PolyString> actualPolyStrings, OperationResult result) {
		if (expectedOrigs.length != actualPolyStrings.size()) {
			fail(message+": expected "+expectedOrigs.length+" values but has "+actualPolyStrings.size()+" values: "+actualPolyStrings, result);
			return;
		}
		for (String expectedOrig: expectedOrigs) {
			boolean found = false;
			for (PolyString actualPolyString: actualPolyStrings) {
				if (expectedOrig.equals(actualPolyString.getOrig())) {
					found = true;
					assertEquals(message+ ", norm", polyStringNorm(expectedOrig), actualPolyString.getNorm(), result);
					break;
				}
			}
			if (!found) {
				fail(message+": expected value '"+expectedOrig+"' not found in actual values "+actualPolyStrings, result);
				return;
			}
		}
	}
	
	private void assertPolyString(String message, String expectedOrig, PolyString actualPolyString, OperationResult result) {
		assertEquals(message+ ", orig", expectedOrig, actualPolyString.getOrig(), result);
		assertEquals(message+ ", norm", polyStringNorm(expectedOrig), actualPolyString.getNorm(), result);
	}

	private void assertPolyStringType(String message, String expectedName, PolyStringType actualPolyStringType, OperationResult result) {
		assertEquals(message+ ", orig", expectedName, actualPolyStringType.getOrig(), result);
		assertEquals(message+ ", norm", polyStringNorm(expectedName), actualPolyStringType.getNorm(), result);
	}

	private String polyStringNorm(String orig) {
		return prismContext.getDefaultPolyStringNormalizer().normalize(orig);
	}
	
	private void assertTrue(String message, boolean condition, OperationResult result) {
		if (!condition) {
			fail(message, result);
		}
	}

	private void assertEquals(String message, Object expected, Object actual, OperationResult result) {
		if (!MiscUtil.equals(expected, actual)) {
			fail(message + "; expected "+expected+", actual "+actual, result);
		}
	}
	
	private void fail(String message, OperationResult result) {
		result.recordFatalError(message);
		LOGGER.error("Repository self-test assertion failed: {}", message);
	}

	private String generateRandomName() {
		return NAME_PREFIX + randomString.nextString();
	}

	private PolyStringType toPolyStringType(String orig) {
		PolyStringType polyStringType = new PolyStringType();
		polyStringType.setOrig(orig);
		return polyStringType;
	}
	
	private PolyString toPolyString(String orig) {
		PolyString polyString = new PolyString(orig);
		polyString.recompute(prismContext.getDefaultPolyStringNormalizer());
		return polyString;
	}


	private <T extends ObjectType> PrismObjectDefinition<T> getObjectDefinition(Class<T> type) {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
	}

}
