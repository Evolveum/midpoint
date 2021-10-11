/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
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

import java.io.IOException;
import java.math.BigInteger;
import java.util.function.BiFunction;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.EXTENSION_NUM_ELEMENT;

public class TestObjectQuery extends AbstractPrismTest {

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
        assertGeFilter(user, 42, true);
        assertGeFilter(user, 44, false);
        assertGeFilter(user, 40, true);
        assertGeFilter(user, 42.0, true);
        assertGeFilter(user, 44.0, false);
        assertGeFilter(user, 40.0, true);
        assertGeFilter(user, 42.0f, true);
        assertGeFilter(user, 44.0f, false);
        assertGeFilter(user, 40.0f, true);
        assertGeFilter(user, 42L, true);
        assertGeFilter(user, 44L, false);
        assertGeFilter(user, 40L, true);
        assertGeFilter(user, (short) 42, true);
        assertGeFilter(user, (short) 44, false);
        assertGeFilter(user, (short) 40, true);
        assertGeFilter(user, BigInteger.valueOf(42), true);
        assertGeFilter(user, BigInteger.valueOf(44), false);
        assertGeFilter(user, BigInteger.valueOf(40), true);

        assertLtFilter(user, 42, false);
        assertLtFilter(user, 44, true);
        assertLtFilter(user, 40, false);
        assertLtFilter(user, 42.0, false);
        assertLtFilter(user, 44.0, true);
        assertLtFilter(user, 40.0, false);
        assertLtFilter(user, 42.0f, false);
        assertLtFilter(user, 44.0f, true);
        assertLtFilter(user, 40.0f, false);
        assertLtFilter(user, 42L, false);
        assertLtFilter(user, 44L, true);
        assertLtFilter(user, 40L, false);
        assertLtFilter(user, (short) 42, false);
        assertLtFilter(user, (short) 44, true);
        assertLtFilter(user, (short) 40, false);
        assertLtFilter(user, BigInteger.valueOf(42), false);
        assertLtFilter(user, BigInteger.valueOf(44), true);
        assertLtFilter(user, BigInteger.valueOf(40), false);
    }

    private void assertGeFilter(PrismObject<UserType> user, Object value, boolean expected) throws SchemaException {
        assertFilter(user, value, expected,
                (path, definition) -> getPrismContext().queryFor(UserType.class)
                        .item(path, definition).ge(value)
                        .buildFilter());
    }

    private void assertLtFilter(PrismObject<UserType> user, Object value, boolean expected) throws SchemaException {
        assertFilter(user, value, expected,
                (path, definition) -> getPrismContext().queryFor(UserType.class)
                        .item(path, definition).lt(value)
                        .buildFilter());
    }

    private void assertFilter(PrismObject<UserType> user, Object value, boolean expected,
            BiFunction<ItemPath, PrismPropertyDefinition<Integer>, ObjectFilter> filterSupplier) throws SchemaException {
        ItemPath path = ItemPath.create(UserType.F_EXTENSION, EXTENSION_NUM_ELEMENT);
        PrismPropertyDefinition<Integer> definition = getPrismContext().definitionFactory()
                .createPropertyDefinition(new QName("num"), DOMUtil.XSD_INT);
        ObjectFilter filter = filterSupplier.apply(path, definition);
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertEquals("filter " + filter + " produces wrong result for " + value.getClass() + ": " + value, expected, match);
    }

    private PrismObject<UserType> parseUserJack() throws SchemaException, IOException {
        return PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
    }
}
