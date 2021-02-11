/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.constructInitializedPrismContext;

import java.util.Comparator;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.impl.PrismContextImpl;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @see TestCompare
 */
public class TestEquals extends AbstractPrismTest {

    @Test
    public void testContainsEquivalentValue01() throws Exception {
        given();
        //noinspection unchecked
        ObjectDelta<UserType> userDelta = getPrismContext().deltaFactory().object()
                .createModificationDeleteContainer(UserType.class, USER_FOO_OID,
                        UserType.F_ASSIGNMENT,
                        createAssignmentValue(ASSIGNMENT_PATLAMA_ID, null));
        displayValue("userDelta", userDelta);

        PrismObject<UserType> user = createUserFoo();
        addAssignment(user, ASSIGNMENT_PATLAMA_ID, ASSIGNMENT_PATLAMA_DESCRIPTION);
        addAssignment(user, ASSIGNMENT_ABRAKADABRA_ID, ASSIGNMENT_ABRAKADABRA_DESCRIPTION);
        displayValue("user", user);

        PrismContainer<AssignmentType> assignmentContainer = user.findContainer(UserType.F_ASSIGNMENT);

        when();
        Comparator<PrismContainerValue<AssignmentType>> comparator =
                EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS.prismValueComparator();

        then();
        // This is no longer true. Original method containsEquivalentValue took care for container ID equality.
//        assertTrue(ASSIGNMENT_PATLAMA_ID + ":null",
//                ItemCollectionsUtil.containsEquivalentValue(assignmentContainer, createAssignmentValue(ASSIGNMENT_PATLAMA_ID, null), comparator));

        assertTrue("null:" + ASSIGNMENT_PATLAMA_DESCRIPTION,
                ItemCollectionsUtil.containsEquivalentValue(assignmentContainer, createAssignmentValue(null, ASSIGNMENT_PATLAMA_DESCRIPTION), comparator));
        assertFalse("364576:null",
                ItemCollectionsUtil.containsEquivalentValue(assignmentContainer, createAssignmentValue(364576L, null), comparator));
        assertFalse("null:never ever never",
                ItemCollectionsUtil.containsEquivalentValue(assignmentContainer, createAssignmentValue(null, "never ever never"), comparator));
    }

    @Test(enabled = false) // normalization no longer removes empty values TODO and what about test? remove? different asserts?
    public void testEqualsBrokenAssignmentActivation() throws Exception {
        given();
        PrismObjectDefinition<UserType> userDef = PrismInternalTestUtil.getUserTypeDefinition();
        PrismContainerDefinition<AssignmentType> assignmentDef = userDef.findContainerDefinition(UserType.F_ASSIGNMENT);
        PrismContainer<AssignmentType> goodAssignment = assignmentDef.instantiate(UserType.F_ASSIGNMENT);
        PrismContainer<AssignmentType> brokenAssignment = goodAssignment.clone();
        assertEquals("Not equals after clone", goodAssignment, brokenAssignment);
        // lets break one of these ...
        PrismContainerValue<AssignmentType> emptyValue = getPrismContext().itemFactory().createContainerValue();
        brokenAssignment.add(emptyValue);

        then();
        assertThat(goodAssignment).isNotEqualTo(brokenAssignment);

        brokenAssignment.normalize();
        assertEquals("Not equals after normalize(bad)", goodAssignment, brokenAssignment);

        goodAssignment.normalize();
        assertEquals("Not equals after normalize(good)", goodAssignment, brokenAssignment);

    }

    @Test
    public void testEqualsProtectedStringTypePrismValue() throws Exception {
        PrismContextImpl prismContext = constructInitializedPrismContext();
        Protector protector = PrismInternalTestUtil.createProtector(Protector.XMLSEC_ENCRYPTION_ALGORITHM_AES256_CBC);
        prismContext.setDefaultProtector(protector);

        given("protected strings 1 and 3 have the same clear value, but 2 has different one");
        ProtectedStringType p1 = new ProtectedStringType();
        p1.setClearValue("a");
        PrismPropertyValueImpl<?> pv1 = new PrismPropertyValueImpl<>(p1, prismContext);

        ProtectedStringType p2 = new ProtectedStringType();
        p2.setClearValue("b");
        PrismPropertyValueImpl<?> pv2 = new PrismPropertyValueImpl<>(p2, prismContext);

        ProtectedStringType p3 = new ProtectedStringType();
        p3.setClearValue("a");
        PrismPropertyValueImpl<?> pv3 = new PrismPropertyValueImpl<>(p3, prismContext);

        expect("protected string pv1 is considered equal to pv3, but not to pv2");
        assertThat(pv1).isEqualTo(pv3);
        assertThat(pv1).isNotEqualTo(pv2);
    }
}
