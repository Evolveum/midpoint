package com.evolveum.midpoint.prism.query.lang;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.EXTENSION_DATETIME_ELEMENT;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.EXTENSION_NUM_ELEMENT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.function.BiFunction;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.AbstractPrismTest;
import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismInternalTestUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.ObjectType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.impl.match.MatchingRuleRegistryFactory;
import com.evolveum.midpoint.prism.impl.query.FullTextFilterImpl;
import com.evolveum.midpoint.prism.impl.query.lang.PrismQuerySerializerImpl;

import static com.evolveum.midpoint.prism.query.PrismQuerySerialization.NotSupportedException;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.FullTextFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.PrismQueryLanguageParser;
import com.evolveum.midpoint.prism.query.PrismQuerySerialization;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class TestBasicQueryConversions extends AbstractPrismTest {

    public static final File FILE_USER_JACK_FILTERS =
            new File(PrismInternalTestUtil.COMMON_DIR_XML, "user-jack-filters.xml");

    private static final MatchingRuleRegistry MATCHING_RULE_REGISTRY =
            MatchingRuleRegistryFactory.createRegistry();

    private PrismObject<UserType> parseUserJacky() throws SchemaException, IOException {
        return PrismTestUtil.parseObject(FILE_USER_JACK_FILTERS);
    }

    private PrismQueryLanguageParser parser() {
        return PrismTestUtil.getPrismContext().createQueryParser();
    }

    private ObjectFilter parse(String query) throws SchemaException {
        return parser().parseQuery(UserType.class, query);
    }

    private void verify(String query, ObjectFilter original) throws SchemaException, IOException {
        verify(query, original, PrismTestUtil.parseObject(FILE_USER_JACK_FILTERS));
    }

    private void verify(String query, ObjectFilter original, PrismObject<?> user) throws SchemaException {
        ObjectFilter dslFilter = parse(query);
        boolean javaResult = ObjectQuery.match(user, original, MATCHING_RULE_REGISTRY);
        boolean dslResult = ObjectQuery.match(user, dslFilter, MATCHING_RULE_REGISTRY);
        try {
            assertEquals(dslFilter.toString(), original.toString());
            assertEquals(dslResult, javaResult, "Filters do not match.");

            //String javaSerialized = serialize(original);
            String dslSerialized = serialize(dslFilter);

            //assertEquals(javaSerialized, query);
            assertEquals(dslSerialized, query);

        } catch (AssertionError e) {
            throw new AssertionError(e.getMessage() + "for filter: \n    " + query);
        }
    }

    private String serialize(ObjectFilter original) {
        PrismQuerySerialization serialization;
        try {
            serialization = new PrismQuerySerializerImpl().serialize(original);
            return serialization.filterText();
        } catch (NotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testMatchAndFilter() throws SchemaException, IOException {
        ObjectFilter filter =
                getPrismContext().queryFor(UserType.class)
                        .item(UserType.F_GIVEN_NAME).eq("Jack").matchingCaseIgnore()
                        .and().item(UserType.F_FULL_NAME).contains("arr")
                        .buildFilter();
        verify("givenName =[stringIgnoreCase] 'Jack' and fullName contains 'arr'", filter);
    }

    @Test
    public void testEscapings() throws SchemaException, IOException {
        ObjectFilter filter =
                getPrismContext().queryFor(UserType.class)
                        .item(UserType.F_GIVEN_NAME).eq("Jack").matchingCaseIgnore()
                        .and().item(UserType.F_FULL_NAME).contains("'Arr")
                        .buildFilter();
        verify("givenName =[stringIgnoreCase] 'Jack' and fullName contains '\\'Arr'", filter);
    }

    @Test
    public void testPathComparison() throws SchemaException, IOException {
        ObjectFilter dslFilter = parse("fullName not equal givenName");
        boolean match = ObjectQuery.match(parseUserJacky(),dslFilter,MATCHING_RULE_REGISTRY);
        assertTrue(match);
        verify("fullName != givenName", dslFilter);
    }

    @Test
    public void testFullText() throws SchemaException, IOException {
        FullTextFilter filter = FullTextFilterImpl.createFullText("jack");
        ObjectFilter dslFilter = parse(". fullText 'jack'");
        assertEquals(dslFilter.toString(), filter.toString());

    }

    @Test   // MID-4173
    public void testExistsPositive() throws Exception {
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT)
                .item(AssignmentType.F_DESCRIPTION).eq("Assignment 2")
                .buildFilter();
        verify("assignment matches (description = 'Assignment 2')", filter);
    }

    @Test
    public void testMatchSubstringAgainstEmptyItem() throws Exception {
        // jack has no locality
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_LOCALITY).startsWith("C")
                .buildFilter();
        verify("locality startsWith 'C'", filter);
    }


    @Test
    public void testMatchOrFilter() throws Exception {
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).eq("Jack")
                .or().item(UserType.F_GIVEN_NAME).eq("Jackie")
                .buildFilter();

        verify("givenName = 'Jack' or givenName = 'Jackie'", filter);
    }

    @Test
    public void testDontMatchEqualFilter() throws Exception {
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).eq("Jackie")
                .buildFilter();
        verify("givenName = 'Jackie'", filter);
    }

    @Test
    public void testMatchEqualMultivalue() throws Exception {
        MutablePrismPropertyDefinition<?> def = getPrismContext().definitionFactory().createPropertyDefinition(new QName("indexedString"), DOMUtil.XSD_STRING);
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(ItemPath.create(UserType.F_EXTENSION, "indexedString"), def).eq("alpha")
                .buildFilter();
        verify("extension/indexedString = 'alpha'", filter);
    }

    @Test
    public void testMatchEqualNonEmptyAgainstEmptyItem() throws Exception {
        // jack has no locality
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_LOCALITY).eq("some")
                .buildFilter();
        verify("locality = 'some'", filter);
    }

    @Test
    public void testMatchEqualEmptyAgainstEmptyItem() throws Exception {
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
        verify("familyName = 'Sparrow' and fullName contains 'arr' "
                + "and (givenName = 'Jack' or givenName = 'Jackie')", filter);
    }

    @Test
    public void testTypeAndMatch() throws Exception {
        ObjectFilter filter =
                getPrismContext().queryFor(ObjectType.class)
                        .type(UserType.class)
                        .item(UserType.F_FAMILY_NAME).eq("Sparrow")
                        .and().item(UserType.F_FULL_NAME).contains("arr")
                        .and()
                        .block()
                        .item(UserType.F_GIVEN_NAME).eq("Jack")
                        .or().item(UserType.F_GIVEN_NAME).eq("Jackie")
                        .endBlock()
                        .buildFilter();
        verify(". type UserType and familyName = 'Sparrow' and fullName contains 'arr' "
                + "and (givenName = 'Jack' or givenName = 'Jackie')", filter);
    }

    @Test
    public void testPolystringMatchEqualFilter() throws Exception {
        PolyString name = new PolyString("jack", "jack");
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_NAME).eq(name).matchingStrict()
                .buildFilter();
        verify("name matches (orig = 'jack' and norm = 'jack')", filter);
        verify("name matches (norm = 'jack')",
                getPrismContext().queryFor(UserType.class)
                .item(UserType.F_NAME).eq(name).matchingNorm()
                .buildFilter());
        verify("name matches (orig = 'jack')",
                getPrismContext().queryFor(UserType.class)
                .item(UserType.F_NAME).eq(name).matchingOrig()
                .buildFilter());

    }


    @Test   // MID-4173
    public void testExistsNegative() throws Exception {
        PrismObject<UserType> user = parseUserJacky();
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT)
                .item(AssignmentType.F_DESCRIPTION).eq("Assignment NONE")
                .buildFilter();
        verify("assignment matches (description = 'Assignment NONE')",filter,user);
    }

    @Test   // MID-4173
    public void testExistsAnyNegative() throws Exception {
        PrismObject<UserType> user = parseUserJacky();
        user.removeContainer(UserType.F_ASSIGNMENT);
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT)
                .buildFilter();

        verify("assignment exists",filter,user);
    }

    @Test   // MID-4173
    public void testExistsAnyPositive() throws Exception {
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .exists(UserType.F_ASSIGNMENT)
                .buildFilter();
        verify("assignment exists", filter);

    }

    @Test   // MID-4217
    public void testMultiRootPositive() throws Exception {
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION).eq("Assignment 2")
                .buildFilter();
        verify("assignment/description = 'Assignment 2'", filter);
    }

    @Test   // MID-4217
    public void testMultiRootNegative() throws Exception {
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION).eq("Assignment XXXXX")
                .buildFilter();

        verify("assignment/description = 'Assignment XXXXX'", filter);
    }

    @Test   // MID-4217
    public void testRefPositive() throws Exception {
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_ACCOUNT_REF).ref("c0c010c0-d34d-b33f-f00d-aaaaaaaa1113")
                .buildFilter();
        verify("accountRef matches (oid = 'c0c010c0-d34d-b33f-f00d-aaaaaaaa1113')",filter);
    }

    @Test
    public void testOidIn() throws Exception {
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .id("c0c010c0-d34d-b33f-f00d-aaaaaaaa1113", "c0c010c0-d34d-b33f-f00d-aaaaaaaa1114", "c0c010c0-d34d-b33f-f00d-aaaaaaaa1115")
                .buildFilter();

        verify(". inOid ('c0c010c0-d34d-b33f-f00d-aaaaaaaa1113', 'c0c010c0-d34d-b33f-f00d-aaaaaaaa1114', 'c0c010c0-d34d-b33f-f00d-aaaaaaaa1115')",filter);


    }

    @Test   // MID-4217
    public void testRefNegative() throws Exception {
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_ACCOUNT_REF).ref("xxxxxxxxxxxxxx")
                .buildFilter();
        verify("accountRef matches (oid = 'xxxxxxxxxxxxxx')",filter);
    }

    @Test
    public void testRefRelationNegative() throws Exception {
        ObjectFilter filter = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_ACCOUNT_REF).refRelation(new QName("a-relation"))
                .buildFilter();
        verify("accountRef matches (relation = a-relation)", filter);
    }

    @Test // MID-6487
    public void testGtFilter() throws Exception {
        ObjectFilter filter =
                getPrismContext().queryFor(UserType.class)
                        .item(UserType.F_NAME).gt(new PolyString("j")).matchingOrig()
                        .buildFilter();
        verify("name >[polyStringOrig] 'j'",filter);
    }

    @Test // MID-6487
    public void testLtFilter() throws Exception {
        ObjectFilter filter =
                getPrismContext().queryFor(UserType.class)
                        .item(UserType.F_NAME).lt(new PolyString("j")).matchingNorm()
                        .buildFilter();
        verify("name <[polyStringNorm] 'j'",filter);
    }

    @Test // MID-6487
    public void testNumericFilters() throws Exception {
        PrismObject<UserType> user = parseUserJacky();
        assertNumGeFilter(user, 42, true);
        assertNumGeFilter(user, 44, false);
        assertNumGeFilter(user, 40, true);

        assertNumLtFilter(user, 42, false);
        assertNumLtFilter(user, 44, true);
        assertNumLtFilter(user, 40, false);
    }

    @Test // MID-6577
    public void testDateTimeFilters() throws Exception {
        PrismObject<UserType> user = parseUserJacky();
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

    @Test // MID-6601
    public void testOidGtFilter() throws Exception {
        ObjectFilter filter =
                getPrismContext().queryFor(UserType.class)
                        .item(PrismConstants.T_ID).gt("00")
                        .buildFilter();
        verify("# > '00'", filter);
    }

    @Test // MID-6601
    public void testOidSubstringFilter() throws Exception {
        PrismObject<UserType> user = parseUserJacky();
        ObjectFilter filter =
                getPrismContext().queryFor(UserType.class)
                        .item(PrismConstants.T_ID).startsWith("c0c0")
                        .buildFilter();
        boolean match = ObjectQuery.match(user, filter, MATCHING_RULE_REGISTRY);
        AssertJUnit.assertTrue("filter does not match object", match);
    }

    private void assertNumGeFilter(PrismObject<UserType> user, Object value, boolean expected) throws SchemaException, IOException {
        assertGeFilter(user, EXTENSION_NUM_ELEMENT, DOMUtil.XSD_INT, value, expected);
    }

    private void assertNumLtFilter(PrismObject<UserType> user, Object value, boolean expected) throws SchemaException, IOException {
        assertLtFilter(user, EXTENSION_NUM_ELEMENT, DOMUtil.XSD_INT, value, expected);
    }

    private void assertDateTimeGeFilter(PrismObject<UserType> user, Object value, boolean expected) throws SchemaException, IOException {
        assertGeFilter(user, EXTENSION_DATETIME_ELEMENT, DOMUtil.XSD_DATETIME, value, expected);
    }

    private void assertDateTimeLeFilter(PrismObject<UserType> user, Object value, boolean expected) throws SchemaException, IOException {
        assertLeFilter(user, EXTENSION_DATETIME_ELEMENT, DOMUtil.XSD_DATETIME, value, expected);
    }

    private void assertDateTimeGtFilter(PrismObject<UserType> user, Object value, boolean expected) throws SchemaException, IOException {
        assertGtFilter(user, EXTENSION_DATETIME_ELEMENT, DOMUtil.XSD_DATETIME, value, expected);
    }

    private void assertDateTimeLtFilter(PrismObject<UserType> user, Object value, boolean expected) throws SchemaException, IOException {
        assertLtFilter(user, EXTENSION_DATETIME_ELEMENT, DOMUtil.XSD_DATETIME, value, expected);
    }


    private String toText(Object value) {
        if (value instanceof XMLGregorianCalendar) {
            return new StringBuilder().append("'").append(value.toString()).append("'").toString();
        }
        return value.toString();
    }

    private void assertGeFilter(PrismObject<UserType> user, ItemName itemName, QName itemType, Object value, boolean expected) throws SchemaException, IOException {
        ObjectFilter filter = createExtensionFilter( itemName, itemType,
                (path, definition) -> getPrismContext().queryFor(UserType.class)
                        .item(path, definition).ge(value)
                        .buildFilter());
        verify("extension/" + itemName.getLocalPart() + " >= " +  toText(value), filter);
    }


    private void assertLeFilter(PrismObject<UserType> user, ItemName itemName, QName itemType, Object value, boolean expected) throws SchemaException, IOException {
        ObjectFilter filter = createExtensionFilter( itemName, itemType,
                (path, definition) -> getPrismContext().queryFor(UserType.class)
                        .item(path, definition).le(value)
                        .buildFilter());
        verify("extension/" + itemName.getLocalPart() + " <= " +  toText(value), filter);
    }

    private void assertGtFilter(PrismObject<UserType> user, ItemName itemName, QName itemType, Object value, boolean expected) throws SchemaException, IOException {
        ObjectFilter filter =createExtensionFilter( itemName, itemType,
                (path, definition) -> getPrismContext().queryFor(UserType.class)
                        .item(path, definition).gt(value)
                        .buildFilter());
        verify("extension/" + itemName.getLocalPart() + " > " +  toText(value), filter);
    }

    private void assertLtFilter(PrismObject<UserType> user, ItemName itemName, QName itemType, Object value, boolean expected) throws SchemaException, IOException {
        ObjectFilter filter = createExtensionFilter( itemName, itemType,
                (path, definition) -> getPrismContext().queryFor(UserType.class)
                        .item(path, definition).lt(value)
                        .buildFilter());
        verify("extension/" + itemName.getLocalPart() + " < " +  toText(value), filter);
    }

    private ObjectFilter createExtensionFilter(ItemName itemName, QName itemType, BiFunction<ItemPath, PrismPropertyDefinition<Integer>, ObjectFilter> filterSupplier) throws SchemaException {
        ItemPath path = ItemPath.create(UserType.F_EXTENSION, itemName);
        PrismPropertyDefinition<Integer> definition = getPrismContext().definitionFactory()
                .createPropertyDefinition(itemName, itemType);
        ObjectFilter filter = filterSupplier.apply(path, definition);
        return filter;
    }


}
