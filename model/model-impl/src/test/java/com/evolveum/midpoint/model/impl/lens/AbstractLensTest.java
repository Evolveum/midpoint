/*
 * Copyright (c) 2013-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.controller.ModelController;
import com.evolveum.midpoint.model.impl.lens.construction.ResourceObjectConstruction;

import com.evolveum.midpoint.prism.delta.ObjectDelta;

import com.evolveum.midpoint.util.exception.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractLensTest extends AbstractInternalModelIntegrationTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "lens");

    static final File ASSIGNMENT_DIRECT_FILE = new File(TEST_DIR, "assignment-direct.xml");
    static final File ASSIGNMENT_DIRECT_EXPRESSION_FILE = new File(TEST_DIR, "assignment-direct-expression.xml");
    static final File ASSIGNMENT_ROLE_ENGINEER_FILE = new File(TEST_DIR, "assignment-role-engineer.xml");
    static final File ASSIGNMENT_ROLE_MANAGER_FILE = new File(TEST_DIR, "assignment-role-manager.xml");
    static final File ASSIGNMENT_ROLE_VISITOR_FILE = new File(TEST_DIR, "assignment-role-visitor.xml");

    protected static final File USER_DRAKE_FILE = new File(TEST_DIR, "user-drake.xml");
    protected static final String USER_DRAKE_OID = "c0c010c0-d34d-b33f-f00d-888888888888";

    static final File REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY = new File(TEST_DIR,
            "user-jack-modify-add-assignment-account-dummy.xml");

    static final File REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY_ATTR = new File(TEST_DIR,
            "user-jack-modify-add-assignment-account-dummy-attr.xml");

    static final File REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE_ENGINEER = new File(TEST_DIR,
            "user-jack-modify-add-assignment-role-engineer.xml");

    static final File REQ_USER_JACK_MODIFY_SET_COST_CENTER = new File(TEST_DIR,
            "user-jack-modify-set-cost-center.xml");

    static final File REQ_USER_JACK_MODIFY_DELETE_ASSIGNMENT_ACCOUNT_DUMMY = new File(TEST_DIR,
            "user-jack-modify-delete-assignment-account-dummy.xml");

    static final File REQ_USER_BARBOSSA_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY_ATTR = new File(TEST_DIR,
            "user-barbossa-modify-add-assignment-account-dummy-attr.xml");

    static final File REQ_USER_BARBOSSA_MODIFY_DELETE_ASSIGNMENT_ACCOUNT_DUMMY_ATTR = new File(TEST_DIR,
            "user-barbossa-modify-delete-assignment-account-dummy-attr.xml");

    protected static final File ROLE_PIRATE_FILE = new File(TEST_DIR, "role-pirate.xml");
    static final File ROLE_PIRATE_RECORD_ONLY_FILE = new File(TEST_DIR, "role-pirate-record-only.xml");
    protected static final String ROLE_PIRATE_OID = "12345678-d34d-b33f-f00d-555555556666";

    static final File ROLE_MUTINEER_FILE = new File(TEST_DIR, "role-mutineer.xml");
    static final String ROLE_MUTINEER_OID = "12345678-d34d-b33f-f00d-555555556668";

    static final File ROLE_JUDGE_FILE = new File(TEST_DIR, "role-judge.xml");
    static final File ROLE_JUDGE_RECORD_ONLY_FILE = new File(TEST_DIR, "role-judge-record-only.xml");
    static final String ROLE_JUDGE_OID = "12345111-1111-2222-1111-121212111111";

    static final File ROLE_CONSTABLE_FILE = new File(TEST_DIR, "role-constable.xml");
    static final String ROLE_CONSTABLE_OID = "16ac2572-de66-11e6-bc86-23e62333976a";

    static final File ROLE_THIEF_FILE = new File(TEST_DIR, "role-thief.xml");
    static final String ROLE_THIEF_OID = "5ad00bd6-c550-466f-b15e-4d5fb195b369";

    static final File ROLE_METAROLE_SOD_NOTIFICATION_FILE = new File(TEST_DIR, "role-metarole-sod-notification.xml");
    @SuppressWarnings("unused") // Useful when searching by OID, so not deleting it (also others like this one)
    protected static final String ROLE_METAROLE_SOD_NOTIFICATION_OID = "f8f217f2-b864-416b-bce6-90c85385e43e";

    static final File ROLE_CORP_CONTRACTOR_FILE = new File(TEST_DIR, "role-corp-contractor.xml");
    static final String ROLE_CORP_CONTRACTOR_OID = "12345678-d34d-b33f-f00d-55555555a004";

    static final File ROLE_CORP_CUSTOMER_FILE = new File(TEST_DIR, "role-corp-customer.xml");
    @SuppressWarnings("unused")
    protected static final String ROLE_CORP_CUSTOMER_OID = "12345678-d34d-b33f-f00d-55555555a006";

    static final File ROLE_CORP_EMPLOYEE_FILE = new File(TEST_DIR, "role-corp-employee.xml");
    static final String ROLE_CORP_EMPLOYEE_OID = "12345678-d34d-b33f-f00d-55555555a001";

    static final File ROLE_CORP_ENGINEER_FILE = new File(TEST_DIR, "role-corp-engineer.xml");
    static final String ROLE_CORP_ENGINEER_OID = "12345678-d34d-b33f-f00d-55555555a002";

    static final File ROLE_CORP_MANAGER_FILE = new File(TEST_DIR, "role-corp-manager.xml");
    @SuppressWarnings("unused")
    protected static final String ROLE_CORP_MANAGER_OID = "12345678-d34d-b33f-f00d-55555555a003";

    static final File ROLE_CORP_VISITOR_FILE = new File(TEST_DIR, "role-corp-visitor.xml");
    static final String ROLE_CORP_VISITOR_OID = "12345678-d34d-b33f-f00d-55555555a005";

    static final File ROLE_CORP_GENERIC_METAROLE_FILE = new File(TEST_DIR, "role-corp-generic-metarole.xml");
    @SuppressWarnings("unused")
    protected static final String ROLE_CORP_GENERIC_METAROLE_OID = "12345678-d34d-b33f-f00d-55555555a020";

    static final File ROLE_CORP_JOB_METAROLE_FILE = new File(TEST_DIR, "role-corp-job-metarole.xml");
    @SuppressWarnings("unused")
    protected static final String ROLE_CORP_JOB_METAROLE_OID = "12345678-d34d-b33f-f00d-55555555a010";

    static final File ROLE_CORP_AUTH_FILE = new File(TEST_DIR, "role-corp-auth.xml");
    @SuppressWarnings("unused")
    protected static final String ROLE_CORP_AUTH_OID = "12345678-d34d-b33f-f00d-55555555aaaa";

    static final TestResource<UserType> USER_LOCALIZED =
            new TestResource<>(TEST_DIR, "user-localized.xml", "c46f4b09-2200-4977-88bc-da1f3ffd0b42");
    static final TestResource<RoleType> ROLE_LOCALIZED =
            new TestResource<>(TEST_DIR, "role-localized.xml", "25294519-5e0e-44d4-bebc-ea549d850ed9");

    static final File[] ROLE_CORP_FILES = {
            ROLE_METAROLE_SOD_NOTIFICATION_FILE,
            ROLE_CORP_AUTH_FILE,
            ROLE_CORP_GENERIC_METAROLE_FILE,
            ROLE_CORP_JOB_METAROLE_FILE,
            ROLE_CORP_VISITOR_FILE,
            ROLE_CORP_CUSTOMER_FILE,
            ROLE_CORP_CONTRACTOR_FILE,
            ROLE_CORP_EMPLOYEE_FILE,
            ROLE_CORP_ENGINEER_FILE,
            ROLE_CORP_MANAGER_FILE
    };

    static final File ORG_BRETHREN_FILE = new File(TEST_DIR, "org-brethren.xml");
    static final String ORG_BRETHREN_OID = "9c6bfc9a-ca01-11e3-a5aa-001e8c717e5b";
    static final String ORG_BRETHREN_INDUCED_ORGANIZATION = "Pirate Brethren";

    static final File TEMPLATE_DYNAMIC_ORG_ASSIGNMENT_FILE = new File(TEST_DIR, "template-dynamic-org-assignment.xml");
    static final String TEMPLATE_DYNAMIC_ORG_ASSIGNMENT_OID = "ee079df8-1146-4e53-872f-b9733f24ebfe";
    static final String DYNAMIC_ORG_ASSIGNMENT_SUBTYPE = "dynamicOrgAssignment";

    @Autowired private ContextFactory contextFactory;
    @Autowired protected Projector projector;
    @Autowired protected Clockwork clockwork;
    @Autowired protected TaskManager taskManager;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        //enable if really needed
        // repoAddObjectFromFile(USER_DRAKE_FILE, initResult);
    }

    AssignmentType getAssignmentType(File assignmentFile)
            throws java.io.IOException, SchemaException {
        AssignmentType assignmentType = unmarshalValueFromFile(assignmentFile);

        // We need to make sure that the assignment has a parent
        PrismContainerDefinition<AssignmentType> assignmentContainerDefinition =
                userTypeJack.asPrismObject().getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
        PrismContainer<AssignmentType> assignmentContainer = assignmentContainerDefinition.instantiate();
        //noinspection unchecked
        assignmentContainer.add(assignmentType.asPrismContainerValue().clone());
        return assignmentType;
    }

    List<EvaluatedPolicyRule> assertEvaluatedTargetPolicyRules(LensContext<? extends FocusType> context, int expected) {
        display("Asserting target policy rules (expected " + expected + ")");
        List<EvaluatedPolicyRule> rules = new ArrayList<>();
        forEvaluatedTargetPolicyRule(context, null, rules::add);
        assertEquals("Unexpected number of evaluated target policy rules in the context", expected, rules.size());
        return rules;
    }

    @SuppressWarnings("UnusedReturnValue")
    List<EvaluatedPolicyRule> assertEvaluatedFocusPolicyRules(LensContext<? extends FocusType> context, int expected) {
        display("Asserting focus policy rules (expected " + expected + ")");
        List<EvaluatedPolicyRule> rules = new ArrayList<>();
        forEvaluatedFocusPolicyRule(context, rules::add);
        assertEquals("Unexpected number of evaluated focus policy rules in the context", expected, rules.size());
        return rules;
    }

    void assertTargetTriggers(LensContext<? extends FocusType> context, PolicyConstraintKindType selectedConstraintKind, int expectedCount) {
        display("Asserting target triggers for selected constraint kind = " + selectedConstraintKind + ", expected count = " + expectedCount);
        List<EvaluatedPolicyRuleTrigger<?>> triggers = new ArrayList<>();
        forTriggeredTargetPolicyRule(context, null, trigger -> {
            if (selectedConstraintKind != null && trigger.getConstraintKind() != selectedConstraintKind) {
                return;
            }
            displayDumpable("Selected trigger", trigger);
            triggers.add(trigger);
        });
        assertEquals("Unexpected number of triggers (" + selectedConstraintKind + ") in the context", expectedCount, triggers.size());
    }

    void assertTargetTriggers(LensContext<? extends FocusType> context, PolicyConstraintKindType selectedConstraintKind, String... expectedConstraintNames) {
        List<String> expectedNamesList = Arrays.asList(expectedConstraintNames);
        display("Asserting target triggers for selected constraint kind = " + selectedConstraintKind + ", expected names = " + expectedNamesList);
        List<EvaluatedPolicyRuleTrigger<?>> triggersFound = new ArrayList<>();
        Set<String> namesFound = new HashSet<>();
        forTriggeredTargetPolicyRule(context, null, trigger -> {
            if (selectedConstraintKind != null && trigger.getConstraintKind() != selectedConstraintKind) {
                return;
            }
            displayDumpable("Selected trigger", trigger);
            triggersFound.add(trigger);
            namesFound.add(trigger.getConstraint().getName());
        });
        assertEquals("Unexpected number of triggers (" + selectedConstraintKind + ") in the context", expectedConstraintNames.length, triggersFound.size());
        assertEquals("Unexpected constraint names", new HashSet<>(expectedNamesList), namesFound);
    }

    void assertFocusTriggers(LensContext<? extends FocusType> context, PolicyConstraintKindType selectedConstraintKind, int expectedCount) {
        List<EvaluatedPolicyRuleTrigger<?>> triggers = new ArrayList<>();
        display("Asserting focus triggers for selected constraint kind = " + selectedConstraintKind + ", expected count = " + expectedCount);
        forTriggeredFocusPolicyRule(context, trigger -> {
            if (selectedConstraintKind != null && trigger.getConstraintKind() != selectedConstraintKind) {
                return;
            }
            displayDumpable("Selected trigger", trigger);
            triggers.add(trigger);
        });
        assertEquals("Unexpected number of focus triggers (" + selectedConstraintKind + ") in the context", expectedCount, triggers.size());
    }

    void assertFocusTriggers(LensContext<? extends FocusType> context, PolicyConstraintKindType selectedConstraintKind, String... expectedConstraintNames) {
        List<String> expectedNamesList = Arrays.asList(expectedConstraintNames);
        display("Asserting focus triggers for selected constraint kind = " + selectedConstraintKind + ", expected names = " + expectedNamesList);
        List<EvaluatedPolicyRuleTrigger<?>> triggersFound = new ArrayList<>();
        Set<String> namesFound = new HashSet<>();
        forTriggeredFocusPolicyRule(context, trigger -> {
            if (selectedConstraintKind != null && trigger.getConstraintKind() != selectedConstraintKind) {
                return;
            }
            displayDumpable("Selected trigger", trigger);
            triggersFound.add(trigger);
            namesFound.add(trigger.getConstraint().getName());
        });
        assertEquals("Unexpected number of triggers (" + selectedConstraintKind + ") in the context", expectedConstraintNames.length, triggersFound.size());
        assertEquals("Unexpected constraint names", new HashSet<>(expectedNamesList), namesFound);
    }

    // exclusive=true : there can be no other triggers than 'expectedCount' of 'expectedConstraintKind'
    @SuppressWarnings("SameParameterValue")
    EvaluatedPolicyRuleTrigger<?> assertTriggeredTargetPolicyRule(
            LensContext<? extends FocusType> context,
            String targetOid,
            PolicyConstraintKindType expectedConstraintKind,
            int expectedCount,
            boolean exclusive) {
        List<EvaluatedPolicyRuleTrigger<?>> triggers = new ArrayList<>();
        forTriggeredTargetPolicyRule(context, targetOid, trigger -> {
            if (!exclusive && trigger.getConstraintKind() != expectedConstraintKind) {
                return;
            }
            displayDumpable("Triggered rule", trigger);
            triggers.add(trigger);
            if (expectedConstraintKind != null) {
                assertEquals("Wrong trigger constraint type in " + trigger, expectedConstraintKind, trigger.getConstraintKind());
            }
        });
        assertEquals("Unexpected number of triggered policy rules in the context", expectedCount, triggers.size());
        return triggers.get(0);
    }

    @SuppressWarnings("SameParameterValue")
    EvaluatedPolicyRule getTriggeredTargetPolicyRule(
            LensContext<? extends FocusType> context, String targetOid, PolicyConstraintKindType expectedConstraintKind) {
        List<EvaluatedPolicyRule> rules = new ArrayList<>();
        forEvaluatedTargetPolicyRule(context, targetOid, rule -> {
            if (rule.getTriggers().stream().anyMatch(t -> t.getConstraintKind() == expectedConstraintKind)) {
                rules.add(rule);
            }
        });
        if (rules.size() != 1) {
            fail("Wrong # of triggered rules for " + targetOid + ": expected 1, got " + rules.size() + ": " + rules);
        }
        return rules.get(0);
    }

    @SuppressWarnings("unused")
    protected EvaluatedPolicyRule getTriggeredFocusPolicyRule(LensContext<? extends FocusType> context, PolicyConstraintKindType expectedConstraintKind) {
        List<EvaluatedPolicyRule> rules = new ArrayList<>();
        forEvaluatedFocusPolicyRule(context, rule -> {
            if (rule.getTriggers().stream().anyMatch(t -> t.getConstraintKind() == expectedConstraintKind)) {
                rules.add(rule);
            }
        });
        if (rules.size() != 1) {
            fail("Wrong # of triggered focus rules: expected 1, got " + rules.size() + ": " + rules);
        }
        return rules.get(0);
    }

    private void forTriggeredTargetPolicyRule(
            LensContext<? extends FocusType> context, String targetOid, Consumer<EvaluatedPolicyRuleTrigger<?>> handler) {
        forEvaluatedTargetPolicyRule(context, targetOid, rule -> {
            Collection<EvaluatedPolicyRuleTrigger<?>> triggers = rule.getTriggers();
            for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
                handler.accept(trigger);
            }
        });
    }

    private void forTriggeredFocusPolicyRule(
            LensContext<? extends FocusType> context, Consumer<EvaluatedPolicyRuleTrigger<?>> handler) {
        forEvaluatedFocusPolicyRule(context, rule -> {
            Collection<EvaluatedPolicyRuleTrigger<?>> triggers = rule.getTriggers();
            for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
                handler.accept(trigger);
            }
        });
    }

    private void forEvaluatedTargetPolicyRule(
            LensContext<? extends FocusType> context, String targetOid, Consumer<EvaluatedPolicyRule> handler) {
        //noinspection unchecked,rawtypes
        DeltaSetTriple<EvaluatedAssignmentImpl<? extends FocusType>> evaluatedAssignmentTriple =
                (DeltaSetTriple) context.getEvaluatedAssignmentTriple();
        evaluatedAssignmentTriple.simpleAccept(assignment -> {
            if (targetOid == null || assignment.getTarget() != null && targetOid.equals(assignment.getTarget().getOid())) {
                assignment.getAllTargetsPolicyRules().forEach(handler);
            }
        });
    }

    void forEvaluatedFocusPolicyRule(LensContext<? extends FocusType> context, Consumer<EvaluatedPolicyRule> handler) {
        //noinspection unchecked,rawtypes
        DeltaSetTriple<EvaluatedAssignmentImpl<? extends FocusType>> evaluatedAssignmentTriple =
                (DeltaSetTriple) context.getEvaluatedAssignmentTriple();
        evaluatedAssignmentTriple.simpleAccept(assignment -> assignment.getObjectPolicyRules().forEach(handler));
    }

    void dumpPolicyRules(LensContext<? extends FocusType> context) {
        displayValue("Policy rules", context.dumpAssignmentPolicyRules(3));
    }

    void dumpPolicySituations(LensContext<? extends FocusType> context) {
        LensFocusContext<? extends FocusType> focusContext = context.getFocusContext();
        if (focusContext != null && focusContext.getObjectNew() != null) {
            FocusType focus = focusContext.getObjectNew().asObjectable();
            display("focus policy situation", focus.getPolicySituation());
            for (AssignmentType assignment : focus.getAssignment()) {
                display("assignment policy situation", assignment.getPolicySituation());
            }
        } else {
            display("no focus context or object");
        }
    }

    void assertAssignmentPath(AssignmentPath path, String... targetOids) {
        assertEquals("Wrong path size", targetOids.length, path.size());
        for (int i = 0; i < targetOids.length; i++) {
            ObjectType target = path.getSegments().get(i).getTarget();
            if (targetOids[i] == null) {
                assertNull("Target #" + (i + 1) + " should be null; it is: " + target, target);
            } else {
                assertNotNull("Target #" + (i + 1) + " should not be null", target);
                assertEquals("Wrong OID in target #" + (i + 1), targetOids[i], target.getOid());
            }
        }
    }

    protected String getDescription(ResourceObjectConstruction<?, ?> construction) {
        return construction.getConstructionBean() != null ?
                construction.getConstructionBean().getDescription() : null;
    }

    @SuppressWarnings("SameParameterValue")
    <O extends ObjectType> LensContext<O> runClockwork(
            ObjectDelta<O> delta, ModelExecuteOptions options, Task task, OperationResult result) throws CommonException {
        return runClockwork(List.of(delta), options, task, result);
    }

    /**
     * Just like {@link ModelController#executeChanges(Collection, ModelExecuteOptions, Task, OperationResult)} but
     *
     * . returns the final {@link LensContext};
     * . much simpler (to avoid code duplication), so only basic functionality presented there is available.
     */
    <O extends ObjectType> LensContext<O> runClockwork(
            Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options, Task task, OperationResult result)
            throws CommonException{
        LensContext<O> context = contextFactory.createContext(deltas, options, task, result);
        clockwork.run(context, task, result);
        return context;
    }
}
