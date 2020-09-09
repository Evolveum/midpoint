/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.USER_JACK_OID;

import java.util.Collection;

import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author semancik
 */
public class TestDiff extends AbstractPrismTest {

    @Test
    public void testUserSimplePropertyDiffNoChange() throws Exception {
        // GIVEN
        PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();

        PrismObject<UserType> user1 = userDef.instantiate();
        user1.setOid(USER_JACK_OID);
        user1.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("test name"));

        PrismObject<UserType> user2 = userDef.instantiate();
        user2.setOid(USER_JACK_OID);
        user2.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("test name"));

        // WHEN
        ObjectDelta<UserType> delta = user1.diff(user2);

        // THEN
        assertNotNull(delta);
        assertEquals("Unexpected number of midifications", 0, delta.getModifications().size());
        assertEquals("Wrong OID", USER_JACK_OID, delta.getOid());
        delta.checkConsistence();
    }

    @Test
    public void testPropertySimplePropertyDiffNoChange() throws Exception {
        // GIVEN
        PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();

        PrismObject<UserType> user1 = userDef.instantiate();
        user1.setOid(USER_JACK_OID);
        user1.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("test name"));
        PrismProperty<PolyString> user1NameProp = user1.findProperty(UserType.F_NAME);

        PrismObject<UserType> user2 = userDef.instantiate();
        user2.setOid(USER_JACK_OID);
        user2.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("test name"));
        PrismProperty<PolyString> user2NameProp = user2.findProperty(UserType.F_NAME);

        // WHEN
        PropertyDelta<PolyString> delta = user1NameProp.diff(user2NameProp);

        // THEN
        assertNull(delta);
    }

    @Test
    public void testPropertySimplePropertyDiffNoChangeStatic() throws Exception {
        // GIVEN
        PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();

        PrismObject<UserType> user1 = userDef.instantiate();
        user1.setOid(USER_JACK_OID);
        user1.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("test name"));
        PrismProperty<PolyString> user1NameProp = user1.findProperty(UserType.F_NAME);

        PrismObject<UserType> user2 = userDef.instantiate();
        user2.setOid(USER_JACK_OID);
        user2.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("test name"));
        PrismProperty<PolyString> user2NameProp = user2.findProperty(UserType.F_NAME);

        // WHEN
        PropertyDelta<PolyString> delta = ItemUtil.diff(user1NameProp, user2NameProp);

        // THEN
        assertNull(delta);
    }

    @Test
    public void testUserSimplePropertyDiffReplace() throws Exception {
        // GIVEN
        PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();

        PrismObject<UserType> user1 = userDef.instantiate();
        user1.setOid(USER_JACK_OID);
        user1.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("test name"));

        PrismObject<UserType> user2 = userDef.instantiate();
        user2.setOid(USER_JACK_OID);
        user2.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("other name"));

        // WHEN
        ObjectDelta<UserType> delta = user1.diff(user2);

        // THEN
        assertNotNull(delta);
        System.out.println(delta.debugDump());
        assertEquals("Unexpected number of midifications", 1, delta.getModifications().size());
        PrismAsserts.assertPropertyReplace(delta, UserType.F_NAME, PrismTestUtil.createPolyString("other name"));
        assertEquals("Wrong OID", USER_JACK_OID, delta.getOid());
        delta.checkConsistence();
    }

    @Test
    public void testPropertyUserSimplePropertyDiffReplace() throws Exception {
        // GIVEN
        PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();

        PrismObject<UserType> user1 = userDef.instantiate();
        user1.setOid(USER_JACK_OID);
        user1.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("test name"));
        PrismProperty<PolyString> user1NameProp = user1.findProperty(UserType.F_NAME);

        PrismObject<UserType> user2 = userDef.instantiate();
        user2.setOid(USER_JACK_OID);
        user2.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("other name"));
        PrismProperty<PolyString> user2NameProp = user2.findProperty(UserType.F_NAME);

        // WHEN
        PropertyDelta<PolyString> delta = user1NameProp.diff(user2NameProp);

        // THEN
        assertNotNull(delta);
        System.out.println(delta.debugDump());
        PrismAsserts.assertReplace(delta, PrismTestUtil.createPolyString("other name"));
        delta.checkConsistence();
    }

    @Test
    public void testPropertyUserSimplePropertyDiffReplaceStatic() throws Exception {
        // GIVEN
        PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();

        PrismObject<UserType> user1 = userDef.instantiate();
        user1.setOid(USER_JACK_OID);
        user1.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("test name"));
        PrismProperty<PolyString> user1NameProp = user1.findProperty(UserType.F_NAME);

        PrismObject<UserType> user2 = userDef.instantiate();
        user2.setOid(USER_JACK_OID);
        user2.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("other name"));
        PrismProperty<PolyString> user2NameProp = user2.findProperty(UserType.F_NAME);

        // WHEN
        PropertyDelta<PolyString> delta = ItemUtil.diff(user1NameProp, user2NameProp);

        // THEN
        assertNotNull(delta);
        System.out.println(delta.debugDump());
        PrismAsserts.assertReplace(delta, PrismTestUtil.createPolyString("other name"));
        delta.checkConsistence();
    }

    @Test
    public void testUserSimpleDiffMultiNoChange() throws Exception {
        // GIVEN
        PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();

        PrismObject<UserType> user1 = userDef.instantiate();
        user1.setOid(USER_JACK_OID);
        PrismProperty<String> anamesProp1 = user1.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
        anamesProp1.addRealValue("foo");
        anamesProp1.addRealValue("bar");

        PrismObject<UserType> user2 = userDef.instantiate();
        user2.setOid(USER_JACK_OID);
        PrismProperty<String> anamesProp2 = user2.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
        anamesProp2.addRealValue("foo");
        anamesProp2.addRealValue("bar");

        // WHEN
        ObjectDelta<UserType> delta = user1.diff(user2);

        // THEN
        assertNotNull(delta);
        assertEquals("Unexpected number of midifications", 0, delta.getModifications().size());
        assertEquals("Wrong OID", USER_JACK_OID, delta.getOid());
        delta.checkConsistence();
    }

    @Test
    public void testPropertyUserSimpleDiffMultiNoChange() throws Exception {
        // GIVEN
        PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();

        PrismObject<UserType> user1 = userDef.instantiate();
        user1.setOid(USER_JACK_OID);
        PrismProperty<String> additionalNamesProp1 = user1.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
        additionalNamesProp1.addRealValue("foo");
        additionalNamesProp1.addRealValue("bar");

        PrismObject<UserType> user2 = userDef.instantiate();
        user2.setOid(USER_JACK_OID);
        PrismProperty<String> additionalNamesProp2 = user2.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
        additionalNamesProp2.addRealValue("foo");
        additionalNamesProp2.addRealValue("bar");

        // WHEN
        PropertyDelta<String> delta = additionalNamesProp1.diff(additionalNamesProp2);

        // THEN
        assertNull(delta);
    }

    @Test
    public void testPropertyUserSimpleDiffMultiNoChangeStatic() throws Exception {
        // GIVEN
        PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();

        PrismObject<UserType> user1 = userDef.instantiate();
        user1.setOid(USER_JACK_OID);
        PrismProperty<String> additionalNamesProp1 = user1.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
        additionalNamesProp1.addRealValue("foo");
        additionalNamesProp1.addRealValue("bar");

        PrismObject<UserType> user2 = userDef.instantiate();
        user2.setOid(USER_JACK_OID);
        PrismProperty<String> additionalNamesProp2 = user2.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
        additionalNamesProp2.addRealValue("foo");
        additionalNamesProp2.addRealValue("bar");

        // WHEN
        PropertyDelta<String> delta = ItemUtil.diff(additionalNamesProp1, additionalNamesProp2);

        // THEN
        assertNull(delta);
    }

    @Test
    public void testUserSimpleDiffMultiAdd() throws Exception {
        // GIVEN
        PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();

        PrismObject<UserType> user1 = userDef.instantiate();
        user1.setOid(USER_JACK_OID);
        PrismProperty<String> anamesProp1 = user1.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
        anamesProp1.addRealValue("foo");
        anamesProp1.addRealValue("bar");

        PrismObject<UserType> user2 = userDef.instantiate();
        user2.setOid(USER_JACK_OID);
        PrismProperty<String> anamesProp2 = user2.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
        anamesProp2.addRealValue("foo");
        anamesProp2.addRealValue("bar");
        anamesProp2.addRealValue("baz");

        // WHEN
        ObjectDelta<UserType> delta = user1.diff(user2);

        // THEN
        assertNotNull(delta);
        System.out.println(delta.debugDump());
        assertEquals("Unexpected number of midifications", 1, delta.getModifications().size());
        PrismAsserts.assertPropertyAdd(delta, UserType.F_ADDITIONAL_NAMES, "baz");
        assertEquals("Wrong OID", USER_JACK_OID, delta.getOid());
        delta.checkConsistence();
    }

    @Test
    public void testPropertyUserSimpleDiffMultiAdd() throws Exception {
        // GIVEN
        PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();

        PrismObject<UserType> user1 = userDef.instantiate();
        user1.setOid(USER_JACK_OID);
        PrismProperty<String> additionalNamesProp1 = user1.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
        additionalNamesProp1.addRealValue("foo");
        additionalNamesProp1.addRealValue("bar");

        PrismObject<UserType> user2 = userDef.instantiate();
        user2.setOid(USER_JACK_OID);
        PrismProperty<String> additionalNamesProp2 = user2.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
        additionalNamesProp2.addRealValue("foo");
        additionalNamesProp2.addRealValue("bar");
        additionalNamesProp2.addRealValue("baz");

        // WHEN
        PropertyDelta<String> delta = additionalNamesProp1.diff(additionalNamesProp2);

        // THEN
        assertNotNull(delta);
        System.out.println(delta.debugDump());
        PrismAsserts.assertAdd(delta, "baz");
        delta.checkConsistence();
    }

    @Test
    public void testPropertyUserSimpleDiffMultiAddStatic() throws Exception {
        // GIVEN
        PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();

        PrismObject<UserType> user1 = userDef.instantiate();
        user1.setOid(USER_JACK_OID);
        PrismProperty<String> additionalNamesProp1 = user1.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
        additionalNamesProp1.addRealValue("foo");
        additionalNamesProp1.addRealValue("bar");

        PrismObject<UserType> user2 = userDef.instantiate();
        user2.setOid(USER_JACK_OID);
        PrismProperty<String> additionalNamesProp2 = user2.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
        additionalNamesProp2.addRealValue("foo");
        additionalNamesProp2.addRealValue("bar");
        additionalNamesProp2.addRealValue("baz");

        // WHEN
        PropertyDelta<String> delta = ItemUtil.diff(additionalNamesProp1, additionalNamesProp2);

        // THEN
        assertNotNull(delta);
        System.out.println(delta.debugDump());
        PrismAsserts.assertAdd(delta, "baz");
        delta.checkConsistence();
    }

    @Test
    public void testPropertyUserSimpleDiffMultiAddStaticNull1() throws Exception {
        // GIVEN
        PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();

        PrismObject<UserType> user2 = userDef.instantiate();
        user2.setOid(USER_JACK_OID);
        PrismProperty<String> additionalNamesProp2 = user2.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
        additionalNamesProp2.addRealValue("foo");
        additionalNamesProp2.addRealValue("bar");

        // WHEN
        PropertyDelta<String> delta = ItemUtil.diff(null, additionalNamesProp2);

        // THEN
        assertNotNull(delta);
        System.out.println(delta.debugDump());
        PrismAsserts.assertAdd(delta, "foo", "bar");
        delta.checkConsistence();
    }

    @Test
    public void testPropertyUserSimpleDiffMultiAddStaticNull2() throws Exception {
        // GIVEN
        PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();

        PrismObject<UserType> user2 = userDef.instantiate();
        user2.setOid(USER_JACK_OID);
        PrismProperty<String> additionalNamesProp1 = user2.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
        additionalNamesProp1.addRealValue("bar");
        additionalNamesProp1.addRealValue("baz");

        // WHEN
        PropertyDelta<String> delta = ItemUtil.diff(additionalNamesProp1, null);

        // THEN
        assertNotNull(delta);
        System.out.println(delta.debugDump());
        PrismAsserts.assertDelete(delta, "bar", "baz");
        delta.checkConsistence();
    }

    @Test
    public void testContainerSimpleDiffModificationsNoChange() throws Exception {
        // GIVEN
        PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();
        PrismContainerDefinition<AssignmentType> assignmentContDef = userDef.findContainerDefinition(UserType.F_ASSIGNMENT);

        PrismContainer<AssignmentType> ass1 = assignmentContDef.instantiate();
        PrismContainerValue<AssignmentType> ass1cval = ass1.createNewValue();
        ass1cval.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "blah blah", PrismTestUtil.getPrismContext());

        PrismContainer<AssignmentType> ass2 = assignmentContDef.instantiate();
        PrismContainerValue<AssignmentType> ass2cval = ass2.createNewValue();
        ass2cval.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "blah blah", PrismTestUtil.getPrismContext());

        // WHEN
        Collection<? extends ItemDelta> modifications = ass1.diffModifications(ass2, EquivalenceStrategy.DATA);

        // THEN
        assertNotNull(modifications);
        System.out.println(DebugUtil.debugDump(modifications));
        assertEquals("Unexpected number of modifications", 0, modifications.size());
        ItemDeltaCollectionsUtil.checkConsistence(modifications);
    }

    @Test
    public void testContainerDiffModificationsDescription() throws Exception {
        // GIVEN
        PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();
        PrismContainerDefinition<AssignmentType> assignmentContDef = userDef.findContainerDefinition(UserType.F_ASSIGNMENT);

        PrismContainer<AssignmentType> ass1 = assignmentContDef.instantiate();
        PrismContainerValue<AssignmentType> ass1cval = ass1.createNewValue();
        ass1cval.setId(1L);
        ass1cval.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "blah blah", PrismTestUtil.getPrismContext());

        PrismContainer<AssignmentType> ass2 = assignmentContDef.instantiate();
        PrismContainerValue<AssignmentType> ass2cval = ass2.createNewValue();
        ass2cval.setId(1L);
        ass2cval.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "chamalalia patlama paprtala", PrismTestUtil.getPrismContext());

        // WHEN
        Collection<? extends ItemDelta> modifications = ass1.diffModifications(ass2, EquivalenceStrategy.REAL_VALUE);

        // THEN
        assertNotNull(modifications);
        System.out.println(DebugUtil.debugDump(modifications));
        assertEquals("Unexpected number of modifications", 1, modifications.size());
        PrismAsserts.assertPropertyReplace(
                modifications,
                ItemPath.create(UserType.F_ASSIGNMENT, 1L, AssignmentType.F_DESCRIPTION),
                "chamalalia patlama paprtala");
        ItemDeltaCollectionsUtil.checkConsistence(modifications);
    }

    @Test
    public void testContainerValueDiffDescriptionNoPath() throws Exception {
        // GIVEN
        PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();
        PrismContainerDefinition<AssignmentType> assignmentContDef = userDef.findContainerDefinition(UserType.F_ASSIGNMENT);

        PrismContainer<AssignmentType> ass1 = assignmentContDef.instantiate();
        PrismContainerValue<AssignmentType> ass1cval = ass1.createNewValue();
        ass1cval.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "blah blah", PrismTestUtil.getPrismContext());

        PrismContainer<AssignmentType> ass2 = assignmentContDef.instantiate();
        PrismContainerValue<AssignmentType> ass2cval = ass2.createNewValue();
        ass2cval.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "chamalalia patlama paprtala", PrismTestUtil.getPrismContext());

        // WHEN
        Collection<? extends ItemDelta> modifications = ass1cval.diff(ass2cval, EquivalenceStrategy.IGNORE_METADATA);

        // THEN
        assertNotNull(modifications);
        System.out.println(DebugUtil.debugDump(modifications));
        assertEquals("Unexpected number of midifications", 1, modifications.size());
        PrismAsserts.assertPropertyReplace(
                modifications,
                ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION),
                "chamalalia patlama paprtala");
        ItemDeltaCollectionsUtil.checkConsistence(modifications);
    }
}
