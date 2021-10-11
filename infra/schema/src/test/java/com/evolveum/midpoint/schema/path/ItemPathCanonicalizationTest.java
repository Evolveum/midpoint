/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.path;

import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import javax.xml.namespace.QName;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.impl.path.CanonicalItemPathImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class ItemPathCanonicalizationTest extends AbstractSchemaTest {

    @Test
    public void testCanonicalizationEmpty() {
        assertCanonical(null, null, "");
        assertCanonical(getPrismContext().emptyPath(), null, "");
    }

    private static final String COMMON = "${common}3";
    private static final String ICFS = "${icf}1/connector-schema-3";
    private static final String ICF = "${icf}1";
    private static final String ZERO = "${0}";
    private static final String ONE = "${1}";

    @Test
    public void testCanonicalizationSimple() {
        ItemPath path = UserType.F_NAME;
        assertCanonical(path, null, "\\" + COMMON + "#name");
    }

    @Test
    public void testCanonicalizationSimpleNoNs() {
        ItemPath path = ItemPath.create(UserType.F_NAME.getLocalPart());
        assertCanonical(path, null, "\\#name");
        assertCanonical(path, UserType.class, "\\" + COMMON + "#name");
    }

    @Test
    public void testCanonicalizationMulti() {
        ItemPath path = ItemPath.create(UserType.F_ASSIGNMENT, 1234, AssignmentType.F_ACTIVATION,
                ActivationType.F_ADMINISTRATIVE_STATUS);
        assertCanonical(path, null, "\\" + COMMON + "#assignment",
                "\\" + COMMON + "#assignment\\" + ZERO + "#activation",
                "\\" + COMMON + "#assignment\\" + ZERO + "#activation\\" + ZERO + "#administrativeStatus");
    }

    @Test
    public void testCanonicalizationMultiNoNs() {
        ItemPath path = ItemPath.create(UserType.F_ASSIGNMENT.getLocalPart(), 1234, AssignmentType.F_ACTIVATION.getLocalPart(),
                ActivationType.F_ADMINISTRATIVE_STATUS.getLocalPart());
        assertCanonical(path, null, "\\#assignment",
                "\\#assignment\\#activation", "\\#assignment\\#activation\\#administrativeStatus");
        assertCanonical(path, UserType.class, "\\" + COMMON + "#assignment",
                "\\" + COMMON + "#assignment\\" + ZERO + "#activation",
                "\\" + COMMON + "#assignment\\" + ZERO + "#activation\\" + ZERO + "#administrativeStatus");
    }

    @Test
    public void testCanonicalizationMixedNs() {
        ItemPath path = ItemPath.create(UserType.F_ASSIGNMENT.getLocalPart(), 1234, AssignmentType.F_EXTENSION,
                new QName("http://piracy.org/inventory", "store"),
                new QName("http://piracy.org/inventory", "shelf"),
                new QName("x"), ActivationType.F_ADMINISTRATIVE_STATUS);
        assertCanonical(path, null,
                "\\#assignment",
                "\\#assignment\\" + COMMON + "#extension",
                "\\#assignment\\" + COMMON + "#extension\\http://piracy.org/inventory#store",
                "\\#assignment\\" + COMMON + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf",
                "\\#assignment\\" + COMMON + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf\\#x",
                "\\#assignment\\" + COMMON + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf\\#x\\" + ZERO + "#administrativeStatus");
        assertCanonical(path, UserType.class,
                "\\" + COMMON + "#assignment",
                "\\" + COMMON + "#assignment\\" + ZERO + "#extension",
                "\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store",
                "\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf",
                "\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf\\#x",
                "\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf\\#x\\" + ZERO + "#administrativeStatus");
    }

    @Test
    public void testCanonicalizationMixedNs2() {
        ItemPath path = ItemPath.create(UserType.F_ASSIGNMENT.getLocalPart(), 1234, AssignmentType.F_EXTENSION.getLocalPart(),
                new QName("http://piracy.org/inventory", "store"),
                new QName("http://piracy.org/inventory", "shelf"),
                AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
        assertCanonical(path, null,
                "\\#assignment",
                "\\#assignment\\#extension",
                "\\#assignment\\#extension\\http://piracy.org/inventory#store",
                "\\#assignment\\#extension\\http://piracy.org/inventory#store\\" + ZERO + "#shelf",
                "\\#assignment\\#extension\\http://piracy.org/inventory#store\\" + ZERO + "#shelf\\" + COMMON + "#activation",
                "\\#assignment\\#extension\\http://piracy.org/inventory#store\\" + ZERO + "#shelf\\" + COMMON + "#activation\\" + ONE + "#administrativeStatus");
        assertCanonical(path, UserType.class,
                "\\" + COMMON + "#assignment",
                "\\" + COMMON + "#assignment\\" + ZERO + "#extension",
                "\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store",
                "\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf",
                "\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf\\" + ZERO + "#activation",
                "\\" + COMMON + "#assignment\\" + ZERO + "#extension\\http://piracy.org/inventory#store\\" + ONE + "#shelf\\" + ZERO + "#activation\\" + ZERO + "#administrativeStatus");
    }

    // from IntegrationTestTools
    private static final String NS_RESOURCE_DUMMY_CONFIGURATION = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.icf.dummy/com.evolveum.icf.dummy.connector.DummyConnector";
    private static final QName RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME = new QName(NS_RESOURCE_DUMMY_CONFIGURATION, "uselessString");

    @Test
    public void testCanonicalizationLong() {
        ItemPath path = ItemPath.create(ResourceType.F_CONNECTOR_CONFIGURATION, SchemaConstants.ICF_CONFIGURATION_PROPERTIES,
                RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME);
        assertCanonical(path, null, "\\" + COMMON + "#connectorConfiguration",
                "\\" + COMMON + "#connectorConfiguration\\" + ICFS + "#configurationProperties",
                "\\" + COMMON + "#connectorConfiguration\\" + ICFS + "#configurationProperties\\" + ICF + "/bundle/com.evolveum.icf.dummy/com.evolveum.icf.dummy.connector.DummyConnector#uselessString");
    }

    private void assertCanonical(ItemPath path, Class<? extends Containerable> clazz, String... representations) {
        CanonicalItemPathImpl canonicalItemPath = CanonicalItemPathImpl.create(path, clazz, getPrismContext());
        System.out.println(path + " => " + canonicalItemPath.asString() + "  (" + clazz + ")");
        for (int i = 0; i < representations.length; i++) {
            String c = canonicalItemPath.allUpToIncluding(i).asString();
            assertEquals("Wrong string representation of length " + (i + 1), representations[i], c);
        }
        assertEquals("Wrong string representation ", representations[representations.length - 1], canonicalItemPath.asString());
    }

}
