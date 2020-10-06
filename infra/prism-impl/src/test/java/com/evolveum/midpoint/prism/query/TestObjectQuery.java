/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.impl.match.MatchingRuleRegistryFactory;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.DOMUtil;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.function.BiFunction;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.EXTENSION_DATETIME_ELEMENT;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.EXTENSION_NUM_ELEMENT;

public class TestObjectQuery extends AbstractPrismTest {

    public static final File FILE_USER_JACK_FILTERS = new File(PrismInternalTestUtil.COMMON_DIR_XML, "user-jack-filters.xml");

    private static final MatchingRuleRegistry MATCHING_RULE_REGISTRY =
            MatchingRuleRegistryFactory.createRegistry();

    @Test
    public void testMatchAndFilter() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        ObjectFilter filter =
                getPrismContext().queryFor(UserType.class)
                        .item(UserType.F_GIVEN_NAME).eq("Jack").matchingCaseIgnore()
                        .and().item(UserType.F_FULL_NAME).contains("arr")
                        .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertTrue("filter does not match object", match);
    }

    @Test
    public void testMatchOrFilter() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).eq("Jack")
                .or().item(UserType.F_GIVEN_NAME).eq("Jackie")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertTrue("filter does not match object", match);
    }

    @Test
    public void testDontMatchEqualFilter() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).eq("Jackie")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }

    @Test
    public void testMatchEqualMultivalue() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        MutablePrismPropertyDefinition<?> def = getPrismContext().definitionFactory().createPropertyDefinition(new QName("indexedString"), DOMUtil.XSD_STRING);
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(ItemPath.create(UserType.F_EXTENSION, "indexedString"), def).eq("alpha")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertTrue("filter does not match object", match);
    }

    @Test
    public void testMatchEqualNonEmptyAgainstEmptyItem() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        // jack has no locality
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_LOCALITY).eq("some")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }

    @Test
    public void testMatchEqualEmptyAgainstEmptyItem() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        // jack has no locality
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_LOCALITY).isNull()
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertTrue("filter does not match object", match);
    }

    @Test
    public void testMatchEqualEmptyAgainstNonEmptyItem() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        // jack has no locality
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_NAME).isNull()
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }

    @Test
    public void testComplexMatch() throws Exception {
        PrismObject<UserType> user = parseUserJack();
//        System.out.println("user given name" + user.asObjectable().getGivenName());
        System.out.println("definition: " + user.findItem(UserType.F_FAMILY_NAME).getDefinition().debugDump());
        ObjectFilter filter =
                getPrismContext().queryFor(UserType.class)
                        .item(UserType.F_FAMILY_NAME).eq("Sparrow")
                        .and().item(UserType.F_FULL_NAME).contains("arr")
                        .and()
                        .block()
                        .item(UserType.F_GIVEN_NAME).eq("Jack")
                        .or().item(UserType.F_GIVEN_NAME).eq("Jackie")
                        .endBlock()
                        .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertTrue("filter does not match object", match);
    }

    @Test
    public void testPolystringMatchEqualFilter() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        PolyString name = new PolyString("jack", "jack");
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_NAME).eq(name)
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertTrue("filter does not match object", match);
    }

    @Test   // MID-4120
    public void testMatchSubstringAgainstEmptyItem() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        // jack has no locality
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_LOCALITY).startsWith("C")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }

    @Test   // MID-4173
    public void testExistsNegative() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT)
                .item(AssignmentType.F_DESCRIPTION).eq("Assignment NONE")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }

    @Test   // MID-4173
    public void testExistsPositive() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT)
                .item(AssignmentType.F_DESCRIPTION).eq("Assignment 2")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertTrue("filter does not match object, but it should", match);
    }

    @Test   // MID-4173
    public void testExistsAnyNegative() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        user.removeContainer(UserType.F_ASSIGNMENT);
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT)
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }

    @Test   // MID-4173
    public void testExistsAnyPositive() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT)
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertTrue("filter does not match object, but it should", match);
    }

    @Test   // MID-4217
    public void testMultiRootPositive() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION).eq("Assignment 2")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertTrue("filter does not match object, but it should", match);
    }

    @Test   // MID-4217
    public void testMultiRootNegative() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION).eq("Assignment XXXXX")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }

    @Test   // MID-4217
    public void testRefPositive() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_ACCOUNT_REF).ref("c0c010c0-d34d-b33f-f00d-aaaaaaaa1113")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertTrue("filter does not match object, but it should", match);
    }

    @Test   // MID-4217
    public void testRefNegative() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_ACCOUNT_REF).ref("xxxxxxxxxxxxxx")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }

    @Test
    public void testRefRelationNegative() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_ACCOUNT_REF).refRelation(new QName("a-relation"))
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }

    @Test // MID-6487
    public void testGtFilter() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        ObjectFilter filter =
                getPrismContext().queryFor(UserType.class)
                        .item(UserType.F_NAME).gt("j").matchingOrig()
                        .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertTrue("filter does not match object", match);
    }

    @Test // MID-6487
    public void testGtFilterWrongRule() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        ObjectFilter filter =
                getPrismContext().queryFor(UserType.class)
                        .item(UserType.F_NAME).gt("j").matchingCaseIgnore()
                        .buildFilter();
        try {
            ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
            fail("match with wrong matching rule succeeded");
        } catch (IllegalArgumentException e) {
            displayExpectedException(e);
        }
    }

    @Test // MID-6487
    public void testLtFilter() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        ObjectFilter filter =
                getPrismContext().queryFor(UserType.class)
                        .item(UserType.F_NAME).lt("j").matchingNorm()
                        .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }

    @Test // MID-6487
    public void testNumericFilters() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        assertNumGeFilter(user, 42, true);
        assertNumGeFilter(user, 44, false);
        assertNumGeFilter(user, 40, true);
        assertNumGeFilter(user, 42.0, true);
        assertNumGeFilter(user, 44.0, false);
        assertNumGeFilter(user, 40.0, true);
        assertNumGeFilter(user, 42.0f, true);
        assertNumGeFilter(user, 44.0f, false);
        assertNumGeFilter(user, 40.0f, true);
        assertNumGeFilter(user, 42L, true);
        assertNumGeFilter(user, 44L, false);
        assertNumGeFilter(user, 40L, true);
        assertNumGeFilter(user, (short) 42, true);
        assertNumGeFilter(user, (short) 44, false);
        assertNumGeFilter(user, (short) 40, true);
        assertNumGeFilter(user, BigInteger.valueOf(42), true);
        assertNumGeFilter(user, BigInteger.valueOf(44), false);
        assertNumGeFilter(user, BigInteger.valueOf(40), true);

        assertNumLtFilter(user, 42, false);
        assertNumLtFilter(user, 44, true);
        assertNumLtFilter(user, 40, false);
        assertNumLtFilter(user, 42.0, false);
        assertNumLtFilter(user, 44.0, true);
        assertNumLtFilter(user, 40.0, false);
        assertNumLtFilter(user, 42.0f, false);
        assertNumLtFilter(user, 44.0f, true);
        assertNumLtFilter(user, 40.0f, false);
        assertNumLtFilter(user, 42L, false);
        assertNumLtFilter(user, 44L, true);
        assertNumLtFilter(user, 40L, false);
        assertNumLtFilter(user, (short) 42, false);
        assertNumLtFilter(user, (short) 44, true);
        assertNumLtFilter(user, (short) 40, false);
        assertNumLtFilter(user, BigInteger.valueOf(42), false);
        assertNumLtFilter(user, BigInteger.valueOf(44), true);
        assertNumLtFilter(user, BigInteger.valueOf(40), false);
    }

    @Test // MID-6577
    public void testDateTimeFilters() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        XMLGregorianCalendar equal = XmlTypeConverter.createXMLGregorianCalendar("2020-07-07T00:00:00.000+02:00");
        XMLGregorianCalendar earlier = XmlTypeConverter.createXMLGregorianCalendar("2020-07-06T00:00:00.000+02:00");
        XMLGregorianCalendar later = XmlTypeConverter.createXMLGregorianCalendar("2020-07-08T00:00:00.000+02:00");

        assertDateTimeGeFilter(user, equal, true);
        assertDateTimeGeFilter(user, later, false);
        assertDateTimeGeFilter(user, earlier, true);

        assertDateTimeLeFilter(user, equal, true);
        assertDateTimeLeFilter(user, later, true);
        assertDateTimeLeFilter(user, earlier, false);

        assertDateTimeGtFilter(user, equal, false);
        assertDateTimeGtFilter(user, later, false);
        assertDateTimeGtFilter(user, earlier, true);

        assertDateTimeLtFilter(user, equal, false);
        assertDateTimeLtFilter(user, later, true);
        assertDateTimeLtFilter(user, earlier, false);
    }

    private void assertNumGeFilter(PrismObject<UserType> user, Object value, boolean expected) throws SchemaException {
        assertGeFilter(user, EXTENSION_NUM_ELEMENT, DOMUtil.XSD_INT, value, expected);
    }

    private void assertNumLtFilter(PrismObject<UserType> user, Object value, boolean expected) throws SchemaException {
        assertLtFilter(user, EXTENSION_NUM_ELEMENT, DOMUtil.XSD_INT, value, expected);
    }

    private void assertDateTimeGeFilter(PrismObject<UserType> user, Object value, boolean expected) throws SchemaException {
        assertGeFilter(user, EXTENSION_DATETIME_ELEMENT, DOMUtil.XSD_DATETIME, value, expected);
    }

    private void assertDateTimeLeFilter(PrismObject<UserType> user, Object value, boolean expected) throws SchemaException {
        assertLeFilter(user, EXTENSION_DATETIME_ELEMENT, DOMUtil.XSD_DATETIME, value, expected);
    }

    private void assertDateTimeGtFilter(PrismObject<UserType> user, Object value, boolean expected) throws SchemaException {
        assertGtFilter(user, EXTENSION_DATETIME_ELEMENT, DOMUtil.XSD_DATETIME, value, expected);
    }

    private void assertDateTimeLtFilter(PrismObject<UserType> user, Object value, boolean expected) throws SchemaException {
        assertLtFilter(user, EXTENSION_DATETIME_ELEMENT, DOMUtil.XSD_DATETIME, value, expected);
    }

    private void assertGeFilter(PrismObject<UserType> user, ItemName itemName, QName itemType, Object value, boolean expected) throws SchemaException {
        assertFilter(user, itemName, itemType, value, expected,
                (path, definition) -> getPrismContext().queryFor(UserType.class)
                        .item(path, definition).ge(value)
                        .buildFilter());
    }

    private void assertLeFilter(PrismObject<UserType> user, ItemName itemName, QName itemType, Object value, boolean expected) throws SchemaException {
        assertFilter(user, itemName, itemType, value, expected,
                (path, definition) -> getPrismContext().queryFor(UserType.class)
                        .item(path, definition).le(value)
                        .buildFilter());
    }

    private void assertGtFilter(PrismObject<UserType> user, ItemName itemName, QName itemType, Object value, boolean expected) throws SchemaException {
        assertFilter(user, itemName, itemType, value, expected,
                (path, definition) -> getPrismContext().queryFor(UserType.class)
                        .item(path, definition).gt(value)
                        .buildFilter());
    }

    private void assertLtFilter(PrismObject<UserType> user, ItemName itemName, QName itemType, Object value, boolean expected) throws SchemaException {
        assertFilter(user, itemName, itemType, value, expected,
                (path, definition) -> getPrismContext().queryFor(UserType.class)
                        .item(path, definition).lt(value)
                        .buildFilter());
    }

    private void assertFilter(PrismObject<UserType> user, ItemName itemName, QName itemType, Object value, boolean expected,
            BiFunction<ItemPath, PrismPropertyDefinition<Integer>, ObjectFilter> filterSupplier) throws SchemaException {
        ItemPath path = ItemPath.create(UserType.F_EXTENSION, itemName);
        PrismPropertyDefinition<Integer> definition = getPrismContext().definitionFactory()
                .createPropertyDefinition(itemName, itemType);
        ObjectFilter filter = filterSupplier.apply(path, definition);
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertEquals("filter " + filter + " produces wrong result for " + itemName +
                " of " + value.getClass() + ": " + value, expected, match);
    }

    private PrismObject<UserType> parseUserJack() throws SchemaException, IOException {
        return PrismTestUtil.parseObject(FILE_USER_JACK_FILTERS);
    }
}
