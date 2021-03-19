/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query.lang;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.displayQuery;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.getFilterCondition;

import java.io.File;
import java.io.IOException;
import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.AbstractPrismTest;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismInternalTestUtil;
import com.evolveum.midpoint.prism.PrismNamespaceContext;
import com.evolveum.midpoint.prism.foo.ObjectType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.impl.xnode.ListXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.MapXNodeImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.FullTextFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.PrismQuerySerialization;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class TestQueryConverters extends AbstractPrismTest {

    private static final File TEST_DIR = new File("src/test/resources/query/lang");

    private static final File FILTER_USER_NAME_FILE = new File(TEST_DIR, "filter-user-name.xml");
    private static final File FILTER_USER_AND_FILE = new File(TEST_DIR, "filter-user-and.xml");
    private static final File FILTER_TYPE_USER_AND = new File(TEST_DIR, "filter-type-user-and-xml.xml");
    private static final File FILTER_NOT_IN_OID = new File(TEST_DIR, "filter-not-in-oid.xml");
    private static final File FILTER_NOT_FULL_TEXT = new File(TEST_DIR, "filter-not-full-text.xml");

    @BeforeSuite
    public void setupDebug() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
        PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
    }

    @Test
    public void testFilterUserNameJaxb() throws Exception {
        SearchFilterType filterType = PrismTestUtil.parseAnyValue(FILTER_USER_NAME_FILE);
        ObjectQuery query = toObjectQuery(UserType.class, filterType);
        displayQuery(query);

        assertNotNull(query);

        ObjectFilter filter = query.getFilter();
        PrismAsserts.assertEqualsFilter(filter, UserType.F_NAME, PolyStringType.COMPLEX_TYPE,
                ItemPath.create(new QName(null, UserType.F_NAME.getLocalPart())));
        PrismAsserts.assertEqualsFilterValue((EqualFilter<?>) filter, createPolyString("jack"));

        QueryType convertedQueryType = toQueryType(query);
        System.out.println("Re-converted query type");
        System.out.println(convertedQueryType.debugDump());

//        Element filterClauseElement = convertedQueryType.getFilter().getFilterClauseAsElement(getPrismContext());
//
//        System.out.println("Serialized filter (JAXB->DOM)");
//        System.out.println(DOMUtil.serializeDOMToString(filterClauseElement));
//
//        DomAsserts.assertElementQName(filterClauseElement, EqualFilter.ELEMENT_NAME);
//        DomAsserts.assertSubElements(filterClauseElement, 2);
//
//        DomAsserts.assertSubElement(filterClauseElement, PrismConstants.Q_VALUE);
//        Element valueElement = DOMUtil.getChildElement(filterClauseElement, PrismConstants.Q_VALUE);
//        DomAsserts.assertTextContent(valueElement, "jack");
    }

    @Test
    public void testFilterUserAndJaxb() throws Exception {
        SearchFilterType filterType = PrismTestUtil.parseAnyValue(FILTER_USER_AND_FILE);
        ObjectQuery query = toObjectQuery(UserType.class, filterType);
        displayQuery(query);

        assertNotNull(query);

        ObjectFilter filter = query.getFilter();
        PrismAsserts.assertAndFilter(filter, 2);

        ObjectFilter first = getFilterCondition(filter, 0);
        PrismAsserts.assertEqualsFilter(first, UserType.F_GIVEN_NAME, DOMUtil.XSD_STRING,
                ItemPath.create(new QName(null, UserType.F_GIVEN_NAME.getLocalPart())));
        PrismAsserts.assertEqualsFilterValue((EqualFilter) first, "Jack");

        ObjectFilter second = getFilterCondition(filter, 1);
        PrismAsserts.assertEqualsFilter(second, UserType.F_LOCALITY, DOMUtil.XSD_STRING,
                ItemPath.create(new QName(null, UserType.F_LOCALITY.getLocalPart())));
        PrismAsserts.assertEqualsFilterValue((EqualFilter) second, "Caribbean");

        QueryType convertedQueryType = toQueryType(query);
        System.out.println("Re-converted query type");
        System.out.println(convertedQueryType.debugDump());

        SearchFilterType convertedFilterType = convertedQueryType.getFilter();
        MapXNode xFilter = convertedFilterType.serializeToXNode(getPrismContext());
        PrismAsserts.assertSize(xFilter, 1);
        PrismAsserts.assertSubnode(xFilter, AndFilter.ELEMENT_NAME, MapXNodeImpl.class);
        MapXNodeImpl xandmap = (MapXNodeImpl) xFilter.get(AndFilter.ELEMENT_NAME);
        PrismAsserts.assertSize(xandmap, 1);
        PrismAsserts.assertSubnode(xandmap, EqualFilter.ELEMENT_NAME, ListXNodeImpl.class);
        ListXNodeImpl xequalsList = (ListXNodeImpl) xandmap.get(EqualFilter.ELEMENT_NAME);
        PrismAsserts.assertSize(xequalsList, 2);

//        Element filterClauseElement = convertedFilterType.getFilterClauseAsElement(getPrismContext());
//        System.out.println("Serialized filter (JAXB->DOM)");
//        System.out.println(DOMUtil.serializeDOMToString(filterClauseElement));
//
//        DomAsserts.assertElementQName(filterClauseElement, AndFilter.ELEMENT_NAME);
//        DomAsserts.assertSubElements(filterClauseElement, 2);
//
//        Element firstSubelement = DOMUtil.getChildElement(filterClauseElement, 0);
//        DomAsserts.assertElementQName(firstSubelement, EqualFilter.ELEMENT_NAME);
//        Element firstValueElement = DOMUtil.getChildElement(firstSubelement, PrismConstants.Q_VALUE);
//        DomAsserts.assertTextContent(firstValueElement, "Jack");
//
//        Element secondSubelement = DOMUtil.getChildElement(filterClauseElement, 1);
//        DomAsserts.assertElementQName(secondSubelement, EqualFilter.ELEMENT_NAME);
//        Element secondValueElement = DOMUtil.getChildElement(secondSubelement, PrismConstants.Q_VALUE);
//        DomAsserts.assertTextContent(secondValueElement, "Caribbean");
    }

    @Test
    public void testFilterTypeUserAnd() throws Exception {
        SearchFilterType filterType = PrismTestUtil.parseAnyValue(FILTER_TYPE_USER_AND);

        ObjectQuery query = toObjectQuery(ObjectType.class, filterType);
        displayQuery(query);

        assertNotNull(query);

        ObjectFilter filter = query.getFilter();
        assertTrue("Filter is not of TYPE type", filter instanceof TypeFilter);

        ObjectFilter subFilter = ((TypeFilter) filter).getFilter();
        assertTrue("Filter is not of NONE type", subFilter instanceof AndFilter);



        String[] variants = new String[] {
            ". type UserType and givenName = 'Jack' and locality = 'Caribbean'",
            "(. type UserType and givenName = 'Jack') and locality = 'Caribbean'",
            ". type UserType and (givenName = 'Jack' and locality = 'Caribbean')",
            ". type UserType and (givenName = 'Jack') and (locality = 'Caribbean')",
            "givenName = 'Jack' and locality = 'Caribbean' and . type UserType",
            "givenName = 'Jack' and (. type UserType and locality = 'Caribbean')",
        };

        for(String variant : variants) {
            ObjectFilter lang = parseFilter(ObjectType.class, variant);
            display("Query DSL variant: " +  variant);
            assertEquals(lang.toString(), filter.toString());
        }
        PrismQuerySerialization serialized = getPrismContext().querySerializer().serialize(filter, PrismNamespaceContext.of(UserType.F_NAME.getNamespaceURI()));
        assertEquals(serialized.filterText(), variants[0]);
    }

    private <C extends Containerable> ObjectFilter parseFilter(Class<C> typeClass, String string) throws SchemaException {
        return getPrismContext().createQueryParser().parseQuery(typeClass, string);
    }

    @Test
    public void testFilterNotInOid() throws Exception {
        SearchFilterType filterType = PrismTestUtil.parseAnyValue(FILTER_NOT_IN_OID);

        ObjectQuery query = toObjectQuery(UserType.class, filterType);
        displayQuery(query);

        assertNotNull(query);

        ObjectFilter filter = query.getFilter();
        assertTrue("Filter is not of NOT type", filter instanceof NotFilter);

        ObjectFilter subFilter = ((NotFilter) filter).getFilter();
        assertTrue("Subfilter is not of IN_OID type", subFilter instanceof InOidFilter);

        QueryType convertedQueryType = toQueryType(query);
        System.out.println("Re-converted query type");
        System.out.println(convertedQueryType.debugDump());

//        Element filterClauseElement = convertedQueryType.getFilter().getFilterClauseAsElement(getPrismContext());
//        logger.info(convertedQueryType.getFilter().getFilterClauseXNode().debugDump());
//
//        System.out.println("Serialized filter (JAXB->DOM)");
//        String filterAsString = DOMUtil.serializeDOMToString(filterClauseElement);
//        System.out.println(filterAsString);
//        logger.info(filterAsString);
//
//        DomAsserts.assertElementQName(filterClauseElement, new QName(PrismConstants.NS_QUERY, "not"));
//        assertEquals("wrong # of inOid subfilters", 1, filterClauseElement.getElementsByTagNameNS(PrismConstants.NS_QUERY, "inOid").getLength());
//        assertEquals("wrong # of value subfilters", 4, filterClauseElement.getElementsByTagNameNS(PrismConstants.NS_QUERY, "value").getLength());
    }

    @Test
    public void testFilterNotFullText() throws Exception {
        SearchFilterType filterType = PrismTestUtil.parseAnyValue(FILTER_NOT_FULL_TEXT);

        ObjectQuery query = toObjectQuery(UserType.class, filterType);
        displayQuery(query);

        assertNotNull(query);

        ObjectFilter filter = query.getFilter();
        assertTrue("Filter is not of NOT type", filter instanceof NotFilter);

        ObjectFilter subFilter = ((NotFilter) filter).getFilter();
        assertTrue("Subfilter is not of FULL_TEXT type", subFilter instanceof FullTextFilter);

        QueryType convertedQueryType = toQueryType(query);
        System.out.println("Re-converted query type");
        System.out.println(convertedQueryType.debugDump());

//        Element filterClauseElement = convertedQueryType.getFilter().getFilterClauseAsElement(getPrismContext());
//        logger.info(convertedQueryType.getFilter().getFilterClauseXNode().debugDump());
//
//        System.out.println("Serialized filter (JAXB->DOM)");
//        String filterAsString = DOMUtil.serializeDOMToString(filterClauseElement);
//        System.out.println(filterAsString);
//        logger.info(filterAsString);
//
//        DomAsserts.assertElementQName(filterClauseElement, new QName(PrismConstants.NS_QUERY, "not"));
//        assertEquals("wrong # of fullText subfilters", 1, filterClauseElement.getElementsByTagNameNS(PrismConstants.NS_QUERY, "fullText").getLength());
//        assertEquals("wrong # of value subfilters", 2, filterClauseElement.getElementsByTagNameNS(PrismConstants.NS_QUERY, "value").getLength());
    }

    private ObjectQuery toObjectQuery(Class type, SearchFilterType filterType) throws Exception {
        return getPrismContext().getQueryConverter().createObjectQuery(type, filterType);
    }

    private QueryType toQueryType(ObjectQuery query) throws Exception {
        return getPrismContext().getQueryConverter().createQueryType(query);
    }
}
