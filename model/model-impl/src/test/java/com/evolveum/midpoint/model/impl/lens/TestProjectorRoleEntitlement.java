/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static org.testng.AssertJUnit.*;

import java.util.Collection;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestProjectorRoleEntitlement extends AbstractLensTest {

    @Autowired
    private Projector projector;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(ROLE_PIRATE_FILE);

        // Set user template. This DOES NOT EXIST in the repository.
        // Setting this nonsense is used to check that projector does not even try to use the template.
        setDefaultUserTemplate(USER_TEMPLATE_OID);
    }

    /**
     * Add direct entitlement assignment to role "pirate". The entitlement projection
     * context should appear in the lens context.
     */
    @Test
    public void test100AddEntitlementToPirateDirect() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        LensContext<RoleType> context = createLensContext(RoleType.class);
        fillContextWithFocus(context, RoleType.class, ROLE_PIRATE_OID, result);
        // We want "shadow" so the fullname will be computed by outbound expression
        addModificationToContextAddProjection(context, RoleType.class, ENTITLEMENT_SHADOW_PIRATE_DUMMY_FILE);

        display("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        display("Output context", context);

        assertNull("Unexpected focus primary changes " + context.getFocusContext().getPrimaryDelta(), context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "focus secondary delta", ActivationStatusType.ENABLED);
        assertFalse("No projection contexts", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext projContext = accountContexts.iterator().next();

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.ADD, projContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = projContext.getPrimaryDelta();
        assertEquals(ChangeType.ADD, accountPrimaryDelta.getChangeType());
        PrismObject<ShadowType> accountToAddPrimary = accountPrimaryDelta.getObjectToAdd();
        assertNotNull("No object in projection primary add delta", accountToAddPrimary);
        PrismProperty<Object> intentProperty = accountToAddPrimary.findProperty(ShadowType.F_INTENT);
        assertNotNull("No intent type in projection primary add delta", intentProperty);
        assertEquals("group", intentProperty.getRealValue());
        assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(getDummyResourceType()), "GroupObjectClass"),
                accountToAddPrimary.findProperty(ShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = accountToAddPrimary.findReference(ShadowType.F_RESOURCE_REF);
        assertEquals(getDummyResourceType().getOid(), resourceRef.getOid());
        accountToAddPrimary.checkConsistence();

        ObjectDelta<ShadowType> projSecondaryDelta = projContext.getSecondaryDelta();
        assertEquals(ChangeType.MODIFY, projSecondaryDelta.getChangeType());

        PropertyDelta<String> groupDescriptionDelta = projSecondaryDelta.findPropertyDelta(
                getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        assertNotNull("No group description delta", groupDescriptionDelta);
        PrismAsserts.assertReplace(groupDescriptionDelta, "Bloody pirates");
        PrismAsserts.assertOrigin(groupDescriptionDelta, OriginType.OUTBOUND);

        PrismObject<ShadowType> projectionNew = projContext.getObjectNew();
        IntegrationTestTools.assertIcfsNameAttribute(projectionNew, "pirate");
        IntegrationTestTools.assertAttribute(projectionNew,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION),
                "Bloody pirates");
    }

    @Test
    public void test110AssignEntitlementToPirate() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<RoleType> context = createLensContext(RoleType.class);
        fillContextWithFocus(context, RoleType.class, ROLE_PIRATE_OID, result);
        ObjectDelta<RoleType> roleAssignmentDelta = createAssignmentDelta(RoleType.class,
                ROLE_PIRATE_OID, RESOURCE_DUMMY_OID, ShadowKindType.ENTITLEMENT, "group", true);
        addFocusDeltaToContext(context, roleAssignmentDelta);

        display("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        assertAssignEntitlementToPirate(context);
    }

    /**
     * Same sa previous test but the deltas are slightly broken.
     */
    @Test
    public void test111AssignEntitlementToPirateBroken() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<RoleType> context = createLensContext(RoleType.class);
        fillContextWithFocus(context, RoleType.class, ROLE_PIRATE_OID, result);
        ObjectDelta<RoleType> roleAssignmentDelta = createAssignmentDelta(RoleType.class,
                ROLE_PIRATE_OID, RESOURCE_DUMMY_OID, ShadowKindType.ENTITLEMENT, "group", true);
        addFocusDeltaToContext(context, roleAssignmentDelta);

        display("Input context", context);

        assertFocusModificationSanity(context);

        // Let's break it a bit...
        breakAssignmentDelta(context);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        assertAssignEntitlementToPirate(context);
    }

    private void assertAssignEntitlementToPirate(LensContext<RoleType> context) {
        display("Output context", context);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(),
                "focus secondary delta", ActivationStatusType.ENABLED);
        assertFalse("No projection changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> projectionContexts = context.getProjectionContexts();
        assertEquals(1, projectionContexts.size());
        LensProjectionContext projContext = projectionContexts.iterator().next();
        assertNull("Projection primary delta sneaked in", projContext.getPrimaryDelta());

        ObjectDelta<ShadowType> projSecondaryDelta = projContext.getSecondaryDelta();

        assertEquals("Wrong decision", SynchronizationPolicyDecision.ADD, projContext.getSynchronizationPolicyDecision());

        assertEquals(ChangeType.MODIFY, projSecondaryDelta.getChangeType());

        PrismAsserts.assertPropertyReplace(projSecondaryDelta, getIcfsNameAttributePath(), "Pirate");
        PrismAsserts.assertPropertyReplace(projSecondaryDelta,
                getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION),
                "Bloody pirates");
        PrismAsserts.assertOrigin(projSecondaryDelta, OriginType.OUTBOUND);

    }

}
