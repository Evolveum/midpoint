package com.evolveum.midpoint.prism.query.lang;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
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
import com.evolveum.midpoint.prism.impl.query.lang.PrismQueryLanguageParser;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class TestBasicQueryConversions extends AbstractPrismTest {

    public static final File FILE_USER_JACK_FILTERS =
            new File(PrismInternalTestUtil.COMMON_DIR_XML, "user-jack-filters.xml");

    private static final MatchingRuleRegistry MATCHING_RULE_REGISTRY =
            MatchingRuleRegistryFactory.createRegistry();

    private PrismObject<UserType> parseUserJack() throws SchemaException, IOException {
        return PrismTestUtil.parseObject(FILE_USER_JACK_FILTERS);
    }

    private PrismQueryLanguageParser parser() {
        return PrismQueryLanguageParser.create(PrismTestUtil.getPrismContext());
    }

    private ObjectFilter parse(String query) throws SchemaException {
        return parser().parseQuery(UserType.class, query);
    }

    private void verify(String query, ObjectFilter original) throws SchemaException, IOException  {
        PrismObject<UserType> user = parseUserJack();
        ObjectFilter dslFilter = parse(query);
        assertEquals(ObjectQuery.match(user, dslFilter, MATCHING_RULE_REGISTRY), ObjectQuery.match(user, original, MATCHING_RULE_REGISTRY));
        assertEquals(dslFilter.toString(), original.toString());
    }

    @Test
    public void testMatchAndFilter() throws SchemaException, IOException {
        ObjectFilter filter =
                getPrismContext().queryFor(UserType.class)
                        .item(UserType.F_GIVEN_NAME).eq("Jack").matchingCaseIgnore()
                        .and().item(UserType.F_FULL_NAME).contains("arr")
                        .buildFilter();
        verify("givenName =[stringIgnoreCase] \"Jack\" and fullName contains \"arr\"", filter);
    }

    @Test   // MID-4173
    public void testExistsPositive() throws Exception {
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT)
                .item(AssignmentType.F_DESCRIPTION).eq("Assignment 2")
                .buildFilter();
        verify("assignment matches ( description = \"Assignment 2\"", filter);
    }

    @Test
    public void testMatchSubstringAgainstEmptyItem() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        // jack has no locality
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_LOCALITY).startsWith("C")
                .buildFilter();
        verify("locality startsWith \"C\"", filter);
    }


    @Test
    public void testMatchOrFilter() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).eq("Jack")
                .or().item(UserType.F_GIVEN_NAME).eq("Jackie")
                .buildFilter();

        verify("givenName = \"Jack\" or givenName = \"Jackie\"", filter);
    }

    @Test
    public void testDontMatchEqualFilter() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).eq("Jackie")
                .buildFilter();
        verify("givenName = \"Jackie\"", filter);
    }

    @Test
    public void testMatchEqualMultivalue() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        MutablePrismPropertyDefinition<?> def = getPrismContext().definitionFactory().createPropertyDefinition(new QName("indexedString"), DOMUtil.XSD_STRING);
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(ItemPath.create(UserType.F_EXTENSION, "indexedString"), def).eq("alpha")
                .buildFilter();
        verify("extension/indexedString = \"alpha\" ", filter);
    }

    @Test
    public void testMatchEqualNonEmptyAgainstEmptyItem() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        // jack has no locality
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_LOCALITY).eq("some")
                .buildFilter();
        verify("locality = \"some\"", filter);
    }

    @Test
    public void testMatchEqualEmptyAgainstEmptyItem() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        // jack has no locality
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_LOCALITY).isNull()
                .buildFilter();
        verify("locality not exists", filter);
    }

    @Test
    public void testMatchEqualEmptyAgainstNonEmptyItem() throws Exception {
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_NAME).isNull()
                .buildFilter();
        verify("name not exists", filter);
    }

    @Test
    public void testComplexMatch() throws Exception {
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
        verify("familyName = \"Sparrow\" and fullName contains \"arr\" "
                + "and ( givenName = \"Jack\" or givenName = \"Jackie\" ) ", filter);
    }

    @Test
    public void testPolystringMatchEqualFilter() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        PolyString name = new PolyString("jack", "jack");
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_NAME).eq(name)
                .buildFilter();
        verify("name matches (orig = \"jack\" and norm = \"jack\")", filter);
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
        verify("assignment exists", filter);

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


        verify("accountRef matches ( targetOid = \"xxxxxxxxxxxxxx\")",filter);
    }

    @Test
    public void testRefRelationNegative() throws Exception {
        PrismObject<UserType> user = parseUserJack();
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_ACCOUNT_REF).refRelation(new QName("a-relation"))
                .buildFilter();
        verify("accountRef matches (relation = a-relation)", filter);
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

}
