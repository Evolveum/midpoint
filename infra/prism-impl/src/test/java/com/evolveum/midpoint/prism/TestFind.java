/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;

import java.io.IOException;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.foo.AccountConstructionType;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 */
public class TestFind extends AbstractPrismTest {

    @Test
    public void testFindString() throws SchemaException, IOException {
        // GIVEN
        PrismObject<UserType> user = createUser();
        ItemPath path = UserType.F_DESCRIPTION;

        // WHEN
        PrismProperty<String> nameProperty = findProperty(user, path);

        // THEN
        assertEquals("Wrong property value (path=" + path + ")", USER_JACK_DESCRIPTION, nameProperty.getRealValue());
        assertTrue("QName found something other", nameProperty == (PrismProperty) user.findProperty(UserType.F_DESCRIPTION));
    }

    @Test
    public void testFindPolyString() throws SchemaException, IOException {
        // GIVEN
        PrismObject<UserType> user = createUser();
        ItemPath path = UserType.F_POLY_NAME;

        // WHEN
        PrismProperty<PolyString> nameProperty = findProperty(user, path);

        // THEN
        PrismInternalTestUtil.asssertJackPolyName(nameProperty, user, true);
        assertTrue("QName found something other", nameProperty == (PrismProperty) user.findProperty(UserType.F_POLY_NAME));
    }

    @Test
    public void testFindPolyStringOrig() throws SchemaException, IOException {
        // GIVEN
        ItemPath path = ItemPath.create(UserType.F_POLY_NAME, PolyString.F_ORIG);

        // WHEN
        Object found = findUser(path);

        // THEN
        assertEquals("Wrong property value (path=" + path + ")", USER_JACK_POLYNAME_ORIG, found);
    }

    @Test
    public void testFindPolyStringNorm() throws SchemaException, IOException {
        // GIVEN
        ItemPath path = ItemPath.create(UserType.F_POLY_NAME, PolyString.F_NORM);

        // WHEN
        Object found = findUser(path);

        // THEN
        assertEquals("Wrong property value (path=" + path + ")", USER_JACK_POLYNAME_NORM, found);
    }

    @Test
    public void testFindExtensionBar() throws SchemaException, IOException {
        // GIVEN
        ItemPath path = ItemPath.create(UserType.F_EXTENSION, EXTENSION_BAR_ELEMENT);

        // WHEN
        PrismProperty<String> property = findUserProperty(path);

        // THEN
        assertEquals("Wrong property value (path=" + path + ")", "BAR", property.getAnyRealValue());
    }

    @Test
    public void testFindAssignment1Description() throws SchemaException, IOException {
        // GIVEN
        ItemPath path = ItemPath.create(UserType.F_ASSIGNMENT, USER_ASSIGNMENT_1_ID, AssignmentType.F_DESCRIPTION);

        // WHEN
        PrismProperty<String> property = findUserProperty(path);

        // THEN
        assertEquals("Wrong property value (path=" + path + ")", "Assignment 1", property.getRealValue());
    }

    @Test
    public void testFindAssignment2Construction() throws SchemaException, IOException {
        // GIVEN
        ItemPath path = ItemPath.create(UserType.F_ASSIGNMENT, USER_ASSIGNMENT_2_ID, AssignmentType.F_ACCOUNT_CONSTRUCTION);

        // WHEN
        PrismProperty<AccountConstructionType> property = findUserProperty(path);

        // THEN
        assertEquals("Wrong property value (path=" + path + ")", "Just do it", property.getRealValue().getHowto());
    }

    @Test
    public void testFindAssignment() throws SchemaException, IOException {
        // GIVEN
        ItemPath path = UserType.F_ASSIGNMENT;

        // WHEN
        PrismContainer<AssignmentType> container = findUserContainer(path);

        // THEN
        PrismContainerValue<AssignmentType> value2 = container.getValue(USER_ASSIGNMENT_2_ID);
        assertEquals("Wrong value2 description (path=" + path + ")", "Assignment 2", value2.findProperty(AssignmentType.F_DESCRIPTION).getRealValue());
    }

    private <T> T findUser(ItemPath path) throws SchemaException, IOException {
        PrismObject<UserType> user = createUser();
        return find(user, path);
    }

    private <T> T find(PrismObject<UserType> user, ItemPath path) {
        System.out.println("Path:");
        System.out.println(path);

        // WHEN
        Object found = user.find(path);

        // THEN
        System.out.println("Found:");
        System.out.println(found);
        return (T) found;
    }

    private <T> PrismProperty<T> findUserProperty(ItemPath path) throws SchemaException, IOException {
        PrismObject<UserType> user = createUser();
        return findProperty(user, path);
    }

    private <T> PrismProperty<T> findProperty(PrismObject<UserType> user, ItemPath path) {
        System.out.println("Path:");
        System.out.println(path);

        // WHEN
        PrismProperty<T> property = user.findProperty(path);

        // THEN
        System.out.println("Found:");
        System.out.println(property);
        return property;
    }

    private <T extends Containerable> PrismContainer<T> findUserContainer(ItemPath path) throws SchemaException, IOException {
        PrismObject<UserType> user = createUser();
        return findContainer(user, path);
    }

    private <T extends Containerable> PrismContainer<T> findContainer(PrismObject<UserType> user, ItemPath path) {
        System.out.println("Path:");
        System.out.println(path);

        // WHEN
        PrismContainer<T> container = user.findContainer(path);

        // THEN
        System.out.println("Found:");
        System.out.println(container);
        return container;
    }

    public PrismObject<UserType> createUser() throws SchemaException, IOException {
        return PrismTestUtil.parseObject(USER_JACK_FILE_XML);
    }

}
