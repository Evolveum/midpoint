/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author mederly
 */
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

}
