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

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.*;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismContextFactory;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.query_2.PagingType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

public class TestQueryConvertor {

	private static final Trace LOGGER = TraceManager.getTrace(TestQueryConvertor.class);

	private static final File TEST_DIR = new File("src/test/resources/queryconvertor");

//	private PrismContext prismContext;

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		 PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
//		PrismContextFactory contextFactory = MidPointPrismContextFactory.FACTORY;
//		prismContext = contextFactory.createPrismContext();
//		prismContext.initialize();
	}

	@Test
	public void testAccountQuery() throws Exception {
		LOGGER.info("===[ testAccountQuery ]===");
		File[] queriesToTest = new File[] { new File(TEST_DIR + "/query.xml"),
				new File(TEST_DIR + "/query-account-by-attributes-and-resource-ref.xml"),
				new File(TEST_DIR + "/query-or-composite.xml") };

		for (File file : queriesToTest) {
			LOGGER.info("Start to convert query {} ", file.getName());
			QueryType queryType = PrismTestUtil.getJaxbUtil().unmarshalObject(file, QueryType.class);
			LOGGER.info("===[ query type parsed ]===");
			ObjectQuery query = null;
			try {
				query = QueryJaxbConvertor.createObjectQuery(ShadowType.class, queryType, PrismTestUtil.getPrismContext());
				LOGGER.info("query converted: ");

				LOGGER.info("QUERY DUMP: {}", query.debugDump());
				LOGGER.info("QUERY Pretty print: {}", query.toString());
				System.out.println("QUERY Pretty print: " + query.toString());

				System.out.println("debug util: " + SchemaDebugUtil.prettyPrint(query));
				QueryType convertedQueryType = QueryJaxbConvertor.createQueryType(query, PrismTestUtil.getPrismContext());
				LOGGER.info(DOMUtil.serializeDOMToString(convertedQueryType.getFilter()));

			} catch (SchemaException ex) {
				LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			} catch (RuntimeException ex) {
				LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			} catch (Exception ex) {
				LOGGER.error("Error while converting query: {}", ex.getMessage(), ex);
			}

		}
		// TODO: add some asserts
	}

	@Test
	public void testConnectorQuery() throws Exception {
		LOGGER.info("===[ testConnectorQuery ]===");
		File connectorQueryToTest = new File(TEST_DIR + "/query-connector-by-type.xml");
		QueryType queryType = PrismTestUtil.getJaxbUtil().unmarshalObject(connectorQueryToTest,
				QueryType.class);
		LOGGER.info("===[ query type parsed ]===");
		ObjectQuery query = null;
		try {
			query = QueryJaxbConvertor.createObjectQuery(ConnectorType.class, queryType, PrismTestUtil.getPrismContext());
			LOGGER.info("query converted: ");

//			LOGGER.info("QUERY DUMP: {}", query.debugDump());
//			LOGGER.info("QUERY Pretty print: {}", query.toString());
//			System.out.println("QUERY Pretty print: " + query.toString());

			assertNotNull(query);
			assertEquals(EqualsFilter.class, query.getFilter().getClass());
			List<? extends PrismValue> values = ((EqualsFilter) query.getFilter()).getValues();
			assertEquals(1, values.size());
			assertEquals(PrismPropertyValue.class, values.get(0).getClass());
			assertEquals("org.identityconnectors.ldap.LdapConnector",
					((PrismPropertyValue) values.get(0)).getValue());
			assertEquals(ConnectorType.F_CONNECTOR_TYPE, ((EqualsFilter) query.getFilter()).getDefinition()
					.getName());
			assertEquals(new ItemPath(ConnectorType.F_CONNECTOR_TYPE), ((EqualsFilter) query.getFilter()).getFullPath());
			assertEquals(DOMUtil.XSD_STRING, ((EqualsFilter) query.getFilter()).getDefinition()
					.getTypeName());

			QueryType convertedQueryType = QueryJaxbConvertor.createQueryType(query, PrismTestUtil.getPrismContext());
			LOGGER.info(DOMUtil.serializeDOMToString(convertedQueryType.getFilter()));
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
	public void testGenericQuery() throws Exception {
		LOGGER.info("===[ testGenericQuery ]===");
		File genericsQueryToTest = new File(TEST_DIR + "/query-and-generic.xml");
		QueryType queryType = PrismTestUtil.getJaxbUtil()
				.unmarshalObject(genericsQueryToTest, QueryType.class);
		LOGGER.info("===[ query type parsed ]===");
		ObjectQuery query = null;
		try {
			query = QueryJaxbConvertor.createObjectQuery(GenericObjectType.class, queryType, PrismTestUtil.getPrismContext());
			LOGGER.info("query converted: ");

			LOGGER.info("QUERY DUMP: {}", query.debugDump());
			LOGGER.info("QUERY Pretty print: {}", query.toString());
			System.out.println("QUERY Pretty print: " + query.toString());

			// check parent filter
			assertNotNull(query);
			assertEquals(AndFilter.class, query.getFilter().getClass());
			assertEquals(2, ((AndFilter) query.getFilter()).getCondition().size());
			// check first condition
			ObjectFilter first = ((AndFilter) query.getFilter()).getCondition().get(0);
			assertEquals(EqualsFilter.class, first.getClass());
			List<? extends PrismValue> values = ((EqualsFilter) first).getValues();
			assertEquals(1, values.size());
			assertEquals(PrismPropertyValue.class, values.get(0).getClass());
			// assertEquals(PrismTestUtil.createPolyString("generic object"),
			// ((PrismPropertyValue) values.get(0)).getValue());
			assertEquals(GenericObjectType.F_NAME, ((EqualsFilter) first).getDefinition().getName());
			assertEquals(PolyStringType.COMPLEX_TYPE, ((EqualsFilter) first).getDefinition().getTypeName());
			// check second condition
			ObjectFilter second = ((AndFilter) query.getFilter()).getCondition().get(1);
			assertEquals(EqualsFilter.class, second.getClass());
			List<? extends PrismValue> secValues = ((EqualsFilter) second).getValues();
			assertEquals(1, secValues.size());
			assertEquals(PrismPropertyValue.class, secValues.get(0).getClass());
			assertEquals(123, ((PrismPropertyValue) secValues.get(0)).getValue());
			assertEquals(new QName("http://midpoint.evolveum.com/xml/ns/test/extension", "intType"),
					((EqualsFilter) second).getDefinition().getName());
			assertEquals(DOMUtil.XSD_INT, ((EqualsFilter) second).getDefinition().getTypeName());

			QueryType convertedQueryType = QueryJaxbConvertor.createQueryType(query, PrismTestUtil.getPrismContext());
			assertNotNull("Re-serialized query is null ", convertedQueryType);
			assertNotNull("Filter in re-serialized query must not be null.", convertedQueryType.getFilter());
			
			LOGGER.info(DOMUtil.serializeDOMToString(convertedQueryType.getFilter()));
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
	public void testUserQuery() throws Exception {
		LOGGER.info("===[ testUserQuery ]===");
		File[] userQueriesToTest = new File[] { new File(TEST_DIR + "/query-user-by-fullName.xml"),
				new File(TEST_DIR + "/query-user-by-name.xml"),
				new File(TEST_DIR + "/query-user-substring-fullName.xml") };
		// prismContext.silentMarshalObject(queryTypeNew, LOGGER);
		for (File file : userQueriesToTest) {
			QueryType queryType = PrismTestUtil.getJaxbUtil().unmarshalObject(file, QueryType.class);
			LOGGER.info("===[ query type parsed ]===");
			ObjectQuery query = null;
			try {
				query = QueryJaxbConvertor.createObjectQuery(UserType.class, queryType, PrismTestUtil.getPrismContext());
				LOGGER.info("query converted: ");

				LOGGER.info("QUERY DUMP: {}", query.debugDump());
				LOGGER.info("QUERY Pretty print: {}", query.toString());
				System.out.println("QUERY Pretty print: " + query.toString());


				QueryType convertedQueryType = QueryJaxbConvertor.createQueryType(query, PrismTestUtil.getPrismContext());
				LOGGER.info(DOMUtil.serializeDOMToString(convertedQueryType.getFilter()));
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
	public void testConvertQueryNullFilter() throws Exception{
		ObjectQuery query = ObjectQuery.createObjectQuery(ObjectPaging.createPaging(0, 10));
		QueryType queryType = QueryJaxbConvertor.createQueryType(query, PrismTestUtil.getPrismContext());
		
		assertNotNull(queryType);
		assertNull(queryType.getFilter());
		PagingType paging = queryType.getPaging();
		assertNotNull(paging);
		assertEquals(new Integer(0), paging.getOffset());
		assertEquals(new Integer(10), paging.getMaxSize());
		
	}
}
