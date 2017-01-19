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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPolicyRules extends AbstractLensTest {
		
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		setDefaultUserTemplate(USER_TEMPLATE_OID);
		
		addObject(ROLE_PIRATE_FILE);
		addObject(ROLE_MUTINIER_FILE);
		addObject(ROLE_JUDGE_FILE);
		
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
        projector.project(context, "test", task, result);
        
        // THEN
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
        projector.project(context, "test", task, result);
        
        // THEN
        assertAssignAccountToJack(context);
        
        DeltaSetTriple<EvaluatedAssignmentImpl<UserType>> evaluatedAssignmentTriple = 
        		(DeltaSetTriple)context.getEvaluatedAssignmentTriple();
//        display("Output evaluatedAssignmentTriple", evaluatedAssignmentTriple);
        
        dumpPolicyRules(context);
        
        assertEvaluatedRules(context, 1);
        EvaluatedPolicyRuleTrigger trigger = assertTriggeredRule(context, PolicyConstraintKindType.EXCLUSION);
        assertNotNull("No conflicting assignment in trigger", trigger.getConflictingAssignment());
        assertEquals("Wrong conflicting assignment in trigger", ROLE_PIRATE_OID, trigger.getConflictingAssignment().getTarget().getOid());
	}
	
	private void assertEvaluatedRules(LensContext<UserType> context, int expected) {
		List<EvaluatedPolicyRule> rules = new ArrayList<>();
		forEvaluatedRule(context, rule -> rules.add(rule));
		assertEquals("Unexpected number of evaluated policy rules in the context", expected, rules.size());
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
	
	private EvaluatedPolicyRuleTrigger assertTriggeredRule(LensContext<UserType> context, PolicyConstraintKindType expectedConstraintKind) {
		List<EvaluatedPolicyRuleTrigger> triggers = new ArrayList<>();
		forTriggeredRule(context, trigger -> {
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
        			if (trigger.getConflictingAssignment() != null) {
        				sb.append("\n");
            			DebugUtil.indentDebugDump(sb, 6);
            			sb.append("conflict: ").append(((EvaluatedAssignmentImpl)trigger.getConflictingAssignment()).toHumanReadableString());
        			}
        		}
        	}
        }, 1);
		display("Policy rules", sb.toString());
	}

	private void assertAssignAccountToJack(LensContext<UserType> context) {
        display("Output context", context);
        // Not loading anything. The account is already loaded in the context
        assertShadowFetchOperationCountIncrement(0);
        
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta", ActivationStatusType.ENABLED);
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull("Account primary delta sneaked in", accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        
        assertEquals("Wrong decision", SynchronizationPolicyDecision.ADD,accContext.getSynchronizationPolicyDecision());
        
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, getIcfsNameAttributePath() , "jack");
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, getDummyResourceController().getAttributeFullnamePath() , "Jack Sparrow");
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
        		getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME) , "mouth", "pistol");

	}

	
}
