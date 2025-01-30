/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.PrismConstants.T_OBJECT_REFERENCE;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_OWNER_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType.F_ROLE_MEMBERSHIP_REF;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.SchemaDebugUtil;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ParentPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class TestQueryConverter extends AbstractUnitTest {

    private static final File TEST_DIR = new File("src/test/resources/queryconverter");

    private static final String NS_EXTENSION = "http://midpoint.evolveum.com/xml/ns/test/extension";
    private static final String NS_ICFS = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3";
    private static final String NS_BLA = "http://midpoint.evolveum.com/blabla";

    private static final QName intExtensionDefinition = new QName(NS_EXTENSION, "intType");
    private static final QName stringExtensionDefinition = new QName(NS_EXTENSION, "stringType");
    private static final QName fooBlaDefinition = new QName(NS_BLA, "foo");
    private static final QName ICF_NAME = new QName(NS_ICFS, "name");

    private static final File FILTER_AND_GENERIC_FILE = new File(TEST_DIR, "filter-and-generic.xml");
    private static final File FILTER_ACCOUNT_FILE = new File(TEST_DIR, "filter-account.xml");
    private static final File FILTER_ACCOUNT_ATTRIBUTES_RESOURCE_REF_FILE = new File(TEST_DIR,
            "filter-account-by-attributes-and-resource-ref.xml");
    private static final File FILTER_ACCOUNT_ATTRIBUTES_RESOURCE_REF_NO_NS_FILE = new File(TEST_DIR,
            "filter-account-by-attributes-and-resource-ref-no-ns.xml");
    private static final File FILTER_OR_COMPOSITE = new File(TEST_DIR, "filter-or-composite.xml");
    private static final File FILTER_CONNECTOR_BY_TYPE_FILE = new File(TEST_DIR, "filter-connector-by-type.xml");
    private static final File FILTER_BY_TYPE_FILE = new File(TEST_DIR, "filter-by-type.xml");
    private static final File FILTER_EQUALS_WITH_TYPED_VALUE = new File(TEST_DIR, "filter-equals-with-typed-value.xml");
    private static final File FILTER_NOT_EQUALS_NULL = new File(TEST_DIR, "filter-not-equals-null.xml");

    private static final ItemName EXT_LONG_TYPE = new ItemName(NS_EXTENSION, "longType");
    private static final ItemPath EXT_LONG_TYPE_PATH = ItemPath.create(UserType.F_EXTENSION, EXT_LONG_TYPE);

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        SchemaDebugUtil.initializePrettyPrinter();
        resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void testAccountFilter() throws Exception {
        SearchFilterType filterType = unmarshalFilter(FILTER_ACCOUNT_FILE);
        ObjectQuery query = toObjectQuery(ShadowType.class, filterType);
        displayQuery(query);

        assertNotNull(query);

        ObjectFilter filter = query.getFilter();
        PrismAsserts.assertAndFilter(filter, 2);

        ObjectFilter first = getFilterCondition(filter, 0);
        PrismAsserts.assertEqualsFilter(first, ShadowType.F_TAG, PrimitiveType.XSD_STRING, ShadowType.F_TAG);
        PrismAsserts.assertEqualsFilterValue((EqualFilter<?>) first, "abc");

        ObjectFilter second = getFilterCondition(filter, 1);
        PrismAsserts.assertEqualsFilter(second, ShadowType.F_NAME, PolyStringType.COMPLEX_TYPE, ShadowType.F_NAME);
        PrismAsserts.assertEqualsFilterValue((EqualFilter<?>) second, createPolyString("someName"));

        QueryType convertedQueryType = toQueryType(query);
        System.out.println("Re-converted query type");
        System.out.println(convertedQueryType.debugDump());

        SearchFilterType convertedFilterType = convertedQueryType.getFilter();
        MapXNode xFilter = convertedFilterType.serializeToXNode(getPrismContext());
        PrismAsserts.assertSize(xFilter, 1);
        PrismAsserts.assertSubnode(xFilter, AndFilter.ELEMENT_NAME, MapXNode.class);
        MapXNode xAndMap = (MapXNode) xFilter.get(AndFilter.ELEMENT_NAME);
        PrismAsserts.assertSize(xAndMap, 1);
        PrismAsserts.assertSubnode(xAndMap, EqualFilter.ELEMENT_NAME, ListXNode.class);
        ListXNode xEqualsList = (ListXNode) xAndMap.get(EqualFilter.ELEMENT_NAME);
        PrismAsserts.assertSize(xEqualsList, 2);
    }

    @Test
    public void testAccountQueryAttributesAndResource() throws Exception {
        SearchFilterType filterType = unmarshalFilter(FILTER_ACCOUNT_ATTRIBUTES_RESOURCE_REF_FILE);
        ObjectQuery query = toObjectQuery(ShadowType.class, filterType);
        displayQuery(query);

        assertNotNull(query);

        ObjectFilter filter = query.getFilter();
        PrismAsserts.assertAndFilter(filter, 2);

        ObjectFilter first = getFilterCondition(filter, 0);
        PrismAsserts.assertRefFilter(first, ShadowType.F_RESOURCE_REF, ObjectReferenceType.COMPLEX_TYPE, ShadowType.F_RESOURCE_REF);
        assertRefFilterValue((RefFilter) first, "aae7be60-df56-11df-8608-0002a5d5c51b");

        ObjectFilter second = getFilterCondition(filter, 1);
        PrismAsserts.assertEqualsFilter(second, ICF_NAME, DOMUtil.XSD_STRING, ItemPath.create(ShadowType.F_ATTRIBUTES,
                ICF_NAME));
        PrismAsserts.assertEqualsFilterValue((EqualFilter<?>) second, "uid=jbond,ou=People,dc=example,dc=com");

        toQueryType(query);
    }

    @Test
    public void testAccountQueryAttributesAndResourceNoNs() throws Exception {
        SearchFilterType filterType = unmarshalFilter(FILTER_ACCOUNT_ATTRIBUTES_RESOURCE_REF_NO_NS_FILE);
        ObjectQuery query = toObjectQuery(ShadowType.class, filterType);
        displayQuery(query);

        assertNotNull(query);

        ObjectFilter filter = query.getFilter();
        PrismAsserts.assertAndFilter(filter, 2);

        ObjectFilter first = getFilterCondition(filter, 0);
        PrismAsserts.assertRefFilter(first, ShadowType.F_RESOURCE_REF, ObjectReferenceType.COMPLEX_TYPE,
                ShadowType.F_RESOURCE_REF);
        assertRefFilterValue((RefFilter) first, "aae7be60-df56-11df-8608-0002a5d5c51b");

        ObjectFilter second = getFilterCondition(filter, 1);
        PrismAsserts.assertEqualsFilter(second, ICF_NAME, DOMUtil.XSD_STRING, ItemPath.create("attributes", "name"));

        toQueryType(query);
    }

    @Test
    public void testAccountQueryCompositeOr() throws Exception {
        SearchFilterType filterType = unmarshalFilter(FILTER_OR_COMPOSITE);
        ObjectQuery query = toObjectQuery(ShadowType.class, filterType);
        displayQuery(query);

        assertNotNull(query);

        ObjectFilter filter = query.getFilter();
        PrismAsserts.assertOrFilter(filter, 4);

        ObjectFilter first = getFilterCondition(filter, 0);
        PrismAsserts.assertEqualsFilter(first, ShadowType.F_INTENT, DOMUtil.XSD_STRING, ShadowType.F_INTENT);
        PrismAsserts.assertEqualsFilterValue((EqualFilter<?>) first, "some account type");

        ObjectFilter second = getFilterCondition(filter, 1);
        PrismAsserts.assertEqualsFilter(second, fooBlaDefinition, DOMUtil.XSD_STRING, ItemPath.create(
                ShadowType.F_ATTRIBUTES, fooBlaDefinition));
        PrismAsserts.assertEqualsFilterValue((EqualFilter<?>) second, "foo value");

        ObjectFilter third = getFilterCondition(filter, 2);
        PrismAsserts.assertEqualsFilter(third, stringExtensionDefinition, DOMUtil.XSD_STRING, ItemPath.create(
                ShadowType.F_EXTENSION, stringExtensionDefinition));
        PrismAsserts.assertEqualsFilterValue((EqualFilter<?>) third, "uid=test,dc=example,dc=com");

        ObjectFilter forth = getFilterCondition(filter, 3);
        PrismAsserts.assertRefFilter(forth, ShadowType.F_RESOURCE_REF, ObjectReferenceType.COMPLEX_TYPE,
                ShadowType.F_RESOURCE_REF);
        assertRefFilterValue((RefFilter) forth, "d0db5be9-cb93-401f-b6c1-86ffffe4cd5e");

        toQueryType(query);
    }

    @Test
    public void testConnectorQuery() throws Exception {
        SearchFilterType filterType = PrismTestUtil.parseAtomicValue(FILTER_CONNECTOR_BY_TYPE_FILE, SearchFilterType.COMPLEX_TYPE);
        ObjectQuery query;
        try {
            query = getQueryConverter().createObjectQuery(ConnectorType.class, filterType);
            displayQuery(query);

            assertNotNull(query);
            ObjectFilter filter = query.getFilter();
            PrismAsserts.assertEqualsFilter(query.getFilter(), ConnectorType.F_CONNECTOR_TYPE, DOMUtil.XSD_STRING,
                    ConnectorType.F_CONNECTOR_TYPE);
            PrismAsserts.assertEqualsFilterValue((EqualFilter<?>) filter, "org.identityconnectors.ldap.LdapConnector");

            QueryType convertedQueryType = toQueryType(query);
            displayQueryType(convertedQueryType);
        } catch (Exception ex) {
            logger.error("Error while converting query: {}", ex.getMessage(), ex);
            throw ex;
        }

    }

    private PrismObjectDefinition<UserType> getUserDefinition() {
        return getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
    }

    @Test
    public void testTypeFilterQuery() throws Exception {
        SearchFilterType filterType = PrismTestUtil.parseAtomicValue(FILTER_BY_TYPE_FILE, SearchFilterType.COMPLEX_TYPE);
        ObjectQuery query;
        try {
            query = getQueryConverter().createObjectQuery(ConnectorType.class, filterType);
            displayQuery(query);

            assertNotNull(query);
            ObjectFilter filter = query.getFilter();
            assertTrue("filter is not an instance of type filter", filter instanceof TypeFilter);

            TypeFilter typeFilter = (TypeFilter) filter;
            assertEquals(typeFilter.getType(), UserType.COMPLEX_TYPE);
            assertNotNull("filter in type filter must not be null", typeFilter.getFilter());
            ItemPath namePath = UserType.F_NAME;
            PrismPropertyDefinition<?> ppd = getUserDefinition().findPropertyDefinition(namePath);
            PrismAsserts.assertEqualsFilter(typeFilter.getFilter(), ppd.getItemName(), ppd.getTypeName(), namePath);
            PrismAsserts.assertEqualsFilterValue((EqualFilter<?>) typeFilter.getFilter(), PrismTestUtil.createPolyString("some name identifier"));

            QueryType convertedQueryType = toQueryType(query);
            displayQueryType(convertedQueryType);
        } catch (Exception ex) {
            logger.error("Error while converting query: {}", ex.getMessage(), ex);
            throw ex;
        }

    }

    @Test
    public void testShadowKindTypeQuery() throws Exception {
        ObjectQuery query;
        try {
            ObjectFilter kindFilter = getPrismContext().queryFor(ShadowType.class)
                    .item(ShadowType.F_KIND).eq(ShadowKindType.ACCOUNT)
                    .buildFilter();
            query = getQueryFactory().createQuery(kindFilter);
            assertNotNull(query);
            ObjectFilter filter = query.getFilter();
            assertTrue("filter is not an instance of type filter", filter instanceof EqualFilter);

            EqualFilter<?> typeFilter = (EqualFilter<?>) filter;
            PrismPropertyValue<?> singleValue = typeFilter.getSingleValue();
            assertThat(singleValue).isNotNull();
            assertEquals(singleValue.getRealValue(), ShadowKindType.ACCOUNT);

            QueryType convertedQueryType = toQueryType(query);

            toObjectQuery(ShadowType.class, convertedQueryType);

            displayQueryType(convertedQueryType);
        } catch (Exception ex) {
            logger.error("Error while converting query: {}", ex.getMessage(), ex);
            throw ex;
        }

    }

    private void assertRefFilterValue(RefFilter filter, String oid) {
        List<? extends PrismValue> values = filter.getValues();
        assertThat(values)
                .isNotNull()
                .hasSize(1);
        assertTrue(values.get(0) instanceof PrismReferenceValue);
        PrismReferenceValue val = (PrismReferenceValue) values.get(0);

        assertEquals(oid, val.getOid());
    }

    private ObjectQuery toObjectQuery(Class<? extends Containerable> type, QueryType queryType) throws Exception {
        return getQueryConverter().createObjectQuery(type, queryType);
    }

    private QueryConverter getQueryConverter() {
        return getPrismContext().getQueryConverter();
    }

    private ObjectQuery toObjectQuery(Class<? extends Containerable> type, SearchFilterType filterType) throws Exception {
        return getQueryConverter().createObjectQuery(type, filterType);
    }

    private QueryType toQueryType(ObjectQuery query) throws Exception {
        return getQueryConverter().createQueryType(query);
    }

    private QueryType toQueryType(String xml) throws SchemaException {
        return PrismTestUtil.parseAtomicValue(xml, QueryType.COMPLEX_TYPE);
    }

    private QueryType toQueryTypeCompat(String xml) throws SchemaException {
        return PrismTestUtil.parseAtomicValueCompat(xml, QueryType.COMPLEX_TYPE);
    }

    private String toXml(QueryType q1jaxb) throws SchemaException {
        return getPrismContext().xmlSerializer().serializeRealValue(q1jaxb, SchemaConstantsGenerated.Q_QUERY);
    }

    @Test
    public void testGenericQuery() throws Exception {
        SearchFilterType queryType = unmarshalFilter(FILTER_AND_GENERIC_FILE);
        ObjectQuery query = toObjectQuery(GenericObjectType.class, queryType);

        displayQuery(query);

        // check parent filter
        assertNotNull(query);
        ObjectFilter filter = query.getFilter();
        PrismAsserts.assertAndFilter(filter, 2);
        // check first condition
        ObjectFilter first = getFilterCondition(filter, 0);
        PrismAsserts.assertEqualsFilter(first, GenericObjectType.F_NAME, PolyStringType.COMPLEX_TYPE, GenericObjectType.F_NAME);
        PrismAsserts.assertEqualsFilterValue((EqualFilter<?>) first, createPolyString("generic object"));
        // check second condition
        ObjectFilter second = getFilterCondition(filter, 1);
        PrismAsserts.assertEqualsFilter(second, intExtensionDefinition, DOMUtil.XSD_INT, ItemPath.create(
                ObjectType.F_EXTENSION, new QName(NS_EXTENSION, "intType")));
        PrismAsserts.assertEqualsFilterValue((EqualFilter<?>) second, 123);

        QueryType convertedQueryType = toQueryType(query);
        assertNotNull("Re-serialized query is null ", convertedQueryType);
        assertNotNull("Filter in re-serialized query must not be null.", convertedQueryType.getFilter());

        displayQueryType(convertedQueryType);
    }

    @Test
    public void testUserQuery() throws Exception {
        File[] userQueriesToTest = new File[] { new File(TEST_DIR, "filter-user-by-fullName.xml"),
                new File(TEST_DIR, "filter-user-by-name.xml"),
                new File(TEST_DIR, "filter-user-substring-fullName.xml"),
                new File(TEST_DIR, "filter-user-substring-employeeType.xml"),
                new File(TEST_DIR, "filter-user-substring-expression.xml"),
                new File(TEST_DIR, "filter-user-substring-anchor-start-end-expression.xml")
        };
        // prismContext.silentMarshalObject(queryTypeNew, LOGGER);
        for (File file : userQueriesToTest) {
            SearchFilterType filterType = PrismTestUtil.parseAtomicValue(file, SearchFilterType.COMPLEX_TYPE);
            logger.info("===[ query type parsed ]===");
            ObjectQuery query;
            try {
                query = getQueryConverter().createObjectQuery(UserType.class, filterType);
                logger.info("query converted: ");

                logger.info("QUERY DUMP: {}", query.debugDump());
                logger.info("QUERY Pretty print: {}", query);
                System.out.println("QUERY Pretty print: " + query);

                getQueryConverter().createQueryType(query);
            } catch (Exception ex) {
                logger.error("Error while converting query: {}", ex.getMessage(), ex);
                throw ex;
            }
        }
    }

    @Test
    public void testConvertQueryNullFilter() throws Exception {
        ObjectQuery query = getQueryFactory().createQuery(getQueryFactory().createPaging(0, 10));
        QueryType queryType = getQueryConverter().createQueryType(query);

        assertNotNull(queryType);
        assertNull(queryType.getFilter());
        PagingType paging = queryType.getPaging();
        assertNotNull(paging);
        assertEquals(Integer.valueOf(0), paging.getOffset());
        assertEquals(Integer.valueOf(10), paging.getMaxSize());

    }

    @NotNull
    public QueryFactory getQueryFactory() {
        return getPrismContext().queryFactory();
    }

    private void checkQuery(Class<? extends Containerable> objectClass, ObjectQuery q1object, String q2xml) throws Exception {
        // step 1 (serialization of Q1 + comparison)
        displayText("Query 1:");
        displayQuery(q1object);
        QueryType q1jaxb = toQueryType(q1object);
        displayQueryType(q1jaxb);
        String q1xml = toXml(q1jaxb);
        displayQueryXml(q1xml);

        // step 2 (parsing of Q2 + comparison)
        displayText("Query 2:");
        displayQueryXml(q2xml);
        QueryType q2jaxb = toQueryType(q2xml);
        displayQueryType(q2jaxb);
        ObjectQuery q2object = toObjectQuery(objectClass, q2jaxb);
        assertEquals("Reparsed query is not as original one (via toString)", q1object.toString(), q2object.toString()); // primitive way of comparing parsed queries
        assertTrue("Reparsed query is not as original one (via equivalent):\nq1=" + q1object + "\nq2=" + q2object, q1object.equivalent(q2object));
    }

    private void checkQueryRoundtrip(Class<? extends Containerable> objectClass, ObjectQuery q1object, String q2xml) throws Exception {
        checkQuery(objectClass, q1object, q2xml);
        String q1xml = toXml(toQueryType(q1object));
        ObjectQuery q2object = toObjectQuery(objectClass, toQueryType(q2xml));
        checkQuery(objectClass, q2object, q1xml);
    }

    private void checkQueryRoundtripFile(Class<? extends Containerable> objectClass, ObjectQuery query) throws Exception {
        String fileName = TEST_DIR + "/" + getTestNameShort() + ".xml";
        checkQueryRoundtrip(objectClass, query,
                FileUtils.readFileToString(new File(fileName), StandardCharsets.UTF_8));
    }

    @Test
    public void test100All() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(UserType.class).all().build();
        checkQueryRoundtripFile(UserType.class, q);
    }

    @Test
    public void test110None() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(UserType.class).none().build();
        checkQueryRoundtripFile(UserType.class, q);
    }

    @Test
    public void test120Undefined() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(UserType.class).undefined().build();
        checkQueryRoundtripFile(UserType.class, q);
    }

    @Test
    public void test200Equal() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_NAME).eqPoly("some-name", "somename").matchingOrig()
                .build();
        checkQueryRoundtripFile(UserType.class, q);
    }

    @Test
    public void test210EqualMultiple() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_SUBTYPE).eq("STD", "TEMP")
                .build();
        checkQueryRoundtripFile(UserType.class, q);
    }

    @Test
    public void test220EqualRightHandItem() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_EMPLOYEE_NUMBER).eq().item(UserType.F_COST_CENTER)
                .build();
        checkQueryRoundtripFile(UserType.class, q);
    }

    @Test
    public void test300Greater() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_COST_CENTER).gt("100000")
                .build();
        checkQueryRoundtripFile(UserType.class, q);
    }

    @Test
    public void test305GreaterLesserMatchingNorm() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_NAME).ge(new PolyString("00", "00")).matchingNorm()
                .and().item(UserType.F_NAME).lt(new PolyString("0a", "0a")).matchingNorm()
                .build();
        checkQueryRoundtripFile(UserType.class, q);
    }

    @Test
    public void test305GreaterOrEqual() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_COST_CENTER).ge("100000")
                .build();
        checkQueryRoundtripFile(UserType.class, q);
    }

    @Test
    public void test310AllComparisons() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_COST_CENTER).gt("100000")
                .and().item(UserType.F_COST_CENTER).lt("999999")
                .or()
                .item(UserType.F_COST_CENTER).ge("X100")
                .and().item(UserType.F_COST_CENTER).le("X999")
                .build();
        checkQueryRoundtripFile(UserType.class, q);
    }

    @Test
    public void test350Substring() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_SUBTYPE).contains("A")
                .or().item(UserType.F_SUBTYPE).startsWith("B")
                .or().item(UserType.F_SUBTYPE).endsWith("C")
                .or().item(UserType.F_NAME).startsWithPoly("john", "john").matchingOrig()
                .build();
        checkQueryRoundtripFile(UserType.class, q);
    }

    @Test
    public void test360Ref() throws Exception {
        // we test only parsing here, as there are more serialized forms used here
        ObjectQuery q1object = getPrismContext().queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref("oid1")
                .or().item(ShadowType.F_RESOURCE_REF).ref("oid2", ResourceType.COMPLEX_TYPE)
                .or().item(ShadowType.F_RESOURCE_REF).ref("oid3")
                .or().item(ShadowType.F_RESOURCE_REF).ref("oid4", ResourceType.COMPLEX_TYPE)
                .build();
        q1object.getFilter().checkConsistence(true);

        String q2xml = FileUtils.readFileToString(
                new File(TEST_DIR + "/" + getTestNameShort() + ".xml"),
                StandardCharsets.UTF_8);
        displayQueryXml(q2xml);
        QueryType q2jaxb = toQueryType(q2xml);
        displayQueryType(q2jaxb);
        ObjectQuery q2object = toObjectQuery(ShadowType.class, q2jaxb);

        System.out.println("q1object:\n" + q1object.debugDump(1));
        System.out.println("q2object:\n" + q2object.debugDump(1));

        assertEquals("Reparsed query is not as original one (via toString)", q1object.toString(), q2object.toString()); // primitive way of comparing parsed queries

        if (!q1object.equivalent(q2object)) {
            AssertJUnit.fail("Reparsed query is not as original one (via equivalent):\nq1=" + q1object + "\nq2=" + q2object);
        }
    }

    @Test
    public void test365RefTwoWay() throws Exception {
        PrismReferenceValue reference3 = getPrismContext().itemFactory().createReferenceValue("oid3", ResourceType.COMPLEX_TYPE).relation(new QName("test"));
        PrismReferenceValue reference4 = getPrismContext().itemFactory().createReferenceValue("oid4", ResourceType.COMPLEX_TYPE).relation(new QName("test"));
        ObjectQuery q = getPrismContext().queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref("oid1")
                .or().item(ShadowType.F_RESOURCE_REF).ref("oid2", ResourceType.COMPLEX_TYPE)
                .or().item(ShadowType.F_RESOURCE_REF).ref(reference3, reference4)
                .build();
        checkQueryRoundtripFile(ShadowType.class, q);
    }

    @Test
    public void test400OrgFilterRoot() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(OrgType.class).isRoot().build();
        checkQueryRoundtripFile(OrgType.class, q);
    }

    @Test
    public void test410OrgFilterSubtree() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(OrgType.class).isChildOf("111").build();
        checkQueryRoundtripFile(OrgType.class, q);
    }

    @Test
    public void test420OrgFilterDirect() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(OrgType.class).isDirectChildOf("222").build();
        checkQueryRoundtripFile(OrgType.class, q);
    }

    @Test
    public void test430OrgFilterDefaultScope() throws Exception {
        // default scope is SUBTREE
        String queryXml = "<?xml version='1.0'?><query><filter><org>\n" +
                "        <orgRef>\n" +
                "            <oid>333</oid>\n" +
                "        </orgRef>\n" +
                "    </org></filter></query>";
        QueryType queryJaxb = toQueryType(queryXml);
        displayQueryType(queryJaxb);
        ObjectQuery query = toObjectQuery(OrgType.class, queryJaxb);
        displayQuery(query);

        ObjectQuery expectedQuery = getPrismContext().queryFor(OrgType.class).isChildOf("333").build();
        assertEquals("Parsed query is wrong", expectedQuery.toString(), query.toString()); // primitive way of comparing queries

        // now reserialize the parsed query and compare with XML - the XML contains explicit scope=SUBTREE (as this is set when parsing original queryXml)
        checkQueryRoundtripFile(OrgType.class, query);
    }

    @Test
    public void test500InOid() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(UserType.class)
                .id("oid1", "oid2", "oid3")
                .build();
        checkQueryRoundtripFile(UserType.class, q);
    }

    @Test
    public void test505InOidEmpty() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(UserType.class)
                .id(new String[0])
                .build();
        checkQueryRoundtripFile(UserType.class, q);
    }

    @Test
    public void test510InOidContainer() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(UserType.class)
                .id(1, 2, 3)
                .and().ownerId("oid4")
                .build();
        checkQueryRoundtripFile(UserType.class, q);
    }

    @Test
    public void test590Logicals() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(UserType.class)
                .block()
                .all()
                .or().none()
                .or().undefined()
                .endBlock()
                .and().none()
                .and()
                .not()
                .block()
                .all()
                .and().undefined()
                .endBlock()
                .build();
        checkQueryRoundtripFile(UserType.class, q);
    }

    @Test
    public void test595Logicals2() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(UserType.class)
                .item(UserType.F_DESCRIPTION).eq("A")
                .and().item(UserType.F_DESCRIPTION).eq("B")
                .or().item(UserType.F_DESCRIPTION).eq("C")
                .and().item(UserType.F_DESCRIPTION).eq("D")
                .and()
                .block()
                .item(UserType.F_DESCRIPTION).eq("E1")
                .and().item(UserType.F_DESCRIPTION).eq("E2")
                .or().item(UserType.F_DESCRIPTION).eq("E3")
                .endBlock()
                .build();

        checkQueryRoundtripFile(UserType.class, q);
    }

    @Test
    public void test600Type() throws Exception {
        ObjectQuery q = getPrismContext().queryFor(ObjectType.class)
                .type(UserType.class)
                .item(UserType.F_NAME).eqPoly("somename", "somename")
                .build();
        checkQueryRoundtripFile(ObjectType.class, q);
    }

    @Test
    public void test700Exists() throws Exception {
        PrismReferenceValue ownerRef = ObjectTypeUtil.createObjectRef("1234567890", ObjectTypes.USER).asReferenceValue();
        ObjectQuery q = getPrismContext().queryFor(AccessCertificationCaseType.class)
                .exists(new ParentPathSegment()) // if using T_PARENT then toString representation of paths is different
                .block()
                .id(123456L)
                .or().item(F_OWNER_REF).ref(ownerRef)
                .endBlock()
                .and().exists(AccessCertificationCaseType.F_WORK_ITEM)
                .item(AccessCertificationWorkItemType.F_STAGE_NUMBER).eq(3)
                .build();
        checkQueryRoundtripFile(AccessCertificationCaseType.class, q);
    }

    public static final String TEST_900_910_FILE_NAME = TEST_DIR + "/test900TypeWrong.xml";

    @Test
    public void test900TypeWrong() throws Exception {
        try {
            toQueryType(FileUtils.readFileToString(
                    new File(TEST_900_910_FILE_NAME), StandardCharsets.UTF_8));
            fail("Unexpected success!");
        } catch (SchemaException e) {
            System.out.println("Got expected exception: " + e.getMessage());
        }
    }

    @Test
    public void test910TypeWrongCompat() throws Exception {
        QueryType jaxb = toQueryTypeCompat(FileUtils.readFileToString(
                new File(TEST_900_910_FILE_NAME), StandardCharsets.UTF_8));
        displayQueryType(jaxb);
        try {
            ObjectQuery query = toObjectQuery(ObjectType.class, jaxb);
            displayQuery(query);
            fail("Unexpected success!");
        } catch (SchemaException e) {
            System.out.println("Got expected exception: " + e.getMessage());
        }
    }

    // Q{AND(IN OID: 8657eccf-c60d-4b5c-bbd4-4c73ecfd6436; ,
    //  REF: resourceRef,PRV(oid=aeff994e-381a-4fb3-af3b-f0f5dcdc9653, targetType=null),
    //  EQUAL: kind,PPV(ShadowKindType:ENTITLEMENT),
    //  EQUAL: intent,PPV(String:group)),null

    @Test
    public void test920QueryTypeEquals() throws Exception {
        ObjectQuery query1 = getPrismContext().queryFor(ShadowType.class)
                .id("8657eccf-c60d-4b5c-bbd4-4c73ecfd6436")
                .and().item(ShadowType.F_RESOURCE_REF).ref("aeff994e-381a-4fb3-af3b-f0f5dcdc9653")
                .and().item(ShadowType.F_KIND).eq(ShadowKindType.ENTITLEMENT)
                .and().item(ShadowType.F_INTENT).eq("group")
                .build();
        ObjectQuery query2 = getPrismContext().queryFor(ShadowType.class)
                .id("8657eccf-c60d-4b5c-bbd4-4c73ecfd6436")
                .and().item(ShadowType.F_RESOURCE_REF).ref("aeff994e-381a-4fb3-af3b-f0f5dcdc9653")
                .and().item(ShadowType.F_KIND).eq(ShadowKindType.ENTITLEMENT)
                .and().item(ShadowType.F_INTENT).eq("group")
                .build();
        System.out.println(query1.equals(query2));
        QueryType bean1 = getPrismContext().getQueryConverter().createQueryType(query1);
        QueryType bean2 = getPrismContext().getQueryConverter().createQueryType(query2);
        System.out.println(bean1.equals(bean2));
        System.out.println(bean1.hashCode() == bean2.hashCode());
    }

    /**
     * MID-6658
     */
    @Test
    public void test930EqualsWithTypedValue() throws Exception {
        SearchFilterType filterBean = PrismTestUtil.parseAtomicValue(FILTER_EQUALS_WITH_TYPED_VALUE, SearchFilterType.COMPLEX_TYPE);
        ObjectQuery query;
        try {
            query = getQueryConverter().createObjectQuery(UserType.class, filterBean);
            displayQuery(query);
            assertNotNull(query);

            ObjectFilter filter = query.getFilter();
            PrismAsserts.assertEqualsFilter(query.getFilter(), EXT_LONG_TYPE, DOMUtil.XSD_LONG, EXT_LONG_TYPE_PATH);
            PrismAsserts.assertEqualsFilterValue((EqualFilter<?>) filter, 4L);

            QueryType convertedQueryBean = toQueryType(query);
            displayQueryType(convertedQueryBean);
        } catch (Exception ex) {
            logger.error("Error while converting query: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * MID-6657
     */
    @Test
    public void test935NotEqualsNull() throws Exception {
        SearchFilterType filterBean = PrismTestUtil.parseAtomicValue(FILTER_NOT_EQUALS_NULL, SearchFilterType.COMPLEX_TYPE);
        ObjectQuery query;
        try {
            query = getQueryConverter().createObjectQuery(UserType.class, filterBean);
            displayQuery(query);
            assertNotNull(query);

            ObjectFilter filter = query.getFilter();
            assertTrue(filter instanceof NotFilter);
            ObjectFilter innerFilter = ((NotFilter) filter).getFilter();
            assertTrue(innerFilter instanceof EqualFilter);
            List<? extends PrismPropertyValue<?>> values = ((EqualFilter<?>) innerFilter).getValues();
            assertTrue(values == null || values.isEmpty());

            QueryType convertedQueryBean = toQueryType(query);
            displayQueryType(convertedQueryBean);
        } catch (Exception ex) {
            logger.error("Error while converting query: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    /** MID-7905 reported for 4.4.1, fails for 4.4.2 as well, fixed in 4.4.3 and in 4.6. */
    @Test
    public void test950ExistsForReferenceTarget() {
        getPrismContext().queryFor(UserType.class)
                .exists(F_ROLE_MEMBERSHIP_REF, T_OBJECT_REFERENCE)
                .item(ObjectType.F_NAME).eq("role-name")
                .build();
    }

    @Test
    public void test960MidpointQueryForOperationExecution() throws SchemaException {
        var filter = PrismContext.get().createQueryParser().parseFilter(OperationExecutionType.class, "taskRef matches (targetType = TaskType)");
        assertNotNull(filter);
    }
}
