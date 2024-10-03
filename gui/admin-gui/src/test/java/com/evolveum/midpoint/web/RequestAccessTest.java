/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.assertj.core.api.Assertions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.impl.page.self.requestAccess.RequestAccess;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.ShoppingCartItem;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class RequestAccessTest extends AbstractGuiIntegrationTest {

    private static final String POI_1_OID = "90000000-0000-0000-0000-000000000001";

    private static final ObjectReferenceType POI_1 = new ObjectReferenceType().oid(POI_1_OID).type(UserType.COMPLEX_TYPE);

    private static final String ROLE_EXISTING_OID = "90000000-0000-0000-0000-000000000010";
    private static final String ROLE_1_OID = "90000000-0000-0000-0000-000000000011";
    private static final String ROLE_2_OID = "90000000-0000-0000-0000-000000000012";

    private static final ObjectReferenceType ROLE_EXISTING =
            new ObjectReferenceType()
                    .oid(ROLE_EXISTING_OID)
                    .type(RoleType.COMPLEX_TYPE)
                    .relation(SchemaConstants.ORG_DEFAULT);

    @Test
    public void test100SameRelationAndTargetOidAllowed() throws Exception {
        testSameRelationAndTargetOidAllowed(false);
    }

    @Test
    public void test150SameRelationAndTargetOidAllowedExplicitly() throws Exception {
        testSameRelationAndTargetOidAllowed(true);
    }

    private void testSameRelationAndTargetOidAllowed(boolean explicit) throws Exception {
        // updating system configuration
        Boolean allowed = explicit ? Boolean.TRUE : null;
        updateSystemConfiguration(allowed, allowed);

        RequestAccess access = new RequestAccess();
        // select user
        access.addPersonOfInterest(POI_1, List.of(ROLE_EXISTING));

        // try to add assignment with same oid and relation -> should be ok
        addAndAssertAssignment(access, ROLE_EXISTING_OID, SchemaConstants.ORG_DEFAULT, true, 1, 1);

        // try to add assignment with same oid but different relation -> should be ok
        addAndAssertAssignment(access, ROLE_EXISTING_OID, SchemaConstants.ORG_APPROVER, true, 2, 1);

        // try to add assignment with different oid and same relation -> should be ok
        addAndAssertAssignment(access, ROLE_1_OID, SchemaConstants.ORG_DEFAULT, true, 3, 1);

        // try to add assignment with different oid and relation -> should be ok
        addAndAssertAssignment(access, ROLE_2_OID, SchemaConstants.ORG_APPROVER, true, 4, 1);
    }

    @Test
    public void test200SameRelationNotAllowed() throws Exception {
        updateSystemConfiguration(false, true);

        RequestAccess access = new RequestAccess();
        // select user
        access.addPersonOfInterest(POI_1, List.of(ROLE_EXISTING));

        // try to add assignment with same oid and relation -> should fail
        addAndAssertAssignment(access, ROLE_EXISTING_OID, SchemaConstants.ORG_DEFAULT, false, 0, 0);

        // try to add assignment with same oid but different relation -> should be ok
        addAndAssertAssignment(access, ROLE_EXISTING_OID, SchemaConstants.ORG_APPROVER, true, 1, 1);

        // try to add assignment with different oid and same relation -> should be ok
        addAndAssertAssignment(access, ROLE_1_OID, SchemaConstants.ORG_DEFAULT, true, 2, 1);

        // try to add assignment with different oid and relation -> should be ok
        addAndAssertAssignment(access, ROLE_2_OID, SchemaConstants.ORG_APPROVER, true, 3, 1);
    }

    private void addAndAssertAssignment(
            RequestAccess access, String oid, QName relation,
            boolean expectedCanAddAssignment, int expectedAllCartItemsCount, int expectedOidRelationCartItemCount) {

        ObjectReferenceType targetRef = new ObjectReferenceType()
                .oid(oid)
                .type(RoleType.COMPLEX_TYPE)
                .relation(relation);

        access.setRelation(relation);

        boolean canAddAssignment = access.canAddTemplateAssignment(targetRef);
        Assertions.assertThat(canAddAssignment)
                .withFailMessage("Can add assignment should be " + expectedCanAddAssignment + ", but it is " + canAddAssignment)
                .isEqualTo(expectedCanAddAssignment);

        access.addAssignments(List.of(new AssignmentType().targetRef(targetRef)));
        List<ShoppingCartItem> items = access.getShoppingCartItems();
        Assertions.assertThat(items)
                .withFailMessage("Shopping cart items count should be " + expectedAllCartItemsCount + ", but it is " + items.size())
                .hasSize(expectedAllCartItemsCount);

        ShoppingCartItem item = items.stream().filter(sci -> {
            ObjectReferenceType ref = sci.getAssignment().getTargetRef();
            return Objects.equals(ref.getOid(), oid) && Objects.equals(ref.getRelation(), relation);
        }).findFirst().orElse(null);

        int itemCount = item != null ? item.getCount() : 0;

        Assertions.assertThat(itemCount)
                .withFailMessage(
                        "Shopping cart item count for oid " + oid + " and relation " + relation
                                + " should be " + expectedOidRelationCartItemCount + ", but it is " + itemCount)
                .isEqualTo(expectedOidRelationCartItemCount);
    }

    @Test
    public void test300SameTargetOidNotAllowed() throws Exception {
        updateSystemConfiguration(true, false);

        RequestAccess access = new RequestAccess();
        // select user
        access.addPersonOfInterest(POI_1, List.of(ROLE_EXISTING));

        // try to add assignment with same oid and relation -> should fail
        addAndAssertAssignment(access, ROLE_EXISTING_OID, SchemaConstants.ORG_DEFAULT, false, 0, 0);

        // try to add assignment with same oid but different relation -> should be ok
        addAndAssertAssignment(access, ROLE_EXISTING_OID, SchemaConstants.ORG_APPROVER, false, 0, 0);

        // try to add assignment with different oid and same relation -> should fail
        addAndAssertAssignment(access, ROLE_1_OID, SchemaConstants.ORG_DEFAULT, true, 1, 1);

        // try to add assignment with different oid and relation -> should be ok
        addAndAssertAssignment(access, ROLE_2_OID, SchemaConstants.ORG_APPROVER, true, 2, 1);
    }

    @Test
    public void test400SameRelationAndTargetOidNotAllowed() throws Exception {
        updateSystemConfiguration(false, false);

        RequestAccess access = new RequestAccess();
        // select user
        access.addPersonOfInterest(POI_1, List.of(ROLE_EXISTING));

        // try to add assignment with same oid and relation -> should fail
        addAndAssertAssignment(access, ROLE_EXISTING_OID, SchemaConstants.ORG_DEFAULT, false, 0, 0);

        // try to add assignment with same oid but different relation -> should fail
        addAndAssertAssignment(access, ROLE_EXISTING_OID, SchemaConstants.ORG_APPROVER, false, 0, 0);

        // try to add assignment with different oid and same relation -> should be ok
        addAndAssertAssignment(access, ROLE_1_OID, SchemaConstants.ORG_DEFAULT, true, 1, 1);

        // try to add assignment with different oid and relation -> should be ok
        addAndAssertAssignment(access, ROLE_2_OID, SchemaConstants.ORG_APPROVER, true, 2, 1);
    }

    private void updateSystemConfiguration(Boolean allowSameRelation, Boolean allowSameTarget) throws Exception {
        AssignmentConstraintsType constraints = new AssignmentConstraintsType();
        constraints.setAllowSameRelation(allowSameRelation);
        constraints.allowSameTarget(allowSameTarget);

        Collection<ObjectDelta<? extends ObjectType>> deltas = ObjectTypeUtil.cast(
                prismContext.deltaFor(SystemConfigurationType.class)
                        .item(SystemConfigurationType.F_ROLE_MANAGEMENT, RoleManagementConfigurationType.F_DEFAULT_ASSIGNMENT_CONSTRAINTS)
                        .replace(constraints)
                        .asObjectDeltas(SystemObjectsType.SYSTEM_CONFIGURATION.value()));

        Task task = getTestTask();
        modelService.executeChanges(deltas, ModelExecuteOptions.create(), task, task.getResult());
    }
}
