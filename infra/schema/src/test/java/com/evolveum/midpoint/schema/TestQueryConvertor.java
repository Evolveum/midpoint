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

import static com.evolveum.midpoint.prism.util.PrismTestUtil.*;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.parser.DomParser;
import com.evolveum.midpoint.prism.util.PrismTestUtil;

import com.evolveum.midpoint.prism.util.PrismUtil;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.LogicalFilter;
import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DomAsserts;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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
		
		Element filterClauseElement = convertedFilterType.getFilterClauseAsElement();
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
		LOGGER.info(DOMUtil.serializeDOMToString(convertedQueryType.getFilter().getFilterClauseAsElement()));

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
		LOGGER.info(DOMUtil.serializeDOMToString(convertedQueryType.getFilter().getFilterClauseAsElement()));

		// TODO: add some asserts
	}

	@Test
	public void testConnectorQuery() throws Exception {
		displayTestTitle("testConnectorQuery");
		SearchFilterType filterType = PrismTestUtil.parseAtomicValue(FILTER_CONNECTOR_BY_TYPE_FILE, SearchFilterType.COMPLEX_TYPE);
		ObjectQuery query = null;
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
		} catch (SchemaException ex) {
			LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			throw ex;
		} catch (RuntimeException ex) {
			LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			throw ex;
		} catch (Exception ex) {
			LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			throw ex;
		}

	}
	
	@Test
	public void testTypeFilterQuery() throws Exception {
		displayTestTitle("testConnectorQuery");
		SearchFilterType filterType = PrismTestUtil.parseAtomicValue(FILTER_BY_TYPE_FILE, SearchFilterType.COMPLEX_TYPE);
		ObjectQuery query = null;
		try {
			query = QueryJaxbConvertor.createObjectQuery(ConnectorType.class, filterType, getPrismContext());
			displayQuery(query);

			assertNotNull(query);
			ObjectFilter filter = query.getFilter();
			assertTrue("filter is not an instance of type filter", filter instanceof TypeFilter);
			
			TypeFilter typeFilter = (TypeFilter) filter;
			assertEquals(typeFilter.getType(), UserType.COMPLEX_TYPE);
			assertNotNull("filter in type filter must not be null", typeFilter.getFilter());
			PrismAsserts.assertEqualsFilter(typeFilter.getFilter(), UserType.COMPLEX_TYPE, DOMUtil.XSD_QNAME, new ItemPath(UserType.F_NAME));
			PrismAsserts.assertEqualsFilterValue((EqualFilter) typeFilter.getFilter(), "some name identificator");
//			PrismAsserts.assertEqualsFilter(query.getFilter(), ConnectorType.F_CONNECTOR_TYPE, DOMUtil.XSD_STRING,
//					new ItemPath(ConnectorType.F_CONNECTOR_TYPE));
//			PrismAsserts.assertEqualsFilterValue((EqualsFilter) filter, "org.identityconnectors.ldap.LdapConnector");

			QueryType convertedQueryType = toQueryType(query);
			displayQueryType(convertedQueryType);
		} catch (SchemaException ex) {
			LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			throw ex;
		} catch (RuntimeException ex) {
			LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			throw ex;
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
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(type, queryType,
				getPrismContext());
		return query;
	}
	
	private ObjectQuery toObjectQuery(Class type, SearchFilterType filterType) throws Exception {
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(type, filterType,
				getPrismContext());
		return query;
	}

	private QueryType toQueryType(ObjectQuery query) throws Exception {
		return QueryJaxbConvertor.createQueryType(query, getPrismContext());
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
				new File(TEST_DIR, "filter-user-substring-fullName.xml") };
		// prismContext.silentMarshalObject(queryTypeNew, LOGGER);
		for (File file : userQueriesToTest) {
			SearchFilterType filterType = PrismTestUtil.parseAtomicValue(file, SearchFilterType.COMPLEX_TYPE);
			LOGGER.info("===[ query type parsed ]===");
			ObjectQuery query = null;
			try {
				query = QueryJaxbConvertor.createObjectQuery(UserType.class, filterType, getPrismContext());
				LOGGER.info("query converted: ");

				LOGGER.info("QUERY DUMP: {}", query.debugDump());
				LOGGER.info("QUERY Pretty print: {}", query.toString());
				System.out.println("QUERY Pretty print: " + query.toString());

				QueryType convertedQueryType = QueryJaxbConvertor.createQueryType(query, getPrismContext());
				LOGGER.info(DOMUtil.serializeDOMToString(convertedQueryType.getFilter().getFilterClauseAsElement()));
			} catch (SchemaException ex) {
				LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
				throw ex;
			} catch (RuntimeException ex) {
				LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
				throw ex;
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
}
