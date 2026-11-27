/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.common.expression.evaluator.caching.SimulatedCreateOnDemandRegistry;
import com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * `midpoint.getParentOrgs()` called on an org whose parent org is "created on demand" in preview mode.
 *
 * In 4.8, `previewChanges` sets `simulation/createOnDemand = safe`, so the on-demand org is not stored and has no OID.
 * The `parentOrgRef` of the focus then looks like `oid=null(OrgType)('name')` and `getParentOrgs` calls
 * `getObject(OrgType.class, null, ...)`.
 *
 * See https://support.evolveum.com/notifications/details/10243
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestPreviewChangesCoDParentOrgs extends AbstractConfiguredModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/simulation/cod-parent-orgs");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<ObjectTemplateType> OBJECT_TEMPLATE_ORG =
            TestObject.file(TEST_DIR, "object-template-org.xml", "b5c3b7f2-5a8c-4a2e-9b0f-6a0f2b8c3e01");

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        addObject(OBJECT_TEMPLATE_ORG, initTask, initResult);
    }

    /**
     * Baseline: real execution. The parent org "r-mid" is created on demand (with an OID), `getParentOrgs` works.
     */
    @Test
    public void test100RealExecutionOrgWithCoDParent() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("org is added (real execution)");
        addObject(createOrg("r-leaf", "r-mid").asPrismObject(), task, result);

        then("everything is OK, parent org was created");
        assertSuccess(result);
        assertThat(findObjectByName(OrgType.class, "r-mid")).as("org r-mid").isNotNull();
        PrismObject<OrgType> org = findObjectByName(OrgType.class, "r-leaf");
        assertThat(org.asObjectable().getCostCenter()).isEqualTo("parent:r-mid");
    }

    /**
     * Preview of adding an org whose parent org ("a-mid") is created on demand.
     * The org template calls `midpoint.getParentOrgs(focus)` on the org itself.
     */
    @Test
    public void test110PreviewOrgWithCoDParent() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("org ADD delta");
        ObjectDelta<OrgType> delta = createOrg("a-leaf", "a-mid").asPrismObject().createAddDelta();

        when("previewChanges is invoked");
        ModelContext<OrgType> context =
                modelInteractionService.previewChanges(List.of(delta), ModelExecuteOptions.create(), task, result);

        then("everything is OK and nothing was created");
        displayDumpable("context", context);
        assertSuccess(result);
        assertThat(findObjectByName(OrgType.class, "a-mid")).as("org a-mid").isNull();

        PrismObject<OrgType> orgNew = context.getFocusContext().getObjectNew();
        display("org new", orgNew);
        assertThat(orgNew.asObjectable().getCostCenter()).isEqualTo("parent:a-mid");
    }

    /**
     * Control: preview of adding an org whose parent org ("e-mid") already exists in the repository (no CoD).
     */
    @Test
    public void test120PreviewOrgWithExistingParent() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("existing parent org");
        addObject(new OrgType().name("e-mid").identifier("e-mid").asPrismObject(), task, result);

        and("org ADD delta");
        ObjectDelta<OrgType> delta = createOrg("e-leaf", "e-mid").asPrismObject().createAddDelta();

        when("previewChanges is invoked");
        ModelContext<OrgType> context =
                modelInteractionService.previewChanges(List.of(delta), ModelExecuteOptions.create(), task, result);

        then("everything is OK");
        assertSuccess(result);
        PrismObject<OrgType> orgNew = context.getFocusContext().getObjectNew();
        display("org new", orgNew);
        assertThat(orgNew.asObjectable().getParentOrgRef()).as("parentOrgRef").hasSize(1);
        assertThat(orgNew.asObjectable().getCostCenter()).isEqualTo("parent:e-mid");
    }

    /**
     * The on-demand parent org must appear in the preview just once: one assignment, one parent org reference.
     */
    @Test
    public void test130PreviewHasSingleCopyOfOnDemandParent() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("preview of an org with an on-demand parent");
        PrismObject<OrgType> orgNew = previewOrg(createOrg("c-leaf", "c-mid"), task, result);

        then("there is exactly one assignment of the parent and one parent org reference");
        assertSuccess(result);
        assertThat(orgNew.asObjectable().getAssignment()).as("assignments").hasSize(1);
        assertThat(orgNew.asObjectable().getParentOrgRef()).as("parentOrgRef").hasSize(1);
    }

    /**
     * Parents of parents created on demand are visible in the preview: the whole chain of ancestors is found.
     */
    @Test
    public void test140PreviewOfThreeLevelsOfOnDemandOrgs() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("preview of an org whose parent and grandparent are created on demand");
        PrismObject<OrgType> orgNew = previewOrg(createOrg("d-leaf", "d-top:d-mid"), task, result);

        then("the direct parent is found, and so are all the ancestors");
        assertSuccess(result);
        assertThat(orgNew.asObjectable().getCostCenter()).isEqualTo("parent:d-mid");
        assertThat(orgNew.asObjectable().getDisplayName()).isNotNull();
        assertThat(orgNew.asObjectable().getDisplayName().getOrig()).isEqualTo("ancestors:d-mid,d-top");
    }

    /**
     * A failed preview must not leave anything behind. Objects simulated by one operation must never be seen by
     * another operation that runs later on the same thread.
     */
    @Test
    public void test150FailedPreviewLeavesNoSimulatedObjectsBehind() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assertThat(SimulatedCreateOnDemandRegistry.get()).as("registry before").isNull();

        when("preview fails already in the initial checks (archetypeRef without the archetype assignment)");
        OrgType org = createOrg("e2-leaf", "e2-mid");
        org.getArchetypeRef().add(new ObjectReferenceType()
                .oid(UUID.randomUUID().toString())
                .type(ArchetypeType.COMPLEX_TYPE));
        try {
            modelInteractionService.previewChanges(
                    List.of(org.asPrismObject().createAddDelta()), ModelExecuteOptions.create(), task, result);
        } catch (Exception e) {
            displayExpectedException(e);
        }

        then("nothing is left on the thread");
        assertThat(SimulatedCreateOnDemandRegistry.get()).as("registry after the failed preview").isNull();
    }

    /**
     * Two previews of the same org are independent: the second one does not reuse the parent simulated by the first.
     */
    @Test
    public void test160TwoPreviewsAreIndependent() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("the same org is previewed twice");
        PrismObject<OrgType> first = previewOrg(createOrg("f-leaf", "f-mid"), task, result);
        PrismObject<OrgType> second = previewOrg(createOrg("f-leaf", "f-mid"), task, result);

        then("each preview has its own single simulated parent");
        assertSuccess(result);
        assertThat(first.asObjectable().getAssignment()).hasSize(1);
        assertThat(second.asObjectable().getAssignment()).hasSize(1);
        assertThat(first.asObjectable().getAssignment().get(0).getTargetRef().getOid())
                .isNotEqualTo(second.asObjectable().getAssignment().get(0).getTargetRef().getOid());
    }

    /**
     * Real execution still stores the on-demand parent, exactly once, and the org has a single assignment of it.
     */
    @Test
    public void test170RealExecutionStoresOnDemandParentOnce() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("an org with an on-demand parent is added (real execution)");
        addObject(createOrg("g-leaf", "g-mid").asPrismObject(), task, result);

        then("the parent exists exactly once and the org refers to it with one assignment");
        assertSuccess(result);
        int parents = repositoryService.countObjects(
                OrgType.class,
                prismContext.queryFor(OrgType.class).item(OrgType.F_NAME).eq("g-mid").matchingOrig().build(),
                null, result);
        assertThat(parents).as("number of orgs g-mid").isEqualTo(1);
        PrismObject<OrgType> leaf = findObjectByName(OrgType.class, "g-leaf");
        assertThat(leaf.asObjectable().getAssignment()).as("assignments").hasSize(1);
    }

    /**
     * The problem is not specific to orgs: a role created on demand is assigned in the preview just once,
     * and the role is really found, so the org is its member.
     */
    @Test
    public void test180PreviewOfRoleCreatedOnDemand() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("preview of an org that gets a role created on demand");
        PrismObject<OrgType> orgNew = previewOrg(
                new OrgType().name("with-role-leaf").identifier("with-role-leaf"), task, result);

        then("the role is assigned exactly once and the org is a member of it");
        assertSuccess(result);
        assertThat(orgNew.asObjectable().getAssignment()).as("assignments").hasSize(1);
        assertThat(orgNew.asObjectable().getRoleMembershipRef()).as("roleMembershipRef").hasSize(1);
    }

    private PrismObject<OrgType> previewOrg(OrgType org, Task task, OperationResult result) throws Exception {
        ModelContext<OrgType> context = modelInteractionService.previewChanges(
                List.of(org.asPrismObject().createAddDelta()), ModelExecuteOptions.create(), task, result);
        return context.getFocusContext().getObjectNew();
    }

    /** Org with given name/identifier; parent org identifier is passed in `description` (see the org template). */
    private OrgType createOrg(String name, String parentIdentifier) {
        return new OrgType()
                .name(name)
                .identifier(name)
                .description(parentIdentifier);
    }
}
