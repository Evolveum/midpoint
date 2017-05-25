/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DomAsserts;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.io.FileUtils;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.evolveum.midpoint.prism.PrismConstants.T_PARENT;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_OWNER_REF;
import static org.testng.AssertJUnit.*;

public class TestQueryConvertor {

	private static final Trace LOGGER = TraceManager.getTrace(TestQueryConvertor.class);

	private static final File TEST_DIR = new File("src/test/resources/queryconvertor");

	private static final String NS_EXTENSION = "http://midpoint.evolveum.com/xml/ns/test/extension";
	private static final String NS_ICFS = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3";
	private static final String NS_BLA = "http://midpoint.evolveum.com/blabla";

	private static final QName intExtensionDefinition = new QName(NS_EXTENSION, "intType");
	private static final QName stringExtensionDefinition = new QName(NS_EXTENSION, "stringType");
	private static final QName fooBlaDefinition = new QName(NS_BLA, "foo");
	private static final QName ICF_NAME = new QName(NS_ICFS, "name");

	private static final QName FAILED_OPERATION_TYPE_QNAME = new QName(SchemaConstantsGenerated.NS_COMMON,
			"FailedOperationTypeType");

	private static final File FILTER_AND_GENERIC_FILE = new File(TEST_DIR, "filter-and-generic.xml");
	private static final File FILTER_ACCOUNT_FILE = new File(TEST_DIR, "filter-account.xml");
	private static final File FILTER_ACCOUNT_ATTRIBUTES_RESOURCE_REF_FILE = new File(TEST_DIR,
			"filter-account-by-attributes-and-resource-ref.xml");
	private static final File FILTER_ACCOUNT_ATTRIBUTES_RESOURCE_REF_NO_NS_FILE = new File(TEST_DIR,
			"filter-account-by-attributes-and-resource-ref-no-ns.xml");
	private static final File FILTER_OR_COMPOSITE = new File(TEST_DIR, "filter-or-composite.xml");
	private static final File FILTER_CONNECTOR_BY_TYPE_FILE = new File(TEST_DIR, "filter-connector-by-type.xml");
	private static final File FILTER_BY_TYPE_FILE = new File(TEST_DIR, "filter-by-type.xml");

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	public void testAccountFilter() throws Exception {
		displayTestTitle("testAccountFilter");

		SearchFilterType filterType = unmarshalFilter(FILTER_ACCOUNT_FILE);
		ObjectQuery query = toObjectQuery(ShadowType.class, filterType);
		displayQuery(query);

		assertNotNull(query);

		ObjectFilter filter = query.getFilter();
		PrismAsserts.assertAndFilter(filter, 2);

		ObjectFilter first = getFilterCondition(filter, 0);
		PrismAsserts.assertEqualsFilter(first, ShadowType.F_FAILED_OPERATION_TYPE, FAILED_OPERATION_TYPE_QNAME,
				new ItemPath(ShadowType.F_FAILED_OPERATION_TYPE));
		PrismAsserts.assertEqualsFilterValue((EqualFilter) first, FailedOperationTypeType.ADD);

		ObjectFilter second = getFilterCondition(filter, 1);
		PrismAsserts.assertEqualsFilter(second, ShadowType.F_NAME, PolyStringType.COMPLEX_TYPE, new ItemPath(
				ShadowType.F_NAME));
		PrismAsserts.assertEqualsFilterValue((EqualFilter) second, createPolyString("someName"));

		QueryType convertedQueryType = toQueryType(query);
		System.out.println("Re-converted query type");
		System.out.println(convertedQueryType.debugDump());
		
		SearchFilterType convertedFilterType = convertedQueryType.getFilter();
		MapXNode xFilter = convertedFilterType.serializeToXNode();
		PrismAsserts.assertSize(xFilter, 1);
		PrismAsserts.assertSubnode(xFilter, AndFilter.ELEMENT_NAME, MapXNode.class);
		MapXNode xandmap = (MapXNode) xFilter.get(AndFilter.ELEMENT_NAME);
		PrismAsserts.assertSize(xandmap, 1);
		PrismAsserts.assertSubnode(xandmap, EqualFilter.ELEMENT_NAME, ListXNode.class);
		ListXNode xequalsList = (ListXNode) xandmap.get(EqualFilter.ELEMENT_NAME);
		PrismAsserts.assertSize(xequalsList, 2);
		
		Element filterClauseElement = convertedFilterType.getFilterClauseAsElement(getPrismContext());
		System.out.println("Serialized filter (JAXB->DOM)");
		System.out.println(DOMUtil.serializeDOMToString(filterClauseElement));
		
		DomAsserts.assertElementQName(filterClauseElement, AndFilter.ELEMENT_NAME);
		DomAsserts.assertSubElements(filterClauseElement, 2);
		
		Element firstSubelement = DOMUtil.getChildElement(filterClauseElement, 0);
		DomAsserts.assertElementQName(firstSubelement, EqualFilter.ELEMENT_NAME);
		Element firstValueElement = DOMUtil.getChildElement(firstSubelement, PrismConstants.Q_VALUE);
		DomAsserts.assertTextContent(firstValueElement, "add");
		
		Element secondSubelement = DOMUtil.getChildElement(filterClauseElement, 1);
		DomAsserts.assertElementQName(secondSubelement, EqualFilter.ELEMENT_NAME);
		Element secondValueElement = DOMUtil.getChildElement(secondSubelement, PrismConstants.Q_VALUE);
		DomAsserts.assertTextContent(secondValueElement, "someName");
	}

    @Test
	public void testAccountQueryAttributesAndResource() throws Exception {
		displayTestTitle("testAccountQueryAttributesAndResource");

		SearchFilterType filterType = unmarshalFilter(FILTER_ACCOUNT_ATTRIBUTES_RESOURCE_REF_FILE);
		ObjectQuery query = toObjectQuery(ShadowType.class, filterType);
		displayQuery(query);

		assertNotNull(query);

		ObjectFilter filter = query.getFilter();
		PrismAsserts.assertAndFilter(filter, 2);

		ObjectFilter first = getFilterCondition(filter, 0);
		PrismAsserts.assertRefFilter(first, ShadowType.F_RESOURCE_REF, ObjectReferenceType.COMPLEX_TYPE, new ItemPath(
				ShadowType.F_RESOURCE_REF));
		assertRefFilterValue((RefFilter) first, "aae7be60-df56-11df-8608-0002a5d5c51b");

		ObjectFilter second = getFilterCondition(filter, 1);
		PrismAsserts.assertEqualsFilter(second, ICF_NAME, DOMUtil.XSD_STRING, new ItemPath(ShadowType.F_ATTRIBUTES,
				ICF_NAME));
		PrismAsserts.assertEqualsFilterValue((EqualFilter) second, "uid=jbond,ou=People,dc=example,dc=com");

		QueryType convertedQueryType = toQueryType(query);
		LOGGER.info(DOMUtil.serializeDOMToString(convertedQueryType.getFilter().getFilterClauseAsElement(getPrismContext())));

		// TODO: add some asserts
	}

    @Test
	public void testAccountQueryAttributesAndResourceNoNs() throws Exception {
		displayTestTitle("testAccountQueryAttributesAndResourceNoNs");

		SearchFilterType filterType = unmarshalFilter(FILTER_ACCOUNT_ATTRIBUTES_RESOURCE_REF_NO_NS_FILE);
		ObjectQuery query = toObjectQuery(ShadowType.class, filterType);
		displayQuery(query);

		assertNotNull(query);

		ObjectFilter filter = query.getFilter();
		PrismAsserts.assertAndFilter(filter, 2);

		ObjectFilter first = getFilterCondition(filter, 0);
		PrismAsserts.assertRefFilter(first, ShadowType.F_RESOURCE_REF, ObjectReferenceType.COMPLEX_TYPE, new ItemPath(
				ShadowType.F_RESOURCE_REF));
		assertRefFilterValue((RefFilter) first, "aae7be60-df56-11df-8608-0002a5d5c51b");

		ObjectFilter second = getFilterCondition(filter, 1);
		PrismAsserts.assertEqualsFilter(second, ICF_NAME, DOMUtil.XSD_STRING, new ItemPath("attributes", "name"));
		//PrismAsserts.assertEqualsFilterValue((EqualFilter) second, "uid=jbond,ou=People,dc=example,dc=com");

		QueryType convertedQueryType = toQueryType(query);
		System.out.println(DOMUtil.serializeDOMToString(convertedQueryType.getFilter().getFilterClauseAsElement(getPrismContext())));

		// TODO: add some asserts
	}

	@Test
	public void testAccountQueryCompositeOr() throws Exception {
		displayTestTitle("testAccountQueryCompositeOr");

		SearchFilterType filterType = unmarshalFilter(FILTER_OR_COMPOSITE);
		ObjectQuery query = toObjectQuery(ShadowType.class, filterType);
		displayQuery(query);

		assertNotNull(query);

		ObjectFilter filter = query.getFilter();
		PrismAsserts.assertOrFilter(filter, 4);

		ObjectFilter first = getFilterCondition(filter, 0);
		PrismAsserts.assertEqualsFilter(first, ShadowType.F_INTENT, DOMUtil.XSD_STRING, new ItemPath(ShadowType.F_INTENT));
		PrismAsserts.assertEqualsFilterValue((EqualFilter) first, "some account type");

		ObjectFilter second = getFilterCondition(filter, 1);
		PrismAsserts.assertEqualsFilter(second, fooBlaDefinition, DOMUtil.XSD_STRING, new ItemPath(
				ShadowType.F_ATTRIBUTES, fooBlaDefinition));
		PrismAsserts.assertEqualsFilterValue((EqualFilter) second, "foo value");

		ObjectFilter third = getFilterCondition(filter, 2);
		PrismAsserts.assertEqualsFilter(third, stringExtensionDefinition, DOMUtil.XSD_STRING, new ItemPath(
				ShadowType.F_EXTENSION, stringExtensionDefinition));
		PrismAsserts.assertEqualsFilterValue((EqualFilter) third, "uid=test,dc=example,dc=com");

		ObjectFilter forth = getFilterCondition(filter, 3);
		PrismAsserts.assertRefFilter(forth, ShadowType.F_RESOURCE_REF, ObjectReferenceType.COMPLEX_TYPE, new ItemPath(
				ShadowType.F_RESOURCE_REF));
		assertRefFilterValue((RefFilter) forth, "d0db5be9-cb93-401f-b6c1-86ffffe4cd5e");

		QueryType convertedQueryType = toQueryType(query);
		LOGGER.info(DOMUtil.serializeDOMToString(convertedQueryType.getFilter().getFilterClauseAsElement(getPrismContext())));

		// TODO: add some asserts
	}

	@Test
	public void testConnectorQuery() throws Exception {
		displayTestTitle("testConnectorQuery");
		SearchFilterType filterType = PrismTestUtil.parseAtomicValue(FILTER_CONNECTOR_BY_TYPE_FILE, SearchFilterType.COMPLEX_TYPE);
		ObjectQuery query;
		try {
			query = QueryJaxbConvertor.createObjectQuery(ConnectorType.class, filterType, getPrismContext());
			displayQuery(query);

			assertNotNull(query);
			ObjectFilter filter = query.getFilter();
			PrismAsserts.assertEqualsFilter(query.getFilter(), ConnectorType.F_CONNECTOR_TYPE, DOMUtil.XSD_STRING,
					new ItemPath(ConnectorType.F_CONNECTOR_TYPE));
			PrismAsserts.assertEqualsFilterValue((EqualFilter) filter, "org.identityconnectors.ldap.LdapConnector");

			QueryType convertedQueryType = toQueryType(query);
			displayQueryType(convertedQueryType);
		} catch (Exception ex) {
			LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			throw ex;
		}

	}
	
	private PrismObjectDefinition<UserType> getUserDefinition(){
		return getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
	}
	
	@Test
	public void testTypeFilterQuery() throws Exception {
		displayTestTitle("testConnectorQuery");
		SearchFilterType filterType = PrismTestUtil.parseAtomicValue(FILTER_BY_TYPE_FILE, SearchFilterType.COMPLEX_TYPE);
		ObjectQuery query;
		try {
			query = QueryJaxbConvertor.createObjectQuery(ConnectorType.class, filterType, getPrismContext());
			displayQuery(query);

			assertNotNull(query);
			ObjectFilter filter = query.getFilter();
			assertTrue("filter is not an instance of type filter", filter instanceof TypeFilter);
			
			TypeFilter typeFilter = (TypeFilter) filter;
			assertEquals(typeFilter.getType(), UserType.COMPLEX_TYPE);
			assertNotNull("filter in type filter must not be null", typeFilter.getFilter());
			ItemPath namePath = new ItemPath(UserType.F_NAME);
			PrismPropertyDefinition ppd = getUserDefinition().findPropertyDefinition(namePath);
			PrismAsserts.assertEqualsFilter(typeFilter.getFilter(), ppd.getName(), ppd.getTypeName(), namePath);
			PrismAsserts.assertEqualsFilterValue((EqualFilter) typeFilter.getFilter(), PrismTestUtil.createPolyString("some name identificator"));
//			PrismAsserts.assertEqualsFilter(query.getFilter(), ConnectorType.F_CONNECTOR_TYPE, DOMUtil.XSD_STRING,
//					new ItemPath(ConnectorType.F_CONNECTOR_TYPE));
//			PrismAsserts.assertEqualsFilterValue((EqualsFilter) filter, "org.identityconnectors.ldap.LdapConnector");

			QueryType convertedQueryType = toQueryType(query);
			displayQueryType(convertedQueryType);
		} catch (Exception ex) {
			LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			throw ex;
		}

	}
	
	@Test
	public void testShadowKindTypeQuery() throws Exception {
		displayTestTitle("testShadowKindTypeQuery");
		SearchFilterType filterType = PrismTestUtil.parseAtomicValue(FILTER_BY_TYPE_FILE, SearchFilterType.COMPLEX_TYPE);
		ObjectQuery query;
		try {
			ObjectFilter kindFilter = QueryBuilder.queryFor(ShadowType.class, getPrismContext())
					.item(ShadowType.F_KIND).eq(ShadowKindType.ACCOUNT)
					.buildFilter();
			query = ObjectQuery.createObjectQuery(kindFilter);
			assertNotNull(query);
			ObjectFilter filter = query.getFilter();
			assertTrue("filter is not an instance of type filter", filter instanceof EqualFilter);
			
			EqualFilter typeFilter = (EqualFilter) filter;
			assertEquals(typeFilter.getSingleValue().getRealValue(), ShadowKindType.ACCOUNT);
			
			QueryType convertedQueryType = toQueryType(query);
			
			toObjectQuery(ShadowType.class, convertedQueryType);
			
			displayQueryType(convertedQueryType);
		} catch (Exception ex) {
			LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			throw ex;
		}

	}

	private void assertRefFilterValue(RefFilter filter, String oid) {
		List<? extends PrismValue> values = filter.getValues();
		assertEquals(1, values.size());
		assertEquals(PrismReferenceValue.class, values.get(0).getClass());
		PrismReferenceValue val = (PrismReferenceValue) values.get(0);

		assertEquals(oid, val.getOid());
		// TODO: maybe some more asserts for type, relation and other reference
		// values
	}

	private ObjectQuery toObjectQuery(Class type, QueryType queryType) throws Exception {
		return QueryJaxbConvertor.createObjectQuery(type, queryType,
				getPrismContext());
	}
	
	private ObjectQuery toObjectQuery(Class type, SearchFilterType filterType) throws Exception {
		return QueryJaxbConvertor.createObjectQuery(type, filterType,
				getPrismContext());
	}

	private QueryType toQueryType(ObjectQuery query) throws Exception {
		return QueryJaxbConvertor.createQueryType(query, getPrismContext());
	}

	private QueryType toQueryType(String xml) throws SchemaException {
		return PrismTestUtil.parseAtomicValue(xml, QueryType.COMPLEX_TYPE);
	}

	private String toXml(QueryType q1jaxb) throws SchemaException {
		return getPrismContext().xmlSerializer().serializeRealValue(q1jaxb, SchemaConstantsGenerated.Q_QUERY);
	}

	@Test
	public void testGenericQuery() throws Exception {
		displayTestTitle("testGenericQuery");

		SearchFilterType queryType = unmarshalFilter(FILTER_AND_GENERIC_FILE);
		ObjectQuery query = toObjectQuery(GenericObjectType.class, queryType);

		displayQuery(query);

		// check parent filter
		assertNotNull(query);
		ObjectFilter filter = query.getFilter();
		PrismAsserts.assertAndFilter(filter, 2);
		// check first condition
		ObjectFilter first = getFilterCondition(filter, 0);
		PrismAsserts.assertEqualsFilter(first, GenericObjectType.F_NAME, PolyStringType.COMPLEX_TYPE, new ItemPath(
				GenericObjectType.F_NAME));
		PrismAsserts.assertEqualsFilterValue((EqualFilter) first, createPolyString("generic object"));
		// check second condition
		ObjectFilter second = getFilterCondition(filter, 1);
		PrismAsserts.assertEqualsFilter(second, intExtensionDefinition, DOMUtil.XSD_INT, new ItemPath(
				ObjectType.F_EXTENSION, new QName(NS_EXTENSION, "intType")));
		PrismAsserts.assertEqualsFilterValue((EqualFilter) second, 123);

		QueryType convertedQueryType = toQueryType(query);
		assertNotNull("Re-serialized query is null ", convertedQueryType);
		assertNotNull("Filter in re-serialized query must not be null.", convertedQueryType.getFilter());

		displayQueryType(convertedQueryType);
	}

	@Test
	public void testUserQuery() throws Exception {
		displayTestTitle("testUserQuery");
		
		File[] userQueriesToTest = new File[] { new File(TEST_DIR, "filter-user-by-fullName.xml"),
				new File(TEST_DIR, "filter-user-by-name.xml"),
				new File(TEST_DIR, "filter-user-substring-fullName.xml"),
				new File(TEST_DIR, "filter-user-substring-employeeType.xml")
		};
		// prismContext.silentMarshalObject(queryTypeNew, LOGGER);
		for (File file : userQueriesToTest) {
			SearchFilterType filterType = PrismTestUtil.parseAtomicValue(file, SearchFilterType.COMPLEX_TYPE);
			LOGGER.info("===[ query type parsed ]===");
			ObjectQuery query;
			try {
				query = QueryJaxbConvertor.createObjectQuery(UserType.class, filterType, getPrismContext());
				LOGGER.info("query converted: ");

				LOGGER.info("QUERY DUMP: {}", query.debugDump());
				LOGGER.info("QUERY Pretty print: {}", query.toString());
				System.out.println("QUERY Pretty print: " + query.toString());

				QueryType convertedQueryType = QueryJaxbConvertor.createQueryType(query, getPrismContext());
				LOGGER.info(DOMUtil.serializeDOMToString(convertedQueryType.getFilter().getFilterClauseAsElement(getPrismContext())));
			} catch (Exception ex) {
				LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
				throw ex;
			}
		}
	}

	@Test
	public void testConvertQueryNullFilter() throws Exception {
		ObjectQuery query = ObjectQuery.createObjectQuery(ObjectPaging.createPaging(0, 10));
		QueryType queryType = QueryJaxbConvertor.createQueryType(query, getPrismContext());

		assertNotNull(queryType);
		assertNull(queryType.getFilter());
		PagingType paging = queryType.getPaging();
		assertNotNull(paging);
		assertEquals(new Integer(0), paging.getOffset());
		assertEquals(new Integer(10), paging.getMaxSize());

	}

	private void checkQuery(Class<? extends Containerable> objectClass, ObjectQuery q1object, String q2xml) throws Exception {
		// step 1 (serialization of Q1 + comparison)
		displayText("Query 1:");
		displayQuery(q1object);
		QueryType q1jaxb = toQueryType(q1object);
		displayQueryType(q1jaxb);
		String q1xml = toXml(q1jaxb);
		displayQueryXml(q1xml);
//		XMLAssert.assertXMLEqual("Serialized query is not correct: Expected:\n" + q2xml + "\n\nReal:\n" + q1xml, q2xml, q1xml);

		// step 2 (parsing of Q2 + comparison)
		displayText("Query 2:");
		displayQueryXml(q2xml);
		QueryType q2jaxb = toQueryType(q2xml);
		displayQueryType(q2jaxb);
		ObjectQuery q2object = toObjectQuery(objectClass, q2jaxb);
		assertEquals("Reparsed query is not as original one (via toString)", q1object.toString(), q2object.toString());	// primitive way of comparing parsed queries
		assertTrue("Reparsed query is not as original one (via equivalent):\nq1="+q1object+"\nq2="+q2object, q1object.equivalent(q2object));
	}

	private void checkQueryRoundtrip(Class<? extends Containerable> objectClass, ObjectQuery q1object, String q2xml) throws Exception {
		checkQuery(objectClass, q1object, q2xml);
		String q1xml = toXml(toQueryType(q1object));
		ObjectQuery q2object = toObjectQuery(objectClass, toQueryType(q2xml));
		checkQuery(objectClass, q2object, q1xml);
	}

	private void checkQueryRoundtripFile(Class<? extends Containerable> objectClass, ObjectQuery query, String testName) throws Exception {
		String fileName = TEST_DIR + "/" + testName + ".xml";
		checkQueryRoundtrip(objectClass, query, FileUtils.readFileToString(new File(fileName)));
	}

	@Test
	public void test100All() throws Exception {
		final String TEST_NAME = "test100All";
		displayTestTitle(TEST_NAME);
		ObjectQuery q = QueryBuilder.queryFor(UserType.class, getPrismContext()).all().build();
		checkQueryRoundtripFile(UserType.class, q, TEST_NAME);
	}

	@Test
	public void test110None() throws Exception {
		final String TEST_NAME = "test110None";
		displayTestTitle(TEST_NAME);
		ObjectQuery q = QueryBuilder.queryFor(UserType.class, getPrismContext()).none().build();
		checkQueryRoundtripFile(UserType.class, q, TEST_NAME);
	}

	@Test
	public void test120Undefined() throws Exception {
		final String TEST_NAME = "test120Undefined";
		displayTestTitle(TEST_NAME);
		ObjectQuery q = QueryBuilder.queryFor(UserType.class, getPrismContext()).undefined().build();
		checkQueryRoundtripFile(UserType.class, q, TEST_NAME);
	}

	@Test
	public void test200Equal() throws Exception {
		final String TEST_NAME = "test200Equal";
		displayTestTitle(TEST_NAME);
		ObjectQuery q = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.item(UserType.F_NAME).eqPoly("some-name", "somename").matchingOrig()
				.build();
		checkQueryRoundtripFile(UserType.class, q, TEST_NAME);
	}

	@Test
	public void test210EqualMultiple() throws Exception {
		final String TEST_NAME = "test210EqualMultiple";
		displayTestTitle(TEST_NAME);
		ObjectQuery q = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.item(UserType.F_EMPLOYEE_TYPE).eq("STD", "TEMP")
				.build();
		checkQueryRoundtripFile(UserType.class, q, TEST_NAME);
	}

	@Test
	public void test220EqualRightHandItem() throws Exception {
		final String TEST_NAME = "test220EqualRightHandItem";
		displayTestTitle(TEST_NAME);
		ObjectQuery q = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.item(UserType.F_EMPLOYEE_NUMBER).eq().item(UserType.F_COST_CENTER)
				.build();
		checkQueryRoundtripFile(UserType.class, q, TEST_NAME);
	}

	@Test
	public void test300Greater() throws Exception {
		final String TEST_NAME = "test300Greater";
		displayTestTitle(TEST_NAME);
		ObjectQuery q = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.item(UserType.F_COST_CENTER).gt("100000")
				.build();
		checkQueryRoundtripFile(UserType.class, q, TEST_NAME);
	}

	@Test
	public void test310AllComparisons() throws Exception {
		final String TEST_NAME = "test310AllComparisons";
		displayTestTitle(TEST_NAME);
		ObjectQuery q = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.item(UserType.F_COST_CENTER).gt("100000")
				.and().item(UserType.F_COST_CENTER).lt("999999")
				.or()
				.item(UserType.F_COST_CENTER).ge("X100")
				.and().item(UserType.F_COST_CENTER).le("X999")
				.build();
		checkQueryRoundtripFile(UserType.class, q, TEST_NAME);
	}

	@Test
	public void test350Substring() throws Exception {
		final String TEST_NAME = "test350Substring";
		displayTestTitle(TEST_NAME);
		ObjectQuery q = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.item(UserType.F_EMPLOYEE_TYPE).contains("A")
				.or().item(UserType.F_EMPLOYEE_TYPE).startsWith("B")
				.or().item(UserType.F_EMPLOYEE_TYPE).endsWith("C")
				.or().item(UserType.F_NAME).startsWithPoly("john", "john").matchingOrig()
				.build();
		checkQueryRoundtripFile(UserType.class, q, TEST_NAME);
	}

	@Test
	public void test360Ref() throws Exception {
		final String TEST_NAME = "test360Ref";
		displayTestTitle(TEST_NAME);

		// we test only parsing here, as there are more serialized forms used here
		ObjectQuery q1object = QueryBuilder.queryFor(ShadowType.class, getPrismContext())
				.item(ShadowType.F_RESOURCE_REF).ref("oid1")
				.or().item(ShadowType.F_RESOURCE_REF).ref("oid2", ResourceType.COMPLEX_TYPE)
				.or().item(ShadowType.F_RESOURCE_REF).ref("oid3")
				.or().item(ShadowType.F_RESOURCE_REF).ref("oid4", ResourceType.COMPLEX_TYPE)
				.build();
		q1object.getFilter().checkConsistence(true);
		
		String q2xml = FileUtils.readFileToString(new File(TEST_DIR + "/" + TEST_NAME + ".xml"));
		displayQueryXml(q2xml);
		QueryType q2jaxb = toQueryType(q2xml);
		displayQueryType(q2jaxb);
		ObjectQuery q2object = toObjectQuery(ShadowType.class, q2jaxb);
		
		System.out.println("q1object:\n"+q1object.debugDump(1));
		System.out.println("q2object:\n"+q2object.debugDump(1));
		
		assertEquals("Reparsed query is not as original one (via toString)", q1object.toString(), q2object.toString());	// primitive way of comparing parsed queries
		
		if (!q1object.equivalent(q2object)) {
			AssertJUnit.fail("Reparsed query is not as original one (via equivalent):\nq1="+q1object+"\nq2="+q2object);
		}
	}

	@Test
	public void test365RefTwoWay() throws Exception {
		final String TEST_NAME = "test365RefTwoWay";
		displayTestTitle(TEST_NAME);
		PrismReferenceValue reference3 = new PrismReferenceValue("oid3", ResourceType.COMPLEX_TYPE);
		reference3.setRelation(new QName("test"));
		ObjectQuery q = QueryBuilder.queryFor(ShadowType.class, getPrismContext())
				.item(ShadowType.F_RESOURCE_REF).ref("oid1")
				.or().item(ShadowType.F_RESOURCE_REF).ref("oid2", ResourceType.COMPLEX_TYPE)
				.or().item(ShadowType.F_RESOURCE_REF).ref(reference3)
				.build();
		checkQueryRoundtripFile(ShadowType.class, q, TEST_NAME);
	}

	@Test
	public void test400OrgFilterRoot() throws Exception {
		final String TEST_NAME = "test400OrgFilterRoot";
		displayTestTitle(TEST_NAME);
		ObjectQuery q = QueryBuilder.queryFor(OrgType.class, getPrismContext()).isRoot().build();
		checkQueryRoundtripFile(OrgType.class, q, TEST_NAME);
	}

	@Test
	public void test410OrgFilterSubtree() throws Exception {
		final String TEST_NAME = "test410OrgFilterSubtree";
		displayTestTitle(TEST_NAME);
		ObjectQuery q = QueryBuilder.queryFor(OrgType.class, getPrismContext()).isChildOf("111").build();
		checkQueryRoundtripFile(OrgType.class, q, TEST_NAME);
	}

	@Test
	public void test420OrgFilterDirect() throws Exception {
		final String TEST_NAME = "test420OrgFilterDirect";
		displayTestTitle(TEST_NAME);
		ObjectQuery q = QueryBuilder.queryFor(OrgType.class, getPrismContext()).isDirectChildOf("222").build();
		checkQueryRoundtripFile(OrgType.class, q, TEST_NAME);
	}

	@Test
	public void test430OrgFilterDefaultScope() throws Exception {
		final String TEST_NAME = "test430OrgFilterDefaultScope";
		displayTestTitle(TEST_NAME);

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

		ObjectQuery expectedQuery = QueryBuilder.queryFor(OrgType.class, getPrismContext()).isChildOf("333").build();
		assertEquals("Parsed query is wrong", expectedQuery.toString(), query.toString());			// primitive way of comparing queries

		// now reserialize the parsed query and compare with XML - the XML contains explicit scope=SUBTREE (as this is set when parsing original queryXml)
		checkQueryRoundtripFile(OrgType.class, query, TEST_NAME);
	}

	@Test
	public void test500InOid() throws Exception {
		final String TEST_NAME = "test500InOid";
		displayTestTitle(TEST_NAME);
		ObjectQuery q = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.id("oid1", "oid2", "oid3")
				.build();
		checkQueryRoundtripFile(UserType.class, q, TEST_NAME);
	}

	@Test
	public void test510InOidContainer() throws Exception {
		final String TEST_NAME = "test510InOidContainer";
		displayTestTitle(TEST_NAME);
		ObjectQuery q = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.id(1, 2, 3)
				.and().ownerId("oid4")
				.build();
		checkQueryRoundtripFile(UserType.class, q, TEST_NAME);
	}

	@Test
	public void test590Logicals() throws Exception {
		final String TEST_NAME = "test590Logicals";
		displayTestTitle(TEST_NAME);
		ObjectQuery q = QueryBuilder.queryFor(UserType.class, getPrismContext())
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
		checkQueryRoundtripFile(UserType.class, q, TEST_NAME);
	}

	@Test
	public void test600Type() throws Exception {
		final String TEST_NAME = "test600Type";
		displayTestTitle(TEST_NAME);
		ObjectQuery q = QueryBuilder.queryFor(ObjectType.class, getPrismContext())
				.type(UserType.class)
					.item(UserType.F_NAME).eqPoly("somename", "somename")
				.build();
		checkQueryRoundtripFile(ObjectType.class, q, TEST_NAME);
	}

	@Test
	public void test700Exists() throws Exception {
		final String TEST_NAME = "test700Exists";
		displayTestTitle(TEST_NAME);
		PrismReferenceValue ownerRef = ObjectTypeUtil.createObjectRef("1234567890", ObjectTypes.USER).asReferenceValue();
		ObjectQuery q = QueryBuilder.queryFor(AccessCertificationCaseType.class, getPrismContext())
				.exists(T_PARENT)
					.block()
						.id(123456L)
						.or().item(F_OWNER_REF).ref(ownerRef)
					.endBlock()
				.and().exists(AccessCertificationCaseType.F_WORK_ITEM)
					.item(AccessCertificationWorkItemType.F_STAGE_NUMBER).eq(3)
				.build();
		checkQueryRoundtripFile(AccessCertificationCaseType.class, q, TEST_NAME);
	}

	@Test
	public void test900TypeWrong() throws Exception {
		final String TEST_NAME = "test900TypeWrong";
		String fileName = TEST_DIR + "/" + TEST_NAME + ".xml";
		QueryType jaxb = toQueryType(FileUtils.readFileToString(new File(fileName)));
		displayQueryType(jaxb);
		try {
			ObjectQuery query = toObjectQuery(ObjectType.class, jaxb);
			displayQuery(query);
			fail("Unexpected success!");
		} catch (SchemaException e) {
			System.out.println("Got expected exception: " + e.getMessage());
		}
	}

}
