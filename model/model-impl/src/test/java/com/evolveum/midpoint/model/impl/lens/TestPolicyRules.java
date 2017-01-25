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
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPolicyRules extends AbstractLensTest {
		
	private static final String ROLE_JUDGE_POLICY_RULE_EXCLUSION_NAME = "criminal exclusion";

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		setDefaultUserTemplate(USER_TEMPLATE_OID);
		
		addObject(ROLE_PIRATE_FILE);
		addObject(ROLE_MUTINIER_FILE);
		addObject(ROLE_JUDGE_FILE);
		addObject(ROLE_CONSTABLE_FILE);
		addObject(ROLE_THIEF_FILE);

		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		
		InternalMonitor.reset();
//		InternalMonitor.setTraceShadowFetchOperation(true);
	}

	
	/**
	 * Mostly preparation for other tests. But good check for no exclusion conflict.
	 */
	@Test
    public void test010JackAssignRoleJudge() throws Exception {
		final String TEST_NAME = "test010JackAssignRoleJudge";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_JUDGE_OID);
	}
	
	/**
	 * No exclusion here. The assignment should go smoothly.
	 */
	@Test
    public void test100AssignRoleMutinierToJack() throws Exception {
		final String TEST_NAME = "test100AssignRoleMutinierToJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        LensContext<UserType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_MUTINIER_OID);

        display("Input context", context);

        assertFocusModificationSanity(context);
        rememberShadowFetchOperationCount();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        projector.project(context, "test", task, result);
        
        // THEN        
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignAccountToJack(context);
        
        DeltaSetTriple<EvaluatedAssignmentImpl<UserType>> evaluatedAssignmentTriple = 
        		(DeltaSetTriple)context.getEvaluatedAssignmentTriple();
//        display("Output evaluatedAssignmentTriple", evaluatedAssignmentTriple);
        
        dumpPolicyRules(context);
        
        assertEvaluatedRules(context, 1);
        assertTriggeredRules(context, 0, null);
	}
	
	@Test
    public void test110AssignRolePirateToJack() throws Exception {
		final String TEST_NAME = "test110AssignRolePirateToJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        LensContext<UserType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_PIRATE_OID);

        display("Input context", context);

        assertFocusModificationSanity(context);
        rememberShadowFetchOperationCount();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        projector.project(context, "test", task, result);
        
        // THEN        
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignAccountToJack(context);
        
        DeltaSetTriple<EvaluatedAssignmentImpl<UserType>> evaluatedAssignmentTriple = 
        		(DeltaSetTriple)context.getEvaluatedAssignmentTriple();
//        display("Output evaluatedAssignmentTriple", evaluatedAssignmentTriple);
        
        dumpPolicyRules(context);
        
        assertEvaluatedRules(context, 1);
        EvaluatedExclusionTrigger trigger = (EvaluatedExclusionTrigger) assertTriggeredRule(context, PolicyConstraintKindType.EXCLUSION, false);
        assertNotNull("No conflicting assignment in trigger", trigger.getConflictingAssignment());
        assertEquals("Wrong conflicting assignment in trigger", ROLE_PIRATE_OID, trigger.getConflictingAssignment().getTarget().getOid());
	}
	
	/**
	 * Assignment with an exception from the exclusion rule.
	 */
	@Test
    public void test112AssignRolePirateWithExceptionToJack() throws Exception {
		final String TEST_NAME = "test112AssignRolePirateWithExceptionToJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        LensContext<UserType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_PIRATE_OID,
        		assignment -> {
        			PolicyExceptionType policyException = new PolicyExceptionType();
        			policyException.setRuleName(ROLE_JUDGE_POLICY_RULE_EXCLUSION_NAME);
					assignment.getPolicyException().add(policyException);
        		});

        display("Input context", context);

        assertFocusModificationSanity(context);
        rememberShadowFetchOperationCount();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        projector.project(context, "test", task, result);
        
        // THEN        
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertAssignAccountToJack(context);
        
        DeltaSetTriple<EvaluatedAssignmentImpl<UserType>> evaluatedAssignmentTriple = 
        		(DeltaSetTriple)context.getEvaluatedAssignmentTriple();
//        display("Output evaluatedAssignmentTriple", evaluatedAssignmentTriple);
        
        dumpPolicyRules(context);
        
        List<EvaluatedPolicyRule> evaluatedRules = assertEvaluatedRules(context, 1);
        assertTriggeredRules(context, 0, null);

        EvaluatedPolicyRule evaluatedPolicyRule = evaluatedRules.get(0);
        Collection<PolicyExceptionType> exceptions = evaluatedPolicyRule.getPolicyExceptions();
        assertEquals("Wrong number of exceptions", 1, exceptions.size());
        PolicyExceptionType policyException = exceptions.iterator().next();
        assertEquals("Wrong rule name in poliy excpetion", ROLE_JUDGE_POLICY_RULE_EXCLUSION_NAME, policyException.getRuleName());        
	}
	
	@Test
    public void test120AssignRoleConstableToJack() throws Exception {
		final String TEST_NAME = "test120AssignRoleConstableToJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        LensContext<UserType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_CONSTABLE_OID);

        display("Input context", context);

        assertFocusModificationSanity(context);
        rememberShadowFetchOperationCount();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        projector.project(context, "test", task, result);
        
        // THEN        
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        display("Output context", context);
        
        DeltaSetTriple<EvaluatedAssignmentImpl<UserType>> evaluatedAssignmentTriple = 
        		(DeltaSetTriple)context.getEvaluatedAssignmentTriple();
//        display("Output evaluatedAssignmentTriple", evaluatedAssignmentTriple);
        
        dumpPolicyRules(context);
        
        assertEvaluatedRules(context, 2);
		EvaluatedExclusionTrigger trigger = (EvaluatedExclusionTrigger) assertTriggeredRule(context, PolicyConstraintKindType.EXCLUSION, false);
        assertNotNull("No conflicting assignment in trigger", trigger.getConflictingAssignment());
        assertEquals("Wrong conflicting assignment in trigger", ROLE_JUDGE_OID, trigger.getConflictingAssignment().getTarget().getOid());
        
        ObjectDelta<UserType> focusSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        PrismAsserts.assertIsModify(focusSecondaryDelta);
        PrismAsserts.assertModifications(focusSecondaryDelta, 2);
        ContainerDelta<AssignmentType> assignmentDelta = focusSecondaryDelta.findContainerDelta(FocusType.F_ASSIGNMENT);
        assertEquals("Unexpected assignment secondary delta", 1, assignmentDelta.getValuesToDelete().size());
        PrismContainerValue<AssignmentType> deletedAssignment = assignmentDelta.getValuesToDelete().iterator().next();
        assertEquals("Wrong OID in deleted assignment", ROLE_JUDGE_OID, deletedAssignment.asContainerable().getTargetRef().getOid());
        
        ObjectDelta<ShadowType> accountSecondaryDelta = assertAssignAccountToJack(context);
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
    		  getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME),
    		  "Constable");
        PrismAsserts.assertPropertyDelete(accountSecondaryDelta, 
      		  getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME),
      		  "Honorable Justice");
	}

	/**
	 * Thief has got separated constraints and actions.
	 */
	@Test
	public void test150AssignRoleThiefToJack() throws Exception {
		final String TEST_NAME = "test150AssignRoleThiefToJack";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserAccountContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_THIEF_OID);

		display("Input context", context);

		assertFocusModificationSanity(context);
		rememberShadowFetchOperationCount();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		projector.project(context, "test", task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		DeltaSetTriple<EvaluatedAssignmentImpl<UserType>> evaluatedAssignmentTriple =
				(DeltaSetTriple)context.getEvaluatedAssignmentTriple();
		display("Output evaluatedAssignmentTriple", evaluatedAssignmentTriple);

		dumpPolicyRules(context);

		assertEvaluatedRules(context, 3);
		EvaluatedExclusionTrigger triggerExclusion = (EvaluatedExclusionTrigger) assertTriggeredRule(context, PolicyConstraintKindType.EXCLUSION, true);
		assertNotNull("No conflicting assignment in trigger", triggerExclusion.getConflictingAssignment());
		assertEquals("Wrong conflicting assignment in trigger", ROLE_JUDGE_OID, triggerExclusion.getConflictingAssignment().getTarget().getOid());

		EvaluatedSituationTrigger triggerSituation = (EvaluatedSituationTrigger) assertTriggeredRule(context, PolicyConstraintKindType.SITUATION, true);
		assertEquals("Wrong # of source rules", 1, triggerSituation.getSourceRules().size());
		EvaluatedPolicyRule sourceRule = triggerSituation.getSourceRules().iterator().next();
		EvaluatedExclusionTrigger sourceTrigger = (EvaluatedExclusionTrigger) sourceRule.getTriggers().iterator().next();
		assertNotNull("No conflicting assignment in source trigger", sourceTrigger.getConflictingAssignment());
		assertEquals("Wrong conflicting assignment in source trigger", ROLE_JUDGE_OID, sourceTrigger.getConflictingAssignment().getTarget().getOid());
	}


	private List<EvaluatedPolicyRule> assertEvaluatedRules(LensContext<UserType> context, int expected) {
		List<EvaluatedPolicyRule> rules = new ArrayList<>();
		forEvaluatedRule(context, rule -> rules.add(rule));
		assertEquals("Unexpected number of evaluated policy rules in the context", expected, rules.size());
		return rules;
	}
	
	private void assertTriggeredRules(LensContext<UserType> context, int expected, PolicyConstraintKindType expectedConstraintKind) {
		List<EvaluatedPolicyRuleTrigger> triggers = new ArrayList<>();
		forTriggeredRule(context, trigger -> {
			display("Triggered rule", trigger);
			triggers.add(trigger);
			if (expectedConstraintKind != null) {
				assertEquals("Wrong trigger constraint type in "+trigger, expectedConstraintKind, trigger.getConstraintKind());
			}
		});
		assertEquals("Unexpected number of triggered policy rules in the context", expected, triggers.size());
	}

	// filter=true : we filter out unwanted triggers instead of reporting them
	private EvaluatedPolicyRuleTrigger assertTriggeredRule(LensContext<UserType> context, PolicyConstraintKindType expectedConstraintKind, boolean filter) {
		List<EvaluatedPolicyRuleTrigger> triggers = new ArrayList<>();
		forTriggeredRule(context, trigger -> {
			if (filter && trigger.getConstraintKind() != expectedConstraintKind) {
				return;
			}
			display("Triggered rule", trigger);
			triggers.add(trigger);
			if (expectedConstraintKind != null) {
				assertEquals("Wrong trigger constraint type in "+trigger, expectedConstraintKind, trigger.getConstraintKind());
			}
		});
		assertEquals("Unexpected number of triggered policy rules in the context", 1, triggers.size());
		return triggers.get(0);
	}
	
	private void forTriggeredRule(LensContext<UserType> context, Consumer<EvaluatedPolicyRuleTrigger> handler) {
		forEvaluatedRule(context, rule -> {
			Collection<EvaluatedPolicyRuleTrigger> triggers = rule.getTriggers();
    		for (EvaluatedPolicyRuleTrigger trigger: triggers) {
    			handler.accept(trigger);
    		}
		});
	}
	
	private void forEvaluatedRule(LensContext<UserType> context, Consumer<EvaluatedPolicyRule> handler) {
		DeltaSetTriple<EvaluatedAssignmentImpl<UserType>> evaluatedAssignmentTriple = 
        		(DeltaSetTriple)context.getEvaluatedAssignmentTriple();
        evaluatedAssignmentTriple.accept(assignment -> {
        	Collection<EvaluatedPolicyRule> targetPolicyRules = assignment.getTargetPolicyRules();
        	for (EvaluatedPolicyRule evaluatedPolicyRule: targetPolicyRules) {
        		handler.accept(evaluatedPolicyRule);
        	}
        });
	}

	// TODO move to main code?
	private void dumpPolicyRules(LensContext<UserType> context) {
		StringBuilder sb = new StringBuilder();
		DeltaSetTriple<EvaluatedAssignmentImpl<UserType>> evaluatedAssignmentTriple = 
        		(DeltaSetTriple)context.getEvaluatedAssignmentTriple();
		evaluatedAssignmentTriple.debugDumpSets(sb, assignment -> {
			DebugUtil.indentDebugDump(sb, 3);
			sb.append(assignment.toHumanReadableString());
        	Collection<EvaluatedPolicyRule> targetPolicyRules = assignment.getTargetPolicyRules();
        	for (EvaluatedPolicyRule rule: targetPolicyRules) {
        		sb.append("\n");
        		DebugUtil.indentDebugDump(sb, 4);
        		sb.append("rule: ").append(rule.getName());
        		for (EvaluatedPolicyRuleTrigger trigger: rule.getTriggers()) {
        			sb.append("\n");
        			DebugUtil.indentDebugDump(sb, 5);
        			sb.append("trigger: ").append(trigger);
        			if (trigger instanceof EvaluatedExclusionTrigger && ((EvaluatedExclusionTrigger) trigger).getConflictingAssignment() != null) {
        				sb.append("\n");
            			DebugUtil.indentDebugDump(sb, 6);
            			sb.append("conflict: ").append(((EvaluatedAssignmentImpl)((EvaluatedExclusionTrigger) trigger).getConflictingAssignment()).toHumanReadableString());
        			}
        		}
        		for (PolicyExceptionType exc: rule.getPolicyExceptions()) {
        			sb.append("\n");
        			DebugUtil.indentDebugDump(sb, 5);
        			sb.append("exception: ").append(exc);
        		}
        	}
        }, 1);
		display("Policy rules", sb.toString());
	}

	private ObjectDelta<ShadowType> assertAssignAccountToJack(LensContext<UserType> context) {
        display("Output context", context);
        
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());
        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull("Account primary delta sneaked in", accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        
        assertEquals("Wrong decision", SynchronizationPolicyDecision.KEEP,accContext.getSynchronizationPolicyDecision());
        
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        
        return accountSecondaryDelta;

	}


}
