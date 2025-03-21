/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.task.WallClockTimeComputer;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

public class TestMiscellaneous extends AbstractSchemaTest {

    public static final File TEST_DIR = new File("src/test/resources/misc");
    private static final File FILE_ROLE_REMOVE_ITEMS = new File(TEST_DIR, "role-remove-items.xml");

    @Test
    public void singleValuedItems() throws Exception {
        UserType userBean = getPrismContext().createObjectable(UserType.class)
                .beginAssignment()
                .id(1L)
                .targetRef(new ObjectReferenceType().oid("123456").type(RoleType.COMPLEX_TYPE))
                .end();

        //noinspection unchecked
        PrismContainerValue<AssignmentType> assignmentPcv = userBean.getAssignment().get(0).asPrismContainerValue();
        PrismContainer<Containerable> limitContentPc = assignmentPcv
                .findOrCreateContainer(AssignmentType.F_LIMIT_TARGET_CONTENT);
        PrismContainerValue<Containerable> val1 = limitContentPc.createNewValue();
        val1.setId(1L);
        PrismContainerValue<Containerable> val2 = val1.clone();
        val2.setId(2L);
        try {
            limitContentPc.add(val2);
            fail("unexpected success");
        } catch (SchemaException e) {
            System.out.println("Got expected exception: " + e);
        }
    }

    @Test
    public void removeOperationalItems() throws Exception {
        PrismObject<RoleType> role = getPrismContext().parseObject(FILE_ROLE_REMOVE_ITEMS);

        AtomicInteger propertyValuesBefore = new AtomicInteger(0);
        role.accept(o -> {
            if (o instanceof PrismPropertyValue) {
                propertyValuesBefore.incrementAndGet();
                System.out.println(((PrismPropertyValue) o).getPath() + ": " + ((PrismPropertyValue) o).getValue());
            }
        });

        System.out.println("Property values before: " + propertyValuesBefore);

        role.getValue().removeOperationalItems();
        System.out.println("After operational items removal:\n" + getPrismContext().xmlSerializer().serialize(role));

        AtomicInteger propertyValuesAfter = new AtomicInteger(0);
        role.accept(o -> {
            if (o instanceof PrismPropertyValue) {
                propertyValuesAfter.incrementAndGet();
                System.out.println(((PrismPropertyValue) o).getPath() + ": " + ((PrismPropertyValue) o).getValue());
            }
        });
        System.out.println("Property values after: " + propertyValuesAfter);

        assertNull("metadata container present", role.findContainer(RoleType.F_METADATA));
        assertNull("effectiveStatus present", role.findProperty(ItemPath.create(RoleType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS)));
        assertNull("assignment[1]/activation/effectiveStatus present",
                role.findProperty(ItemPath.create(RoleType.F_ASSIGNMENT, 1L, AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS)));

        assertEquals("Wrong property values after", propertyValuesBefore.intValue() - 6, propertyValuesAfter.intValue());
    }

    /**
     * MID-6256
     */
    @Test
    public void testFilterInReferences() {
        UserType user = new UserType(getPrismContext())
                .linkRef("123", ShadowType.COMPLEX_TYPE);

        ObjectReferenceType reference = user.getLinkRef().get(0);
        assertThat(reference.getFilter()).as("filter in simple reference").isNull();
    }

    @Test
    public void testWallClockComputer1() {
        assertThat(new WallClockTimeComputer(new long[][] {
                { 1000, 1100 }, // 100
                { 1000, 1110 }, // +10
                { 700, 800 }, // 100
                { 650, 850 }, // +100
                { 3000, 3200} // +200
        }).getSummaryTime()).isEqualTo(510);
    }

    @Test
    public void testWallClockComputerEmpty() {
        assertThat(new WallClockTimeComputer(new long[][] {})
                .getSummaryTime()).isEqualTo(0);
    }

    @Test
    public void testWallClockComputerSame() {
        assertThat(new WallClockTimeComputer(new long[][] {
                { 100, 110 },
                { 100, 110 },
                { 100, 110 }
        }).getSummaryTime()).isEqualTo(10);
    }

    @Test
    public void testWallClockComputerDisjunct() {
        assertThat(new WallClockTimeComputer(new long[][] {
                { 100, 110 },
                { 200, 210 },
                { 300, 310 }
        }).getSummaryTime()).isEqualTo(30);
    }

    @Test
    public void testWallClockComputerJoint() {
        assertThat(new WallClockTimeComputer(new long[][] {
                { 100, 110 },
                { 110, 120 },
                { 120, 130 }
        }).getSummaryTime()).isEqualTo(30);
    }

    /**
     * Changing the targetRef in an assignment via {@link AssignmentType#targetRef(String, QName, QName)}.
     *
     * MID-10578
     */
    @Test
    public void testRelationSetViaTargetRefMethod() {
        var oid = UUID.randomUUID().toString();
        var assignment = new AssignmentType();
        assignment.targetRef(oid, RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT);
        assignment.targetRef(oid, RoleType.COMPLEX_TYPE, SchemaConstants.ORG_APPROVER);
        assertThat(assignment.getTargetRef().getRelation())
                .as("relation in assignment")
                .isEqualTo(SchemaConstants.ORG_APPROVER);
    }

    /**
     * Changing the targetRef in an assignment via {@link AssignmentType#setTargetRef(ObjectReferenceType)}.
     *
     * MID-10578
     */
    @Test
    public void testRelationSetViaSetTargetRefMethod() {
        var oid = UUID.randomUUID().toString();
        var assignment = new AssignmentType();
        assignment.targetRef(oid, RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT);
        assignment.setTargetRef(
                new ObjectReferenceType()
                        .oid(oid)
                        .type(RoleType.COMPLEX_TYPE)
                        .relation(SchemaConstants.ORG_APPROVER));
        assertThat(assignment.getTargetRef().getRelation())
                .as("relation in assignment")
                .isEqualTo(SchemaConstants.ORG_APPROVER);
    }

    /**
     * Clearing the targetRef before setting the new value.
     *
     * MID-10578
     */
    @Test
    public void testClearingTargetRefBeforeSettingTheRelation() {
        var oid = UUID.randomUUID().toString();
        var assignment = new AssignmentType();
        assignment.targetRef(oid, RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT);
        assignment.setTargetRef(null);
        assignment.targetRef(oid, RoleType.COMPLEX_TYPE, SchemaConstants.ORG_APPROVER);
        assertThat(assignment.getTargetRef().getRelation())
                .as("relation in assignment")
                .isEqualTo(SchemaConstants.ORG_APPROVER);
    }
}
