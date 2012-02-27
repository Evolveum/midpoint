/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 Igor Farinic
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.repo.test;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author Igor Farinic
 */
@ContextConfiguration(locations = { "../../../../../application-context-repository.xml",
		"classpath:application-context-configuration-test.xml" })
@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
public class RepositorySearchPagingTest extends AbstractTestNGSpringContextTests {

	public static final String BASE_PATH = "src/test/resources/";

	private boolean initialized = false;

	@Autowired
	private RepositoryService repositoryService;

	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	public RepositorySearchPagingTest() {
	}
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(new MidPointPrismContextFactory());
	}

	@BeforeClass
	public static void setUpClass() throws Exception {

	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@BeforeMethod
	public void setUp() throws Exception {
		if (!initialized) {
			OperationResult result = new OperationResult(RepositorySearchPagingTest.class.getName()
					+ ".setupPagingTests");

			EventHandler handler = new MyEventHandler(repositoryService);
			processFile("users.xml", handler, result);

			initialized = true;
		}
	}

	@AfterMethod
	public void tearDown() throws Exception {
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void pagingFromFirstElementTest() throws Exception {
		// GIVEN
		// prepare paging
		PagingType paging = new PagingType();
		Element el = DOMUtil.getDocument().createElementNS(SchemaConstants.NS_C, "property");
		el.setTextContent("name");
		PropertyReferenceType orderBy = new PropertyReferenceType();
		orderBy.setProperty(el);
		paging.setOrderBy(orderBy);
		paging.setOrderDirection(OrderDirectionType.DESCENDING);

		paging.setOffset(0);
		paging.setMaxSize(2);
		// create query
		QueryType query = new QueryType();
		Document doc = DOMUtil.getDocument();
		Element filter = doc.createElementNS(SchemaConstants.C_FILTER_AND.getNamespaceURI(),
				SchemaConstants.C_FILTER_AND.getLocalPart());
		query.setFilter(filter);

		// WHEN
		OperationResult parentResult = new OperationResult(RepositorySearchPagingTest.class.getName()
				+ ".pagingTest");
		List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, paging, parentResult);

		// THEN
		assertEquals(2, users.size());
		assertEquals("testuser0005", users.get(0).asObjectable().getName());
		assertEquals("testuser0004", users.get(1).asObjectable().getName());

		
	}

	@Test
	public void pagingMaxValueTest() throws Exception {
		// GIVEN
		// prepare paging
		PagingType paging = new PagingType();
		paging.setOffset(3);
		paging.setMaxSize(Integer.MAX_VALUE);
		// create query
		QueryType query = new QueryType();
		Document doc = DOMUtil.getDocument();
		Element filter = doc.createElementNS(SchemaConstants.C_FILTER_AND.getNamespaceURI(),
				SchemaConstants.C_FILTER_AND.getLocalPart());
		query.setFilter(filter);

		// WHEN
		OperationResult parentResult = new OperationResult(RepositorySearchPagingTest.class.getName()
				+ ".pagingTest");
		List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, paging, parentResult);

		// THEN
		assertEquals(2, users.size());
	}

	/**
	 * There is no API contract for Paging negative values, yet.
	 * Therefor we are ok, if repository returns 0 objects for negative paging values 
	 * 
	 * @throws Exception
	 */
	@Test
	public void pagingNegativeValuesTest() throws Exception {
		// GIVEN
		// prepare paging
		PagingType paging = new PagingType();
		paging.setOffset(-2);
		paging.setMaxSize(-4);
		// create query
		QueryType query = new QueryType();
		Document doc = DOMUtil.getDocument();
		Element filter = doc.createElementNS(SchemaConstants.C_FILTER_AND.getNamespaceURI(),
				SchemaConstants.C_FILTER_AND.getLocalPart());
		query.setFilter(filter);

		// WHEN
		OperationResult parentResult = new OperationResult(RepositorySearchPagingTest.class.getName()
				+ ".pagingTest");
		List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, paging, parentResult);

		// THEN
		assertEquals(0, users.size());
	}

	@Test
	public void pagingOnlySortingTest() throws Exception {
		// GIVEN
		// prepare paging
		PagingType paging = new PagingType();
		Element el = DOMUtil.getDocument().createElementNS(SchemaConstants.NS_C, "property");
		el.setTextContent("name");
		PropertyReferenceType orderBy = new PropertyReferenceType();
		orderBy.setProperty(el);
		paging.setOrderBy(orderBy);
		paging.setOrderDirection(OrderDirectionType.DESCENDING);
		// create query
		QueryType query = new QueryType();
		Document doc = DOMUtil.getDocument();
		Element filter = doc.createElementNS(SchemaConstants.C_FILTER_AND.getNamespaceURI(),
				SchemaConstants.C_FILTER_AND.getLocalPart());
		query.setFilter(filter);

		// WHEN
		OperationResult parentResult = new OperationResult(RepositorySearchPagingTest.class.getName()
				+ ".pagingTest");
		List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, paging, parentResult);

		// THEN
		assertEquals(5, users.size());
		assertEquals("testuser0005", users.get(0).getName());
		assertEquals("testuser0001", users.get(4).getName());
	}

	private static void processFile(String filename, EventHandler handler, OperationResult result)
			throws FileNotFoundException {

		String filepath = BASE_PATH + filename;

		System.out.println("Validating " + filename);

		FileInputStream fis = null;

		File file = new File(filepath);
		fis = new FileInputStream(file);

		Validator validator = new Validator(PrismTestUtil.getPrismContext());
		if (handler != null) {
			validator.setHandler(handler);
		}
		validator.setVerbose(false);

		validator.validate(fis, result, RepositorySearchPagingTest.class.getName() + ".validateObject");

		if (!result.isSuccess()) {
			System.out.println("Errors:");
			System.out.println(result.dump());
		} else {
			System.out.println("No errors");
			System.out.println(result.dump());
		}

	}

	private static final class MyEventHandler implements EventHandler {

		RepositoryService repositoryService;

		public MyEventHandler(RepositoryService repositoryService) {
			this.repositoryService = repositoryService;
		}

		@Override
		public EventResult preMarshall(Element objectElement, Node postValidationTree,
				OperationResult objectResult) {
			return EventResult.cont();
		}

		@Override
		public <T extends ObjectType> EventResult postMarshall(PrismObject<T> object, Element objectElement, OperationResult objectResult) {
			System.out.println("Handler processing " + object + ", result:");
			try {
				repositoryService.addObject(object, objectResult);
			} catch (ObjectAlreadyExistsException e) {
				throw new RuntimeException(e);
			} catch (SchemaException e) {
				throw new RuntimeException(e);
			}
			System.out.println(objectResult.dump());
			return EventResult.cont();
		}

		@Override
		public void handleGlobalError(OperationResult currentResult) {
			System.out.println("Handler got global error:");
			System.out.println(currentResult.dump());
		}

	};

}
