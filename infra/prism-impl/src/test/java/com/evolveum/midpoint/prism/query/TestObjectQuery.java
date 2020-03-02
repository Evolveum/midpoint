/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.AbstractPrismTest;
import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismInternalTestUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.impl.match.MatchingRuleRegistryFactory;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.DOMUtil;

public class TestObjectQuery extends AbstractPrismTest {

    private static MatchingRuleRegistry matchingRuleRegistry =
            MatchingRuleRegistryFactory.createRegistry();

    @Test
    public void testMatchAndFilter() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
        ObjectFilter filter =
                getPrismContext().queryFor(UserType.class)
                        .item(UserType.F_GIVEN_NAME).eq("Jack").matchingCaseIgnore()
                        .and().item(UserType.F_FULL_NAME).contains("arr")
                        .buildFilter();
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertTrue("filter does not match object", match);
    }

    @Test
    public void testMatchOrFilter() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).eq("Jack")
                .or().item(UserType.F_GIVEN_NAME).eq("Jackie")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertTrue("filter does not match object", match);
    }

    @Test
    public void testDontMatchEqualFilter() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).eq("Jackie")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }

    @Test
    public void testMatchEqualMultivalue() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
        MutablePrismPropertyDefinition<?> def = getPrismContext().definitionFactory().createPropertyDefinition(new QName("indexedString"), DOMUtil.XSD_STRING);
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(ItemPath.create(UserType.F_EXTENSION, "indexedString"), def).eq("alpha")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertTrue("filter does not match object", match);
    }

    @Test
    public void testMatchEqualNonEmptyAgainstEmptyItem() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
        // jack has no locality
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_LOCALITY).eq("some")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }

    @Test
    public void testMatchEqualEmptyAgainstEmptyItem() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
        // jack has no locality
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_LOCALITY).isNull()
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertTrue("filter does not match object", match);
    }

    @Test
    public void testMatchEqualEmptyAgainstNonEmptyItem() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
        // jack has no locality
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_NAME).isNull()
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }

    @Test
    public void testComplexMatch() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
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
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertTrue("filter does not match object", match);
    }

    @Test
    public void testPolystringMatchEqualFilter() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
        PolyString name = new PolyString("jack", "jack");
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_NAME).eq(name)
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertTrue("filter does not match object", match);
    }

    @Test   // MID-4120
    public void testMatchSubstringAgainstEmptyItem() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
        // jack has no locality
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_LOCALITY).startsWith("C")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }

    @Test   // MID-4173
    public void testExistsNegative() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT)
                .item(AssignmentType.F_DESCRIPTION).eq("Assignment NONE")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }

    @Test   // MID-4173
    public void testExistsPositive() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT)
                .item(AssignmentType.F_DESCRIPTION).eq("Assignment 2")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertTrue("filter does not match object, but it should", match);
    }

    @Test   // MID-4173
    public void testExistsAnyNegative() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
        user.removeContainer(UserType.F_ASSIGNMENT);
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT)
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }

    @Test   // MID-4173
    public void testExistsAnyPositive() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT)
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertTrue("filter does not match object, but it should", match);
    }

    @Test   // MID-4217
    public void testMultiRootPositive() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION).eq("Assignment 2")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertTrue("filter does not match object, but it should", match);
    }

    @Test   // MID-4217
    public void testMultiRootNegative() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION).eq("Assignment XXXXX")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }

    @Test   // MID-4217
    public void testRefPositive() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_ACCOUNT_REF).ref("c0c010c0-d34d-b33f-f00d-aaaaaaaa1113")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertTrue("filter does not match object, but it should", match);
    }

    @Test   // MID-4217
    public void testRefNegative() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_ACCOUNT_REF).ref("xxxxxxxxxxxxxx")
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }

    @Test
    public void testRefRelationNegative() throws Exception {
        PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_ACCOUNT_REF).refRelation(new QName("a-relation"))
                .buildFilter();
        boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
        AssertJUnit.assertFalse("filter matches object, but it should not", match);
    }
}
