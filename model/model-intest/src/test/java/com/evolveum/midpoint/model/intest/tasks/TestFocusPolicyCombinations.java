/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyUtils;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.work.ActivityDefinitionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyObjectsCreator;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.asserter.TaskAsserter;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestFocusPolicyCombinations extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/policy-combinations");
    private static final File COMMON_DIR = new File("src/test/resources/tasks/common");

    private static final String RESOURCE_SOURCE_OID = "c1a70000-0000-0000-0000-000000000001";

    private static final DummyTestResource RESOURCE_SOURCE = new DummyTestResource(
            COMMON_DIR, "resource-dummy-source.xml", RESOURCE_SOURCE_OID, "fpc-source",
            DummyResourceContoller::populateWithDefaultSchema);

    private static final TestObject<TaskType> FX_COMPOSITE_MT =
            TestObject.file(TEST_DIR, "combo-composite-mt.xml", "e3f00000-0000-0000-0000-000000000001");

    private static final int ACCOUNTS = 20;
    private static final String PATTERN = "a%02d";

    private static final int THRESHOLD = 5;

    private static final long TIMEOUT = 90_000;
    private static final long SLEEP = 1000;

    private static final String DUMMY_POLICY_NOTIFIER = "dummy:comboPolicyNotifier";

    enum Constraint {NONE, ADD}

    record Contribution(PolicyRuleType rule, ActivityPath placement) {
    }

    record Run(String label, List<Contribution> contributions, int importedLo, int importedHi,
               BiConsumer<TaskAsserter<?>, RunContext> assertion) {
    }

    record Combo(String name, TestObject<TaskType> fixture, Constraint precondition, List<Run> runs) {
    }

    record RunContext(Map<String, String> counterIdByRule) {
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(RESOURCE_SOURCE, initTask, initResult);
        DummyObjectsCreator.accounts()
                .withObjectCount(ACCOUNTS)
                .withNamePattern(PATTERN)
                .withController(RESOURCE_SOURCE.controller)
                .execute();

        SimplePolicyRuleNotifierType policyNotifier = new SimplePolicyRuleNotifierType();
        policyNotifier.getTransport().add(DUMMY_POLICY_NOTIFIER);
        EventHandlerType handler = new EventHandlerType();
        handler.getSimplePolicyRuleNotifier().add(policyNotifier);

        modifyObjectAddContainer(
                SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                ItemPath.create(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION, NotificationConfigurationType.F_HANDLER),
                initTask, initResult, handler);
    }

    @BeforeMethod
    public void resetState() throws Exception {
        prepareNotifications();
        OperationResult result = getTestOperationResult();
        for (int i = 0; i < ACCOUNTS; i++) {
            String name = String.format(PATTERN, i);
            List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class,
                    prismContext.queryFor(UserType.class).item(UserType.F_NAME).eqPoly(name).matchingNorm().build(),
                    null, result);
            for (PrismObject<UserType> user : users) {
                repositoryService.deleteObject(UserType.class, user.getOid(), result);
            }
        }
        deleteShadowsOf(RESOURCE_SOURCE_OID, result);
        restoreAccounts(RESOURCE_SOURCE, result);
    }

    private void deleteShadowsOf(String resourceOid, OperationResult result) throws Exception {
        List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class,
                prismContext.queryFor(ShadowType.class).item(ShadowType.F_RESOURCE_REF).ref(resourceOid).build(),
                null, result);
        for (PrismObject<ShadowType> shadow : shadows) {
            repositoryService.deleteObject(ShadowType.class, shadow.getOid(), result);
        }
    }

    private void restoreAccounts(DummyTestResource resource, OperationResult result) throws Exception {
        for (int i = 0; i < ACCOUNTS; i++) {
            String name = String.format(PATTERN, i);
            if (resource.controller.getDummyResource().getAccountByName(name) == null) {
                resource.controller.addAccount(name);
            }
        }
    }

    private PolicyRuleType rule(String name, PolicyConstraintsType constraints, int threshold, PolicyActionsType actions) {
        return new PolicyRuleType()
                .name(name)
                .policyConstraints(constraints)
                .policyThreshold(new PolicyThresholdType().lowWaterMark(new WaterMarkType().count(threshold)))
                .policyActions(actions);
    }

    private PolicyRuleType addRule(String name, int threshold, PolicyActionsType actions) {
        return rule(name, new PolicyConstraintsType()
                .modification(new ModificationPolicyConstraintType().operation(ChangeTypeType.ADD)), threshold, actions);
    }

    private PolicyActionsType suspend() {
        return withNotify().suspendTask(new SuspendTaskPolicyActionType());
    }

    private PolicyActionsType withNotify() {
        return new PolicyActionsType().notification(new NotificationPolicyActionType());
    }

    private Consumer<PrismObject<TaskType>> contribute(List<Contribution> contributions) {
        return taskObj -> contributions.forEach(c -> {
            ActivityDefinitionType def = ActivityDefinitionUtil.findActivityDefinition(
                    taskObj.asObjectable().getActivity(), c.placement());
            assertThat(def).as("activity def at " + c.placement()).isNotNull();
            policies(def).getPolicy().add(c.rule().clone());
        });
    }

    private static ActivityPoliciesType policies(ActivityDefinitionType def) {
        if (def.getPolicies() == null) {
            def.setPolicies(new ActivityPoliciesType());
        }
        return def.getPolicies();
    }

    private String inlineCounterId(TestObject<TaskType> task, Contribution c) throws CommonException {
        return ActivityPolicyUtils.buildPolicyIdentifier(getTask(task.oid), c.placement(), c.rule().getName(), true);
    }

    private int notificationCount(String transport) {
        List<Message> messages = dummyTransport.getMessages(transport);
        return messages == null ? 0 : messages.size();
    }

    private void setupPrecondition(Constraint constraint, OperationResult result) throws Exception {
        switch (constraint) {
            case NONE, ADD -> {}
        }
    }

    private int countImported() throws Exception {
        int count = 0;
        OperationResult result = getTestOperationResult();
        for (int i = 0; i < ACCOUNTS; i++) {
            count += repositoryService.countObjects(UserType.class,
                    prismContext.queryFor(UserType.class).item(UserType.F_NAME)
                            .eqPoly(String.format(PATTERN, i)).matchingNorm().build(), null, result);
        }
        return count;
    }

    // todo is this hack?
    private void waitForRootTermination(String oid, long timeout) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            Task t = taskManager.getTaskWithResult(oid, getTestOperationResult());
            if (t.isClosed() || t.isSuspended()) {
                return;
            }
            Thread.sleep(SLEEP);
        }
        throw new AssertionError("Root task " + oid + " did not terminate within " + timeout + " ms");
    }

    @Test(dataProvider = "combos")
    public void testCombo(Combo combo) throws Exception {
        OperationResult result = getTestOperationResult();
        given(combo.name());

        for (Run run : combo.runs()) {
            resetState();
            setupPrecondition(combo.precondition(), result);

            TestObject<TaskType> task = combo.fixture();
            deleteIfPresent(task, result);

            when(combo.name() + " " + run.label());
            addObject(task, getTestTask(), result, contribute(run.contributions()));

            waitForRootTermination(task.oid, TIMEOUT);

            then(combo.name() + " " + run.label());
            Map<String, String> ids = new HashMap<>();
            for (Contribution c : run.contributions()) {
                ids.put(c.rule().getName(), inlineCounterId(task, c));
            }
            run.assertion().accept(assertTaskTree(task.oid, combo.name() + "/" + run.label()), new RunContext(ids));

            if (run.importedLo() >= 0) {
                assertThat(countImported()).as("imported users for " + combo.name() + "/" + run.label())
                        .isBetween(run.importedLo(), run.importedHi());
            }
        }
    }

    private static Object[] combo(Combo c) {
        return new Object[] { c };
    }

    private Combo single(String name, TestObject<TaskType> fixture, Constraint precondition, Run run) {
        return new Combo(name, fixture, precondition, List.of(run));
    }

    private Run run(String label, List<Contribution> contributions, int importedLo, int importedHi,
            BiConsumer<TaskAsserter<?>, RunContext> assertion) {
        return new Run(label, contributions, importedLo, importedHi, assertion);
    }

    private Contribution at(PolicyRuleType rule, ActivityPath placement) {
        return new Contribution(rule, placement);
    }

    @DataProvider(name = "combos")
    public Object[][] combos() {
        return new Object[][] {

                combo(single("f-add/suspend/parent/mt", FX_COMPOSITE_MT, Constraint.ADD,
                        run("only", List.of(at(addRule("f-add", THRESHOLD, suspend()), ActivityPath.empty())),
                                THRESHOLD - 1, ACCOUNTS - 1,
                                (a, ctx) -> {
                                    // @formatter:off
                                    a.display()
                                            .assertSuspended()
                                            .rootActivityState()
                                                .child("main")
                                                    .fullExecutionModePolicyRulesCounters()
                                                        .assertCounterMinMax(ctx.counterIdByRule().get("f-add"), THRESHOLD, THRESHOLD + 5);
                                    // @formatter:on
                                    assertThat(notificationCount(DUMMY_POLICY_NOTIFIER)).isGreaterThanOrEqualTo(1);
                                }))),
        };
    }
}
