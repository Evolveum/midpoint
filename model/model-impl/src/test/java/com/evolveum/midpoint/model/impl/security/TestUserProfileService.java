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

package com.evolveum.midpoint.model.impl.security;

import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.DelegatorWithOtherPrivilegesLimitations;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType.DISABLED;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestUserProfileService extends AbstractInternalModelIntegrationTest {

	@Test
	public void test100DeputyNoLimits() throws Exception {
		final String TEST_NAME = "test100DeputyNoLimits";
		executeDeputyLimitationsTest(TEST_NAME, Collections.emptyList(), null);
	}

	@Test // MID-4111
	public void test110DeputyAssignmentDisabled() throws Exception {
		final String TEST_NAME = "test110DeputyAssignmentDisabled";
		executeDeputyLimitationsTest(TEST_NAME, null,
				(a) -> a.setActivation(new ActivationType().administrativeStatus(DISABLED)));
	}

	@Test // MID-4111
	public void test120DeputyAssignmentNotValid() throws Exception {
		final String TEST_NAME = "test120DeputyAssignmentNotValid";
		executeDeputyLimitationsTest(TEST_NAME, null,
				(a) -> a.setActivation(new ActivationType().validTo("2017-03-31T00:00:00+01:00")));
	}

	@Test
	public void test130DeputyAssignmentFalseCondition() throws Exception {
		final String TEST_NAME = "test130DeputyAssignmentFalseCondition";
		executeDeputyLimitationsTest(TEST_NAME, null,
				(a) -> a.beginCondition()
							.beginExpression()
								.expressionEvaluator(
										new JAXBElement<ScriptExpressionEvaluatorType>(new QName("script"),
												ScriptExpressionEvaluatorType.class,
												new ScriptExpressionEvaluatorType().code("false"))
								));
	}

	@Test
	public void test140DeputyBlockOtherPrivileges() throws Exception {
		final String TEST_NAME = "test140DeputyBlockOtherPrivileges";
		executeDeputyLimitationsTest(TEST_NAME, Collections.singletonList(new OtherPrivilegesLimitationType()),
				(a) -> a.limitOtherPrivileges(new OtherPrivilegesLimitationType()));
	}

	private void executeDeputyLimitationsTest(String TEST_NAME, List<OtherPrivilegesLimitationType> expectedLimitations,
			Consumer<AssignmentType> assignmentModifier) throws CommonException {
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		AssignmentType assignment = new AssignmentType()
				.targetRef(USER_JACK_OID, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEPUTY);
		if (assignmentModifier != null) {
			assignmentModifier.accept(assignment);
		}

		UserType deputy = prismContext.createObjectable(UserType.class)
				.name("deputy")
				.oid("deputy")
				.assignment(assignment);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		display("Logging in as", deputy);
		login(deputy.asPrismObject());

		// THEN
		TestUtil.displayThen(TEST_NAME);
		MidPointPrincipal principal = securityEnforcer.getPrincipal();
		Collection<DelegatorWithOtherPrivilegesLimitations> delegators = principal.getDelegatorWithOtherPrivilegesLimitationsCollection();
		display("delegators with other privileges limitations", delegators);
		if (expectedLimitations == null) {
			assertEquals("Wrong # of delegator records: " + DebugUtil.debugDump(delegators), 0, delegators.size());
		} else {
			assertEquals("Wrong # of delegator records: " + DebugUtil.debugDump(delegators), 1, delegators.size());
			DelegatorWithOtherPrivilegesLimitations record = delegators.iterator().next();
			assertEquals("Unexpected limitations: " + DebugUtil.debugDump(delegators), new HashSet<>(expectedLimitations), new HashSet<>(record.getLimitations()));
		}
	}

}
