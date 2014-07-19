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

package com.evolveum.midpoint.prism.query;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.*;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.parser.DomParser;
import com.evolveum.midpoint.prism.util.PrismUtil;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismInternalTestUtil;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.foo.UserType;
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
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DomAsserts;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class TestQueryConvertors {

	private static final Trace LOGGER = TraceManager.getTrace(TestQueryConvertors.class);

	private static final File TEST_DIR = new File("src/test/resources/query");

	private static final File FILTER_USER_NAME_FILE = new File(TEST_DIR, "filter-user-name.xml");
	private static final File FILTER_USER_AND_FILE = new File(TEST_DIR, "filter-user-and.xml");

	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
	}
	
	@Test
	public void testFilterUserNameJaxb() throws Exception {
		displayTestTitle("testFilterUserNameJaxb");

        SearchFilterType filterType = PrismTestUtil.parseAnyValue(FILTER_USER_NAME_FILE);
		ObjectQuery query = toObjectQuery(UserType.class, filterType);
		displayQuery(query);

		assertNotNull(query);

		ObjectFilter filter = query.getFilter();
		PrismAsserts.assertEqualsFilter(filter, UserType.F_NAME, PolyStringType.COMPLEX_TYPE,
				new ItemPath(new QName(null,UserType.F_NAME.getLocalPart())));
		PrismAsserts.assertEqualsFilterValue((EqualFilter) filter, createPolyString("jack"));

		QueryType convertedQueryType = toQueryType(query);
		System.out.println("Re-converted query type");
		System.out.println(convertedQueryType.debugDump());

		Element filterClauseElement = convertedQueryType.getFilter().getFilterClauseAsElement();

		System.out.println("Serialized filter (JAXB->DOM)");
		System.out.println(DOMUtil.serializeDOMToString(filterClauseElement));
		
		DomAsserts.assertElementQName(filterClauseElement, EqualFilter.ELEMENT_NAME);
		DomAsserts.assertSubElements(filterClauseElement, 2);
		
		DomAsserts.assertSubElement(filterClauseElement, PrismConstants.Q_VALUE);
		Element valueElement = DOMUtil.getChildElement(filterClauseElement, PrismConstants.Q_VALUE);
		DomAsserts.assertTextContent(valueElement, "jack");
		
	}
	
	@Test
	public void testFilterUserAndJaxb() throws Exception {
		displayTestTitle("testFilterUserAndJaxb");

		SearchFilterType filterType = PrismTestUtil.parseAnyValue(FILTER_USER_AND_FILE);
		ObjectQuery query = toObjectQuery(UserType.class, filterType);
		displayQuery(query);

		assertNotNull(query);

		ObjectFilter filter = query.getFilter();
		PrismAsserts.assertAndFilter(filter, 2);
		
		ObjectFilter first = getFilterCondition(filter, 0);
		PrismAsserts.assertEqualsFilter(first, UserType.F_GIVEN_NAME, DOMUtil.XSD_STRING,
				new ItemPath(new QName(null,UserType.F_GIVEN_NAME.getLocalPart())));
		PrismAsserts.assertEqualsFilterValue((EqualFilter) first, "Jack");
		
		ObjectFilter second = getFilterCondition(filter, 1);
		PrismAsserts.assertEqualsFilter(second, UserType.F_LOCALITY, DOMUtil.XSD_STRING,
				new ItemPath(new QName(null,UserType.F_LOCALITY.getLocalPart())));
		PrismAsserts.assertEqualsFilterValue((EqualFilter) second, "Caribbean");

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
		DomAsserts.assertTextContent(firstValueElement, "Jack");
		
		Element secondSubelement = DOMUtil.getChildElement(filterClauseElement, 1);
		DomAsserts.assertElementQName(secondSubelement, EqualFilter.ELEMENT_NAME);
		Element secondValueElement = DOMUtil.getChildElement(secondSubelement, PrismConstants.Q_VALUE);
		DomAsserts.assertTextContent(secondValueElement, "Caribbean");
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

}
